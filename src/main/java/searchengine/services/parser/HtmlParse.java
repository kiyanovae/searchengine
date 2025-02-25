package searchengine.services.parser;

import lombok.Getter;
import org.jsoup.Connection;
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

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ConcurrentSkipListSet;

import static java.lang.Thread.sleep;

public class HtmlParse {

    private static final Logger log = LoggerFactory.getLogger(IndexingService.class);

    private static ConcurrentSkipListSet<String> links;

    private static SiteEntity siteEntity;
    private static SiteMap siteMap;
    private static PageRepository pageRepository;

    public HtmlParse(SiteEntity siteEntity, SiteMap siteMap, PageRepository pageRepository) {
        HtmlParse.siteEntity = siteEntity;
        HtmlParse.siteMap = siteMap;
        HtmlParse.pageRepository = pageRepository;
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

    public static ConcurrentSkipListSet<String> getLinks(String url) {
        links = new ConcurrentSkipListSet<>();
        try {
            sleep(550);
            Connection connection = Jsoup.connect(url)
                    .ignoreHttpErrors(true)
                    .userAgent("Firefox")
                    .timeout(30 * 1000);
            Document document = connection.get();
            Elements elements = document.select("body").select("a");
            for (Element element : elements) {
                String link = element.absUrl("href");
                if (isLink(link) && !isFile(link)) {
                    links.add(link);
                    savePage(document);
                }
            }
        } catch (InterruptedException e) {
            System.out.println(e + " - " + url);
        } catch (SocketTimeoutException e) {
            System.out.println(e + " - " + url);
        } catch (IOException e) {
            System.out.println(e + " - " + url);
        }
        return links;
    }

    public static PageEntity savePage(Document doc) {
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
