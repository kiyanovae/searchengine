package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.controllers.ApiController;
import searchengine.logicClasses.DeleteLemma;
import searchengine.logicClasses.FillingLemmaAndIndex;
import searchengine.logicClasses.FillingTablePage;
import searchengine.model.Page;
import searchengine.model.SiteStatus;
import searchengine.model.SiteTable;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

@EnableAsync
@Service
@RequiredArgsConstructor

public class StartIndexingService {
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final FillingLemmaAndIndex fillingLemmaAndIndex;
    private final DeleteLemma delete;

    @Async
    public void startIndexing() {

        tableClearing();

        ExecutorService service= Executors.newFixedThreadPool(sites.getSites().size());
        for (Site sites : sites.getSites()) {
            service.execute(() -> {
                SiteTable siteTable = new SiteTable();
                try {
                    Jsoup.connect(sites.getUrl()).
                            userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                            .referrer("http://www.google.com").get();
                } catch (IOException e) {
                    siteTable.setName(sites.getName());
                    siteTable.setUrl(sites.getUrl());
                    siteTable.setStatus(SiteStatus.FAILED);
                    siteTable.setStatusTime(new Date());
                    siteTable.setLastError("Указанная страница не найдена");
                    siteRepository.save(siteTable);
                    e.getMessage();
                }

                if (siteTable.getName() == null) {
                    siteTable.setName(sites.getName());
                    siteTable.setUrl(sites.getUrl());
                    siteTable.setStatus(SiteStatus.INDEXING);
                    siteTable.setStatusTime(new Date());
                    siteRepository.save(siteTable);

                    ScheduledExecutorService time = Executors.newSingleThreadScheduledExecutor();
                    time.scheduleAtFixedRate(() -> {
                        siteTable.setStatusTime(new Date());
                        siteRepository.save(siteTable);
                    }, 0, 1, TimeUnit.SECONDS);

                    ForkJoinPool pool = new ForkJoinPool();
                    FillingTablePage page = new FillingTablePage(sites.getUrl(), siteTable, pageRepository, indexRepository, lemmaRepository, fillingLemmaAndIndex);
                    pool.invoke(page);

                    if (ApiController.checkStartFlag.get()) {
                        siteTable.setStatus(SiteStatus.INDEXED);
                        siteRepository.save(siteTable);
                    } else {
                        siteTable.setStatus(SiteStatus.FAILED);
                        siteTable.setLastError("Индексация остановлена пользователем");
                        siteRepository.save(siteTable);
                    }
                    time.shutdown();

                }
            });
        }
    }

    private void tableClearing() {
        ExecutorService service= Executors.newFixedThreadPool(sites.getSites().size());
        for (Site site : sites.getSites()) {
            SiteTable detectedSite=siteRepository.findByUrl(site.getUrl());
            if (detectedSite != null) {
                service.execute(() -> {
                    List<Page> pageList = pageRepository.findAllBySiteId(detectedSite.getId());
                    if (!pageList.isEmpty()) {
                        for (Page page : pageList) {
                            delete.deleteLemmaAndIndex(page);
                            pageRepository.delete(page);
                        }
                    }
                    siteRepository.delete(detectedSite);
                });
            }
        }
        List< Page> pageListWithError=pageRepository.findAllByCode(400);
        pageRepository.deleteAll(pageListWithError);
    }
}
