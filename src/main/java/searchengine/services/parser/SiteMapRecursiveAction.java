package searchengine.services.parser;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import searchengine.services.IndexingService;
import searchengine.services.PageRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class SiteMapRecursiveAction extends RecursiveAction {

    private static final Logger log = LoggerFactory.getLogger(IndexingService.class);

    private SiteMap siteMap;
    private static Set<String> linksPool = ConcurrentHashMap.newKeySet();

    private final SiteEntity siteEntity;
    private final PageRepository pageRepository;

    public SiteMapRecursiveAction(SiteMap siteMap, SiteEntity siteEntity, PageRepository pageRepository) {
        this.siteMap = siteMap;
        this.siteEntity = siteEntity;
        this.pageRepository = pageRepository;
    }

    @Override
    protected void compute() {
        linksPool.add(siteMap.getUrl());

        Document doc = getDocumentByUrl(siteMap.getUrl());
        savePage(doc);
        Set<String> links = getLinks(doc);

        List<SiteMapRecursiveAction> taskList = new ArrayList<>();
        for (String link : links) {
            if (linksPool.add(link)) {
                SiteMap childSiteMap = new SiteMap(link);
                siteMap.addChildren(childSiteMap);
                SiteMapRecursiveAction task = new SiteMapRecursiveAction(childSiteMap, siteEntity, pageRepository);
                task.fork();
                taskList.add(task);
            }
        }

        for (SiteMapRecursiveAction task : taskList) {
            task.join();
        }
    }

    public Set<String> getLinks(Document doc) {
        return doc.select("a[href]").stream()
                .map(link -> link.attr("abs:href"))
                .filter(url -> url.startsWith(siteMap.getDomain()))
                .filter(url -> !isFile(url))
                .collect(Collectors.toCollection(ConcurrentHashMap::newKeySet));
    }

    private static boolean isFile(String link) {
        link = link.toLowerCase();
        return link.contains(".jpg")
                || link.contains(".jpeg")
                || link.contains(".png")
                || link.contains(".gif")
                || link.contains(".webp")
                || link.contains(".pdf")
                || link.contains(".eps")
                || link.contains(".xlsx")
                || link.contains(".doc")
                || link.contains(".pptx")
                || link.contains(".docx")
                || link.contains("?_ga");
    }

    public Document getDocumentByUrl(String url) {
        Document doc = null;
        try {
            doc = Jsoup.connect(url)
                    .ignoreHttpErrors(true)
                    .userAgent("Firefox")
                    .timeout(30 * 1000)
                    .get();
        } catch (Exception e) {
            log.error("Ошибка в методе getDocumentByUrl");
        }
        return doc;
    }

    public void savePage(Document doc) {
        if (doc == null) {
            log.warn("Failed to save page");
            return;
        }
        String content = "";
        try {
            content = doc.body().text();
        } catch (Exception e) {
            log.warn("Ошибка при получении контекста страницы: {}", siteMap.getUrl());
        }

        String title = "";
        try {
            title = doc.title();
        } catch (Exception e) {
            log.warn("Ошибка при получении заголовка страницы: {}", siteMap.getUrl());
        }

        String path = siteMap.getUrl().substring(siteMap.getDomain().length());
        if (!path.endsWith("/")) {
            path = path + "/";
        }

        Integer statusCode = doc.connection().response().statusCode();
        PageEntity page = new PageEntity();
        page.setSite(siteEntity);
        page.setPath(path);
        page.setCode(statusCode);
        page.setContent(content);

        pageRepository.save(page);

        log.info("Domain: {}", siteMap.getDomain());
        log.info("URL: {}", siteMap.getUrl());
        log.info("Path: {}", path);

    }
}

