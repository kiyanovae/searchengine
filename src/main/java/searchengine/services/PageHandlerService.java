package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import searchengine.exceptions.StoppedByUserException;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

@Slf4j
@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "connection-settings")
public class PageHandlerService extends RecursiveAction {
    private static final int MAX_REPEAT = 5;
    private static final int MAX_STATUS_CODE = 400;
    private static final int TIMEOUT_MILLISECONDS = 60000;
    @Setter
    private String userAgent;
    @Setter
    private String referrer;
    @Setter
    @Getter
    private SiteEntity site;
    @Setter
    private String baseUri;
    @Setter
    private String pageUrl;
    @Setter
    private int repeatCount;
    private final SaverService saverService;
    private final StatusService statusService;
    private final PageIndexerService pageIndexerService;
    private final PageRepository pageRepository;

    @Override
    protected void compute() {
        try {
            if (statusService.isIndexingStoppedByUser()) {
                throw new StoppedByUserException("Indexing stopped by the user");
            }
            Thread.sleep(150);
            Connection.Response response = Jsoup.connect(pageUrl)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .ignoreHttpErrors(true)
                    .timeout(TIMEOUT_MILLISECONDS)
                    .execute();
            PageEntity page;
            synchronized (saverService) {
                page = saverService.savePage(site, response);
            }
            if (page == null) {
                return;
            }
            if (page.getCode() >= MAX_STATUS_CODE) {
                return;
            }
            pageIndexerService.index(site, page);
            List<PageHandlerService> newTasks = parseHtml(response.parse());
            log.info("{} indexed", pageUrl);
            newTasks.forEach(ForkJoinTask::join);
        } catch (UnsupportedMimeTypeException ignored) {
        } catch (InterruptedException e) {
            throw new RuntimeException("Indexing error '" + pageUrl + "' - " + e.getMessage());
        } catch (IOException e) {
            if (repeatCount == MAX_REPEAT) {
                throw new RuntimeException("Indexing error '" + pageUrl + "' - " + e.getMessage());
            } else {
                PageHandlerService task = createTask(pageUrl);
                task.setRepeatCount(repeatCount + 1);
                statusService.incrementTaskCount();
                task.fork();
                task.join();
            }
        } finally {
            statusService.decrementTaskCount();
        }
    }

    private List<PageHandlerService> parseHtml(Document doc) {
        Elements elementsContainsLinks = doc.select("a[href]");
        List<PageHandlerService> taskList = new ArrayList<>();
        for (Element element : elementsContainsLinks) {
            String url = element.attr("abs:href");
            if (!site.getStatus().equals(SiteEntity.SiteStatus.FAILED) && isValidUrl(url)) {
                String path = url.substring(site.getUrl().length());
                if (isExploredPage(path)) {
                    continue;
                }
                PageHandlerService task = createTask(url);
                statusService.incrementTaskCount();
                taskList.add(task);
                task.fork();
            }
        }
        return taskList;
    }

    private PageHandlerService createTask(String url) {
        PageHandlerService task = getPageHandlerService();
        task.setSite(site);
        task.setBaseUri(baseUri);
        task.setPageUrl(url);
        return task;
    }

    @Lookup
    public PageHandlerService getPageHandlerService() {
        return null;
    }

    private boolean isExploredPage(String path) {
        return pageRepository.existsByPathAndSite(path, site);
    }

    private boolean isValidUrl(String url) {
        return url.startsWith(baseUri) && !url.contains("#") && !url.contains("?");
    }
}