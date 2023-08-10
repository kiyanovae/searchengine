package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.config.Connection;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.model.SitePage;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.ApiService;
import searchengine.services.LemmaService;
import searchengine.services.PageIndexerService;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class ApiServiceImpl implements ApiService {

    private final PageIndexerService pageIndexerService;
    private final LemmaService lemmaService;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList sitesToIndexing;
    private final Set<SitePage> sitePagesAllFromDB;
    private final Connection connection;
    private static final Logger logger = LoggerFactory.getLogger(ApiServiceImpl.class);
    private AtomicBoolean indexingProcessing;

    @Override
    public void startIndexing(AtomicBoolean indexingProcessing) {
        this.indexingProcessing = indexingProcessing;
        try {
            deleteSitePageAndPagesInDB();
            addSitePagesToDB();
            indexAllSitePages();
        } catch (Exception ex) {
            logger.error("Error ", ex);
        }
    }

    @Override
    public void refreshPage(SitePage sitePage, URL url) {
        SitePage existsSitePage = siteRepository.getSitePagesByUrl(sitePage.getUrl());
        sitePage.setId(existsSitePage.getId());
        ConcurrentHashMap<String, Page> resultForkJoinPageIndexer = new ConcurrentHashMap<>();
        try {
            System.out.println("Indexing starts again " + url.getHost());
            PageFinder f = new PageFinder(siteRepository, pageRepository, sitePage, url.getPath(), resultForkJoinPageIndexer, connection, lemmaService, pageIndexerService, indexingProcessing);
            f.refreshPage();
        } catch (SecurityException ex) {
            SitePage site = siteRepository.findById(sitePage.getId()).orElseThrow();
            site.setStatus(Status.FAILED);
            site.setLastError(ex.getMessage());
            siteRepository.save(site);
        }
        System.out.println("Indexed site: " + sitePage.getName());
        SitePage site = siteRepository.findById(sitePage.getId()).orElseThrow();
        site.setStatus(Status.INDEXED);
        siteRepository.save(sitePage);
    }

    private void deleteSitePageAndPagesInDB() {
        List<SitePage> sitesFromDB = siteRepository.findAll();
        for (SitePage sitePageDB : sitesFromDB) {
            for (Site siteApp : sitesToIndexing.getSites()) {
                if (sitePageDB.getUrl().equals(siteApp.getUrl())) {
                    siteRepository.deleteById(sitePageDB.getId());
                }
            }
        }
    }

    private void addSitePagesToDB() {
        for (Site siteApp : sitesToIndexing.getSites()) {
            SitePage sitePageDAO = new SitePage();
            sitePageDAO.setName(siteApp.getName());
            sitePageDAO.setUrl(siteApp.getUrl().toString());
            sitePageDAO.setStatus(Status.INDEXING);
            siteRepository.save(sitePageDAO);
        }
    }

    private void indexAllSitePages() throws InterruptedException {
        sitePagesAllFromDB.addAll(siteRepository.findAll());
        List<String> urlToIndexing = new ArrayList<>();
        for (Site siteApp : sitesToIndexing.getSites()) {
            urlToIndexing.add(siteApp.getUrl().toString());
        }
        sitePagesAllFromDB.removeIf(sitePage -> !urlToIndexing.contains(sitePage.getUrl()));

        List<Thread> indexingThreadList = new ArrayList<>();
        for (SitePage siteDomain : sitePagesAllFromDB) {
            Runnable indexSite = () -> {
                try {
                    ConcurrentHashMap<String, Page> resultForkJoinIndexer = new ConcurrentHashMap<>();
                    System.out.println("Indexing started " + siteDomain.getUrl());
                    new ForkJoinPool().invoke(new PageFinder(pageIndexerService, lemmaService, siteRepository, pageRepository, indexingProcessing, connection, "", resultForkJoinIndexer));


                } catch (SecurityException ex) {
                    SitePage sitePage = siteRepository.findById(siteDomain.getId()).orElseThrow();
                    sitePage.setStatus(Status.FAILED);
                    sitePage.setLastError(ex.getMessage());
                    siteRepository.save(sitePage);
                }
                if(!indexingProcessing.get()) {
                    SitePage sitePage = siteRepository.findById(siteDomain.getId()).orElseThrow();
                    sitePage.setStatus(Status.FAILED);
                    sitePage.setLastError("Indexing stopped by user");
                    siteRepository.save(sitePage);
                } else {
                    System.out.println("Site was indexing " + siteDomain.getName());
                    SitePage sitePage = siteRepository.findById(siteDomain.getId()).orElseThrow();
                    sitePage.setStatus(Status.INDEXED);
                    siteRepository.save(sitePage);
                }
            };
            Thread thread = new Thread(indexSite);
            indexingThreadList.add(thread);
            thread.start();
        }
        for (Thread thread : indexingThreadList) {
            thread.join();
        }
        indexingProcessing.set(false);
    }

}
