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
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveAction;

public class SiteMapRecursiveAction extends RecursiveAction {

    private static final Logger log = LoggerFactory.getLogger(IndexingService.class);

    private SiteMap siteMap;
    private static CopyOnWriteArrayList linksPool = new CopyOnWriteArrayList();

    private final SiteEntity siteEntity;
    private final PageRepository pageRepository;
    ConcurrentSkipListSet<String> newPaths = new ConcurrentSkipListSet<>();

    public SiteMapRecursiveAction(SiteMap siteMap, SiteEntity siteEntity, PageRepository pageRepository) {
        this.siteMap = siteMap;
        this.siteEntity = siteEntity;
        this.pageRepository = pageRepository;
    }

    @Override
    protected void compute() {
        linksPool.add(siteMap.getUrl());

        //ConcurrentSkipListSet<String> links = HtmlParse.getLinks(siteMap.getUrl());
        Document doc = getDocumentByUrl(siteMap.getUrl());
        savePage(doc);
        ConcurrentSkipListSet<String> links = getLinks(siteMap.getUrl(),doc);
        for (String link : links) {
            if (!linksPool.contains(link)) {
                linksPool.add(link);
                siteMap.addChildren(new SiteMap(link));
            }
        }

        List<SiteMapRecursiveAction> taskList = new ArrayList<>();
        for (SiteMap child : siteMap.getSiteMapChildrens()) {
            SiteMapRecursiveAction task = new SiteMapRecursiveAction(child, siteEntity, pageRepository);
            task.fork();
            taskList.add(task);
        }
        for (SiteMapRecursiveAction task : taskList) {
            task.join();
        }
    }

    public ConcurrentSkipListSet<String> getLinks(String url, Document doc) {
//        Elements links = doc.select("a[href]");
//        ConcurrentSkipListSet<String> newPaths = new ConcurrentSkipListSet<>();
//        for (Element link : links) {
//            String nextUrl = link.attr("abs:href");
//            if (nextUrl.startsWith(url) && isLink(nextUrl) && !isFile(nextUrl)) {
//                String newPath = nextUrl.substring(url.length());
//                newPaths.add(newPath);
//            }
//        }
        Elements elements = doc.select("a[href~=^/?([\\w\\d/-]+)?]");
        for (Element link : elements) {
            String checkingUrl = link.attr("abs:href");
            if (checkingUrl.startsWith(siteMap.getDomain()) && !isFile(checkingUrl)) {
                newPaths.add(checkingUrl);
            }
        }
        return newPaths;
    }

    private static boolean isLink(String link) {
        String regex = "(^https:\\/\\/)(?:[^@\\/\\n]+@)?(?:www\\.)?([^:\\/\\n]+)";
        return link.matches(regex);
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

    public PageEntity savePage(Document doc) {
        if (doc == null) {
            log.warn("Failed to save page");
            return null;
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

        return page;
    }
}

