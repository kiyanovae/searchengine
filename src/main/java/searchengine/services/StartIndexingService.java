package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.logicClasses.DeleteLemma;
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
import java.util.concurrent.ForkJoinPool;


@Service
@RequiredArgsConstructor

public class StartIndexingService {
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    public static boolean flag = true;

    public void startIndexing() {

        flag = true;

        tableClearing();

        for (Site sites : sites.getSites()) {
            Thread thread = new Thread(() -> {
                SiteTable siteTable = new SiteTable();
                try {
                    Document document = Jsoup.connect(sites.getUrl()).
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

                    Thread dateThread = new Thread(() -> {
                        while (!Thread.currentThread().isInterrupted()) {
                            try {
                                Thread.sleep(2000);
                                siteTable.setStatusTime(new Date());
                            } catch (InterruptedException e) {
                                e.getMessage();
                                break;
                            }
                            siteRepository.save(siteTable);
                        }
                    });
                    dateThread.start();

                    ForkJoinPool pool = new ForkJoinPool();
                    FillingTablePage page = new FillingTablePage(sites.getUrl(), siteTable, pageRepository, indexRepository, lemmaRepository);
                    pool.invoke(page);

                    if (flag) {
                        siteTable.setStatus(SiteStatus.INDEXED);
                        siteRepository.save(siteTable);
                    } else {
                        siteTable.setStatus(SiteStatus.FAILED);
                        siteTable.setLastError("Индексация остановлена пользователем");
                        siteRepository.save(siteTable);
                    }
                    dateThread.interrupt();
                }
            });
            thread.start();
        }
        flag = true;
    }

    public void stopIndexing() {
        flag = false;
    }

    private void tableClearing() {
        for (Site site : sites.getSites()) {
            if (siteRepository.findByUrl(site.getUrl()) != null) {
                List<Page> pageList = pageRepository.findAllBySite(siteRepository.findByUrl(site.getUrl()));
                if (!pageList.isEmpty()) {
                    for (Page page : pageList) {
                        ForkJoinPool pool = new ForkJoinPool();
                        DeleteLemma delete = new DeleteLemma(indexRepository, lemmaRepository, page);
                        pool.invoke(delete);
                        pageRepository.delete(page);
                    }
                }
                siteRepository.delete(siteRepository.findByUrl(site.getUrl()));
            }

        }
    }
}
