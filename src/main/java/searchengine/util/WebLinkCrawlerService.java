package searchengine.util;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.config.SiteFromConfig;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class WebLinkCrawlerService {
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final SitesList sitesList;
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();


    public WebLinkCrawlerService(PageRepository pageRepository, SiteRepository siteRepository, SitesList sitesList) {
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.sitesList = sitesList;

    }

    public void startIndexing() {
        long before = System.currentTimeMillis();
        List<SiteFromConfig> fromConfigFile = sitesList.getSites();
        List<Site> siteList = new ArrayList<>();
        List<LinkCrawler> tasks = new ArrayList<>();

        siteRepository.deleteAll();// удалить все потому что запускается новая индексация

        fromConfigFile.forEach(siteFromConfig -> {
            Site site = new Site();
            site.setUrl(siteFromConfig.getUrl());
            site.setName(siteFromConfig.getName());
            site.setStatus(Site.Status.INDEXING);
            site.setLastError(null);
            site.setStatusTime(LocalDateTime.now());
            siteList.add(site);
            tasks.add(new LinkCrawler(site, site.getUrl()));
        });
        siteRepository.saveAll(siteList);
        tasks.forEach(forkJoinPool::invoke);
        long after = System.currentTimeMillis();
        log.error("Время выполнения  = {}, мс", (after-before));
    }


    private class LinkCrawler extends RecursiveAction {
        private final Site site;
        private final String path;

        public LinkCrawler(Site site, String path) {
            this.site = site;
            this.path = path;
        }

        @Override
        protected void compute() {

            if (pageRepository.existsByPath(path)) {
                return;
            }
            List<LinkCrawler> taskList = new ArrayList<>();
            final Page page;
            try {
                String tempUrl = path;
                if (site.getUrl().equals(path)) {
                    tempUrl = "/";
                }

                Connection connection = Jsoup.connect(site.getUrl() + tempUrl);
                log.warn("Переходим по ссылке  = {}",(site.getUrl() + tempUrl));

                Document document = connection
                        .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                        .referrer("http://www.google.com")
                        .timeout(0)
                        .get();

                int statusCode = connection.response().statusCode();
                String html = document.html();
                page = new Page();
                page.setSite(site);
                page.setCode(statusCode);
                page.setPath(tempUrl);
                page.setContent(html);

                pageRepository.save(page);

               getLinksForPage(document)
                       .forEach(link ->
                               taskList.add(new LinkCrawler(site, link)));
                invokeAll(taskList);
                site.setStatus(Site.Status.INDEXED);

            } catch (IOException e) {
                log.error("Ошибка при обработки страницы: {}", e.getMessage());
                site.setLastError(e.getMessage());
                site.setStatus(Site.Status.FAILED);
                throw new RuntimeException(e);
            }finally {
                siteRepository.save(site);
            }
        }


        private List<String> getLinksForPage(Document document) {
            Elements links = document.select("a");
            List<String> linksList = new ArrayList<>();
            for (Element link : links) {
                String absLink = link.attr("abs:href"); // Получаем абсолютный URL
                String relativeLink = link.attr("href");

                // Проверяем, что ссылка принадлежит текущему сайту и еще не посещена
                if (checkLinkOnCorrectFormat(absLink) && checkBaseUrl(absLink, link.baseUri()) && !relativeLink.isBlank()) {
                    if (pageRepository.existsByPath(relativeLink)) {
                        continue;
                    }
                    linksList.add(relativeLink);
                }
            }
            return linksList;
        }


        private static boolean checkLinkOnCorrectFormat(String link) {
            String regex = "^(https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([\\/\\w \\.-]*)*\\/?(?!#.*)$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(link);
            if (!matcher.matches()) {
                return false;
            }

            return (checkExtension(link));
        }

        private static boolean checkBaseUrl(String link, String baseURL) {
            return link.toLowerCase().startsWith(baseURL);
        }

        private static boolean checkExtension(String link) {
            String[] excludeFormats = {".jpg", ".jpeg", ".png", ".gif",
                    ".bmp", ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".zip", ".rar"};

            for (String extension : excludeFormats) {
                if (link.toLowerCase().endsWith(extension)) {
                    return false;
                }
            }
            return true;
        }
    }
}

