package searchengine.services.impl;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.Connection;
import searchengine.model.Page;
import searchengine.model.SitePage;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.LemmaService;
import searchengine.services.PageIndexerService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RequiredArgsConstructor
@AllArgsConstructor
public class PageFinder extends RecursiveAction {

    private final PageIndexerService pageIndexerService;
    private final LemmaService lemmaService;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final AtomicBoolean indexingProcessing;
    private final Connection connection;
    private final Set<String> urlSet = new HashSet<>();
    private final String page;
    private SitePage siteDomain;
    private final ConcurrentHashMap<String, Page> resultForkJoinPoolIndexedPages;

    public PageFinder(SiteRepository siteRepository, PageRepository pageRepository, SitePage siteDomain, String page, ConcurrentHashMap<String, Page> resultForkJoinPoolIndexedPages, Connection connection, LemmaService lemmaService, PageIndexerService pageIndexerService, AtomicBoolean indexingProcessing) {
        this.pageIndexerService = pageIndexerService;
        this.lemmaService = lemmaService;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.indexingProcessing = indexingProcessing;
        this.connection = connection;
        this.page = page;
        this.resultForkJoinPoolIndexedPages = resultForkJoinPoolIndexedPages;
    }

    @Override
    protected void compute() {
        if (resultForkJoinPoolIndexedPages.get(page) != null || indexingProcessing.get()) {
            return;
        }
        Page indexingPage = new Page();
        indexingPage.setPath(page);
        indexingPage.setSiteId(siteDomain.getId());
        try {
            org.jsoup.Connection connect = Jsoup.connect(siteDomain.getUrl() + page)
                    .userAgent(connection.getUserAgent()).referrer(connection.getReferer());

            Document doc = connect.timeout(60000).get();

            indexingPage.setContent(doc.head() + String.valueOf(doc.body()));
            Elements pages = doc.getElementsByTag("a");

            for (Element element : pages) {
                if (!element.attr("href").isEmpty() && element.attr("href").charAt(0) == '/') {
                    if (resultForkJoinPoolIndexedPages.get(page) != null || indexingProcessing.get()) {
                        return;
                    } else if (resultForkJoinPoolIndexedPages.get(element.attr("href")) == null) {
                        urlSet.add(element.attr("href"));
                    }
                }
            }
            indexingPage.setCode(doc.connection().response().statusCode());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (resultForkJoinPoolIndexedPages.get(page) != null || indexingProcessing.get()) {
            return;
        }
        resultForkJoinPoolIndexedPages.putIfAbsent(indexingPage.getPath(), indexingPage);
        SitePage sitePage = siteRepository.findById(siteDomain.getId()).orElseThrow();
        sitePage.setStatusTime(LocalDateTime.now());
        siteRepository.save(sitePage);
        pageRepository.save(indexingPage);
        pageIndexerService.indexHtml(indexingPage.getContent(), indexingPage);
        List<PageFinder> indexingPagesTasks = new ArrayList<>();
        for (String url : urlSet) {
            if (resultForkJoinPoolIndexedPages.get(url) == null && indexingProcessing.get()) {
                PageFinder task = new PageFinder(siteRepository, pageRepository, sitePage, url, resultForkJoinPoolIndexedPages, connection, lemmaService, pageIndexerService, indexingProcessing);
                task.fork();
                indexingPagesTasks.add(task);
            }
        }
        for (PageFinder page : indexingPagesTasks) {
            if (!indexingProcessing.get()) {
                return;
            }
            page.join();
        }
    }

    public void refreshPage() {

        Page indexingPage = new Page();
        indexingPage.setPath(page);
        indexingPage.setSiteId(siteDomain.getId());

        try {
            org.jsoup.Connection connect = Jsoup.connect(siteDomain.getUrl() + page).userAgent(connection.getUserAgent()).referrer(connection.getReferer());
            Document doc = connect.timeout(60000).get();
            indexingPage.setContent(doc.head() + String.valueOf(doc.body()));
            indexingPage.setCode(doc.connection().response().statusCode());
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        SitePage sitePage = siteRepository.findById(siteDomain.getId()).orElseThrow();
        sitePage.setStatusTime(LocalDateTime.now());
        siteRepository.save(sitePage);

        Page pageToRefresh = pageRepository.findPageBySiteIdAndPath(page, sitePage.getId());
        pageToRefresh.setCode(indexingPage.getCode());
        pageToRefresh.setContent(indexingPage.getContent());
        pageRepository.save(pageToRefresh);

        pageIndexerService.refreshIndex(indexingPage.getContent(), pageToRefresh);
    }








}
