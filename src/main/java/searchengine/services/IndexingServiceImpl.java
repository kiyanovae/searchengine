package searchengine.services;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.SuccessfulResponse;
import searchengine.exceptions.BadRequestException;
import searchengine.exceptions.ConflictRequestException;
import searchengine.exceptions.InternalServerException;
import searchengine.exceptions.StoppedByUserException;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@Service
@ConfigurationProperties(prefix = "connection-settings")
public class IndexingServiceImpl implements IndexingService {
    private static final int SLEEP_DURATION = 1000;
    private static final int TIMEOUT_MILLISECONDS = 60000;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList sites;
    private final PageIndexerService pageIndexerService;
    private final StatusService statusService;
    @Lazy
    private final IndexingService indexingService;
    private ForkJoinPool pool = new ForkJoinPool();
    private Thread indexingThread;
    @Setter
    private String userAgent;
    @Setter
    private String referrer;

    @Autowired
    public IndexingServiceImpl(SiteRepository siteRepository, PageRepository pageRepository, SitesList sites,
                               PageIndexerService pageIndexerService, StatusService statusService,
                               @Lazy IndexingService indexingService) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.sites = sites;
        this.pageIndexerService = pageIndexerService;
        this.statusService = statusService;
        this.indexingService = indexingService;
    }

    @Override
    public SuccessfulResponse startIndexing() {
        synchronized (statusService) {
            if (statusService.isIndexingRunning()) {
                throw new ConflictRequestException("Indexing has already started");
            }
            try {
                statusService.startIndexing();
            } catch (InterruptedException e) {
                restoreIndexingState();
            }
            indexingThread = new Thread(this::indexing);
            log.info("Indexing has been started");
            indexingThread.start();
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
        try {
            indexingThread.join();
        } catch (InterruptedException e) {
            throw new InternalServerException("Server error");
        }
        log.info("Indexing has been stopped");
        SuccessfulResponse response = new SuccessfulResponse();
        synchronized (statusService) {
            statusService.stopIndexing();
            return response;
        }
    }

    @Override
    public SuccessfulResponse individualPage(String url) {
        statusService.incrementAdditionalTaskCount();
        try {
            Site configurationSite = findConfigurationSite(url)
                    .orElseThrow(() ->
                            new BadRequestException("The '" + url + "' page is outside the sites specified in the " +
                                    "configuration file"));
            Connection.Response response = Jsoup.connect(url).userAgent(userAgent).referrer(referrer)
                    .ignoreHttpErrors(true).timeout(TIMEOUT_MILLISECONDS).execute();
            SiteEntity site = getSite(configurationSite);
            if (!site.getStatus().equals(SiteEntity.SiteStatus.INDEXED)) {
                throw new ConflictRequestException("The site must have an indexed status");
            }
            String content = response.body();
            String urlPath = response.url().getPath();
            String path;
            if (urlPath.isEmpty()) {
                path = "/";
            } else {
                path = urlPath;
            }
            int code = response.statusCode();
            pool.execute(() -> pageIndexerService.handle(url, site, path, code, content));
            log.info("Page '{}' added to the queue", url);
            return new SuccessfulResponse();
        } catch (IllegalArgumentException e) {
            statusService.decrementAdditionalTaskCount();
            throw new BadRequestException(e.getMessage());
        } catch (IOException e) {
            statusService.decrementAdditionalTaskCount();
            throw new BadRequestException(url + " - " + e.getMessage());
        }
    }

    @Transactional
    public SiteEntity cleanUpAndSaveSite(String url, String name) {
        indexingService.cleanSite(url);
        if (statusService.isIndexingStoppedByUser()) {
            throw new StoppedByUserException("");
        }
        return siteRepository.save(new SiteEntity(SiteEntity.SiteStatus.INDEXING, url, name));
    }

    @Transactional
    public void cleanSite(String url) {
        siteRepository.findByUrl(url).ifPresent(site -> {
            List<Integer> pageIdsBySite = pageRepository.findIdsBySite(site);
            for (int pageId : pageIdsBySite) {
                pageRepository.customDeleteById(pageId);
                if (statusService.isIndexingStoppedByUser()) {
                    throw new StoppedByUserException("");
                }
            }
            siteRepository.delete(site);
        });
    }

    private void indexing() {
        List<PageHandlerService> tasks = new ArrayList<>();
        for (Site confSite : sites.getSites()) {
            if (statusService.isIndexingStoppedByUser()) {
                return;
            }
            String siteUrl = confSite.getUrl();
            String siteName = confSite.getName();
            SiteEntity site;
            try {
                site = indexingService.cleanUpAndSaveSite(siteUrl, siteName);
            } catch (StoppedByUserException e) {
                return;
            }
            PageHandlerService task = getPageHandlerService();
            task.setSite(site);
            String baseUri = siteUrl.concat("/");
            task.setPageUrl(baseUri);
            task.setBaseUri(baseUri);
            tasks.add(task);
            statusService.incrementTaskCount();
        }
        tasks.forEach(task -> {
            pool.execute(task);
            log.info("Indexing has been started for '{}' site", task.getSite().getUrl());
        });
        awaitIndexingTermination(tasks);
    }

    @Lookup
    public PageHandlerService getPageHandlerService() {
        return null;
    }

    private void awaitIndexingTermination(List<PageHandlerService> tasks) {
        while (!tasks.isEmpty()) {
            sleep();
            Iterator<PageHandlerService> taskIterator = tasks.iterator();
            while (taskIterator.hasNext()) {
                PageHandlerService task = taskIterator.next();
                if (task.isDone()) {
                    SiteEntity site = task.getSite();
                    if (task.isCompletedNormally()) {
                        site.setStatus(SiteEntity.SiteStatus.INDEXED);
                        log.info("The '{}' site has been indexed", site.getUrl());
                    } else {
                        String errorMessage = task.getException().getMessage();
                        site.setStatus(SiteEntity.SiteStatus.FAILED);
                        site.setLastError(errorMessage);
                        log.info("Indexing of '{}' site finished with an error: {}", site.getUrl(), errorMessage);
                    }
                    site.setStatusTime(LocalDateTime.now());
                    siteRepository.save(site);
                    taskIterator.remove();
                }
            }
        }
        if (!statusService.isIndexingStoppedByUser()) {
            log.info("Indexing has been finished");
            statusService.setIndexingRunning(false);
            return;
        }
        while (statusService.getTaskCount() != 0) {
            log.info("{} pages in the progress", statusService.getTaskCount());
            sleep();
        }
        while (statusService.getAdditionalTaskCount() != 0) {
            log.info("{} additional pages in the progress", statusService.getTaskCount());
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
            return siteRepository.findByUrl(url).orElseGet(() ->
                    siteRepository.save(new SiteEntity(SiteEntity.SiteStatus.INDEXED, url, name)));
        }
    }

    private void sleep() {
        try {
            Thread.sleep(SLEEP_DURATION);
        } catch (InterruptedException e) {
            restoreIndexingState();
        }
    }

    private void restoreIndexingState() {
        pool.shutdownNow();
        while (!pool.isShutdown()) {
        }
        pool = new ForkJoinPool();
        statusService.seDefault();
        throw new InternalServerException("Server error. Try to repeat the operation.");
    }
}