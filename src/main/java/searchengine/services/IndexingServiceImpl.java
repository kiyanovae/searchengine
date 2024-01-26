package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.ConnectionSettings;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.SuccessfulResponse;
import searchengine.exceptions.RequestException;
import searchengine.model.*;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
public class IndexingServiceImpl implements IndexingService {
    private static volatile boolean subIndexingIsRunning;
    private static volatile boolean mainIndexingIsRunning;
    private static volatile boolean isStoppedByUser;
    private final SiteRepository siteRepository;
    private final SitesList sites;
    private final ConnectionSettings connectionSettings;
    private final PageIndexerService pageIndexerService;
    private final ForkJoinPool pool;
    private Thread indexingThread;

    public IndexingServiceImpl(SiteRepository siteRepository, SitesList sites, ConnectionSettings connectionSettings, PageIndexerService pageIndexerService) {
        this.siteRepository = siteRepository;
        this.sites = sites;
        this.connectionSettings = connectionSettings;
        this.pageIndexerService = pageIndexerService;
        pool = new ForkJoinPool();
        mainIndexingIsRunning = false;
        indexingThread = null;
    }

    public static boolean getMainIndexingIsRunning() {
        return mainIndexingIsRunning;
    }

    public static boolean getIsStoppedByUser() {
        return isStoppedByUser;
    }

    public static void setSubIndexingIsRunning(boolean subIndexingIsRunning) {
        IndexingServiceImpl.subIndexingIsRunning = subIndexingIsRunning;
    }

    @Transactional
    @Override
    public SuccessfulResponse startIndexing() {
        check(1);
        List<UrlFinder> taskList = createTasks();
        log.info("Indexing has begun");
        taskList.forEach(pool::execute);
        indexingThread = new Thread(() -> indexingAllSites(taskList));
        indexingThread.start();
        return new SuccessfulResponse(true);
    }

    @Override
    public SuccessfulResponse stopIndexing() {
        check(2);
        isStoppedByUser = true;
        while (indexingThread.isAlive()) {
        }
        log.info("Индексация остановлена");
        return new SuccessfulResponse(true);
    }

    @Override
    public SuccessfulResponse indexPage(String url) {
        Site site = getSite(url);
        if (site == null) {
            throw new RequestException("Данная страница находится за пределами сайтов указанных в конфигурационном файле");
        }
        check(3);
        try {
            Connection.Response response = Jsoup.connect(url).userAgent(connectionSettings.getUserAgent()).referrer(connectionSettings.getReferrer()).ignoreHttpErrors(true).execute();
            Document document = response.parse();
            SiteEntity siteEntity = getSiteEntity(site);
            new Thread(() -> {
                try {
                    pageIndexerService.indexPage(siteEntity, url.substring(site.getUrl().length()), response.statusCode(), document.outerHtml());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new SuccessfulResponse(true);
    }

    private synchronized void check(int code) {
        switch (code) {
            case 1 -> {
                if (subIndexingIsRunning) {
                    throw new RequestException("Запущена индексация отдельной страницы.");
                }
                if (mainIndexingIsRunning) {
                    throw new RequestException("Индексация уже запущена.");
                }
                mainIndexingIsRunning = true;
                isStoppedByUser = false;
            }
            case 2 -> {
                if (!mainIndexingIsRunning) {
                    throw new RequestException("Индексация остановлена/останавливается.");
                }
                mainIndexingIsRunning = false;
            }
            case 3-> {
                if (mainIndexingIsRunning) {
                    throw new RequestException("Запущена основная индексация.");
                }
                subIndexingIsRunning = true;
                isStoppedByUser = false;
                PageIndexerServiceImpl.getQueuedTaskCount().incrementAndGet();
            }
        }
    }

    private List<UrlFinder> createTasks() {
        List<UrlFinder> taskList = new ArrayList<>();
        UrlFinder.setUserAgent(connectionSettings.getUserAgent());
        UrlFinder.setReferrer(connectionSettings.getReferrer());
        for (Site site : sites.getSites()) {
            String siteUrl = site.getUrl().concat("/");
            deleteSiteFromDataBase(siteUrl);
            int siteId = createSiteAndGetId(siteUrl, site.getName());
            log.info("Task ".concat(siteUrl).concat(" created"));
            UrlFinder task = new UrlFinder(pageIndexerService, siteId, siteUrl, siteUrl);
            taskList.add(task);
        }
        return taskList;
    }

    private void deleteSiteFromDataBase(String siteUrl) {
        Optional<SiteEntity> optSiteEntity = siteRepository.findByUrl(siteUrl);
        optSiteEntity.ifPresent(siteEntity -> siteRepository.deleteById(siteEntity.getId()));
    }

    private int createSiteAndGetId(String siteUrl, String siteName) {
        SiteEntity siteEntity = siteRepository.save(new SiteEntity(SiteStatus.INDEXING, siteUrl, siteName));
        return siteEntity.getId();
    }

    private void indexingAllSites(List<UrlFinder> taskList) {
        while (!taskList.isEmpty()) {
            Iterator<UrlFinder> taskIterator = taskList.iterator();
            while (taskIterator.hasNext()) {
                UrlFinder task = taskIterator.next();
                if (task.isDone()) {
                    updateSiteInfo(task.getException(), task);
                    if (task.getException() != null) {
                        log.warn(task.getException().getMessage());
                        /*throw new RuntimeException(task.getException());*/
                    }
                    log.warn(task.getMainUrl() + " Done!");
                    taskIterator.remove();
                }
            }
        }
        /*pageIndexerService.flushLemmaIndex();*/
        //pageIndexerService.flushIndexEntities();
        mainIndexingIsRunning = false;
    }

    private void updateSiteInfo(Throwable taskException, UrlFinder task) {
        Optional<SiteEntity> optSiteEntity = siteRepository.findByUrl(task.getMainUrl());
        if (taskException == null) {
            optSiteEntity.ifPresent(site -> {
                site.setStatus(SiteStatus.INDEXED);
                siteRepository.save(site);
            });
        } else {
            optSiteEntity.ifPresent(site -> {
                site.setStatus(SiteStatus.FAILED);
                site.setLastError(taskException.getMessage());
                siteRepository.save(site);
            });
        }
    }

    private Site getSite(String url) {
        List<Site> siteList = sites.getSites();
        for (Site site : siteList) {
            if (url.startsWith(site.getUrl())) {
                return site;
            }
        }
        return null;
    }

    private SiteEntity getSiteEntity(Site site) {
        String siteUrl = site.getUrl().concat("/");
        String siteName = site.getName();
        synchronized (siteRepository) {
            Optional<SiteEntity> optionalSiteEntity = siteRepository.findByUrl(siteUrl);
            return optionalSiteEntity.orElseGet(() -> siteRepository.save(new SiteEntity(SiteStatus.INDEXING, siteUrl, siteName)));
        }
    }
}
