package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.SuccessfulResponse;
import searchengine.exceptions.BadRequestException;
import searchengine.exceptions.ConflictRequestException;
import searchengine.exceptions.InternalServerException;
import searchengine.model.SiteEntity;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@RequiredArgsConstructor
@Service
@ConfigurationProperties(prefix = "connection-settings")
public class IndexingServiceImpl implements IndexingService {
    private static final int SLEEP_DURATION = 1000;
    private static final int TIMEOUT_MILLISECONDS = 60000;
    private final SiteRepository siteRepository;
    private final SitesList sites;
    private final PageIndexerService pageIndexerService;
    private final StatusService statusService;
    private final SaverService saverService;
    private final ForkJoinPool pool = ForkJoinPool.commonPool();
    ArrayList<PageHandlerService> taskList = new ArrayList<>();
    private Thread observerThread;
    private Thread indexThread;
    @Setter
    private String userAgent;
    @Setter
    private String referrer;

    @Override
    public SuccessfulResponse startIndexing() {
        synchronized (statusService) {
            if (statusService.isIndexingRunning()) {
                throw new ConflictRequestException("Indexing has already started");
            }
            statusService.setAdditionalTasksStoppedByIndexing(true);
            while (statusService.getAdditionalTaskCount() != 0) {
                sleep();
            }
            statusService.setAdditionalTasksStoppedByIndexing(false);
            statusService.setIndexingRunning(true);
            statusService.setIndexingStoppedByUser(false);
            indexThread = new Thread(this::index);
            log.info("Indexing has been started");
            indexThread.start();
            return new SuccessfulResponse();
        }
    }

    @Override
    public SuccessfulResponse stopIndexing() {
        synchronized (statusService) {
            if (!statusService.isIndexingRunning()) {
                throw new ConflictRequestException("Indexing is not running");
            }
            if (statusService.isIndexingStoppedByUser()) {
                throw new ConflictRequestException("Indexing already in the process of stopping");
            }
            statusService.setIndexingStoppedByUser(true);
        }
        log.info("Started the process of stopping indexing");
        indexThread.interrupt();
        if (observerThread != null) {
            try {
                observerThread.join();
            } catch (InterruptedException e) {
                throw new InternalServerException(e.getMessage(), e);
            }
        }
        log.info("Indexing has been stopped");
        SuccessfulResponse response = new SuccessfulResponse();
        synchronized (statusService) {
            statusService.setIndexingRunning(false);
            statusService.setIndexingStoppedByUser(false);
            return response;
        }
    }

    @Override
    public SuccessfulResponse individualPage(String url) {
        statusService.incrementAdditionalTaskCount();
        try {
            Site configurationSite = findConfigurationSite(url).orElseThrow(() -> new BadRequestException("The '" + url + "' page is outside the sites specified in the configuration file"));
            Connection.Response response = Jsoup.connect(url).userAgent(userAgent).referrer(referrer).ignoreHttpErrors(true).timeout(TIMEOUT_MILLISECONDS).execute();
            SiteEntity site = getSite(configurationSite);
            if (!site.getStatus().equals(SiteEntity.SiteStatus.INDEXED)) {
                throw new ConflictRequestException("The site must have an indexed status");
            }
            pool.execute(() -> pageIndexerService.handle(site, response));
            log.info("{} has been added to the queue", url);
            return new SuccessfulResponse();
        } catch (IllegalArgumentException e) {
            statusService.decrementAdditionalTaskCount();
            throw new BadRequestException(e.getMessage());
        } catch (IOException e) {
            statusService.decrementAdditionalTaskCount();
            throw new BadRequestException(url + " - " + e.getMessage());
        }
    }

    private void index() {
        taskList.clear();
        taskList.trimToSize();
        createAndExecuteTasks();
        observerThread = new Thread(this::awaitIndexingTermination);
        observerThread.start();
    }

    private void createAndExecuteTasks() {
        for (Site confSite : sites.getSites()) {
            String siteUrl = confSite.getUrl();
            String siteName = confSite.getName();
            SiteEntity site = saverService.cleanUpAndSaveSite(siteUrl, siteName);
            PageHandlerService task = getPageHandlerService();
            task.setSite(site);
            String baseUri = siteUrl.concat("/");
            task.setUrl(baseUri);
            task.setBaseUri(baseUri);
            taskList.add(task);
            statusService.incrementTaskCount();
            pool.execute(task);
            log.info("The {} site has been started indexing", siteUrl);
        }
    }

    @Lookup
    public PageHandlerService getPageHandlerService() {
        return null;
    }

    private void awaitIndexingTermination() {
        while (!taskList.isEmpty()) {
            sleep();
            Iterator<PageHandlerService> taskIterator = taskList.iterator();
            while (taskIterator.hasNext()) {
                PageHandlerService task = taskIterator.next();
                if (task.isDone()) {
                    SiteEntity site = task.getSite();
                    if (task.isCompletedNormally()) {
                        site.setStatus(SiteEntity.SiteStatus.INDEXED);
                    } else {
                        String errorMessage = task.getException().getMessage();
                        site.setStatus(SiteEntity.SiteStatus.FAILED);
                        site.setLastError(errorMessage);
                        log.info("Indexing of {} finished with an error: {}", site.getUrl(), errorMessage);
                    }
                    site.setStatusTime(LocalDateTime.now());
                    siteRepository.save(site);
                    log.info("The {} site has been indexed", site.getUrl());
                    taskIterator.remove();
                }
            }
        }
        if (!statusService.isIndexingStoppedByUser()) {
            log.info("Indexing has been finished");
            synchronized (statusService) {
                statusService.setIndexingRunning(false);
                return;
            }
        }
        while (statusService.getTaskCount() != 0) {
            log.info("{} tasks in the progress", statusService.getTaskCount());
            sleep();
        }
        while (statusService.getAdditionalTaskCount() != 0) {
            log.info("{} additional tasks in the progress", statusService.getTaskCount());
            sleep();
        }
    }

    private Optional<Site> findConfigurationSite(String url) {
        for (Site site : sites.getSites()) {
            if (url.startsWith(site.getUrl())) {
                return Optional.of(site);
            }
        }
        return Optional.empty();
    }

    private SiteEntity getSite(Site site) {
        String url = site.getUrl();
        String name = site.getName();
        Optional<SiteEntity> optionalSite = siteRepository.findByUrl(url);
        if (optionalSite.isPresent()) {
            return optionalSite.get();
        }
        synchronized (this) {
            return siteRepository.findByUrl(url).orElseGet(() -> saverService.saveSiteWithIndexedStatus(url, name));
        }
    }

    private void sleep() {
        try {
            Thread.sleep(SLEEP_DURATION);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}