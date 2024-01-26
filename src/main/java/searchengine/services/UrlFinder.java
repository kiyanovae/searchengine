package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.exceptions.RequestException;
import searchengine.model.PageEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

@Slf4j
public class UrlFinder extends RecursiveAction {
    private static String userAgent;
    private static String referrer;
    private final PageIndexerService pageIndexerService;
    private final int siteId;
    private final String mainUrl;
    private final String currentUrl;

    public UrlFinder(PageIndexerService pageIndexerService, int siteId, String mainUrl, String currentUrl) {
        this.pageIndexerService = pageIndexerService;
        this.siteId = siteId;
        this.mainUrl = mainUrl;
        this.currentUrl = currentUrl;
    }

    public static void setUserAgent(String userAgent) {
        UrlFinder.userAgent = userAgent;
    }

    public static void setReferrer(String referrer) {
        UrlFinder.referrer = referrer;
    }

    public String getMainUrl() {
        return mainUrl;
    }

    protected void compute() {
        try {
            if (IndexingServiceImpl.getIsStoppedByUser()) {
                throw new RequestException("Индексация остановлена пользователем");
            }
            Thread.sleep(200);
            Connection.Response response = Jsoup.connect(currentUrl).userAgent(userAgent).referrer(referrer).ignoreHttpErrors(true).execute();
            Document urlDocument = response.parse();
            String urlPath = currentUrl.substring(mainUrl.length() - 1);
            int statusCode = response.statusCode();
            String content = urlDocument.outerHtml();
            PageEntity pageEntity = pageIndexerService.getNewPage(siteId, urlPath, statusCode, content);
            if (pageEntity == null) {
                return;
            }
            if (statusCode < 400) {
                pageIndexerService.indexPage(siteId, pageEntity.getId(), content);
            }
            log.info(currentUrl.concat(" +++"));
            findUrlsAndCreateTasks(urlDocument);
        } catch (UnsupportedMimeTypeException ignored) {
        } catch (IOException | InterruptedException e) {
            log.warn(currentUrl.concat(" - ").concat(e.getMessage()));
            throw new RequestException("Ошибка индексации: ".concat(currentUrl).concat(" - ").concat(e.getMessage()));
        }
    }

    private void findUrlsAndCreateTasks(Document document) {
        Elements links = document.select("a[href]");
        List<UrlFinder> taskList = new ArrayList<>();
        for (Element link : links) {
            String url = link.attr("abs:href");
            if (isUrlValid(url, siteId)) {
                UrlFinder task = new UrlFinder(pageIndexerService, siteId, mainUrl, url);
                task.fork();
                taskList.add(task);
            }
        }
        taskList.forEach(ForkJoinTask::join);
    }

    private boolean isUrlValid(String url, int siteId) {
        if (!url.startsWith(mainUrl) || url.contains("#")) {
            return false;
        }
        return pageIndexerService.isUrlUnexplored(url.substring(mainUrl.length() - 1), siteId);
    }
}