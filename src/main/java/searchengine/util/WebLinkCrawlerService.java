package searchengine.util;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WebLinkCrawlerService {
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final SitesList sitesList;
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();

    private ExecutorService executorService;


    public WebLinkCrawlerService(PageRepository pageRepository, SiteRepository siteRepository, SitesList sitesList) {
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.sitesList = sitesList;
    }

    public void startIndexing() {
        List<SiteFromConfig> sites = sitesList.getSites();
        List<Site> urlsFromConfig = new ArrayList<>();
        List<LinkCrawler> tasks = new ArrayList<>();

        siteRepository.deleteAll();// если имеются сайты то удалить все потому что запускается новая индексация

        sites.forEach(siteFromConfig -> {
            Site site = new Site();
            site.setUrl(siteFromConfig.getUrl());
            site.setName(siteFromConfig.getName());
            site.setStatus(searchengine.model.Site.Status.INDEXING);
            site.setLastError(null);
            site.setStatusTime(LocalDateTime.now());
            urlsFromConfig.add(site);
            tasks.add(new LinkCrawler(site, site.getUrl()));
        });
        siteRepository.saveAll(urlsFromConfig);
        tasks.forEach(forkJoinPool::invoke);
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
                Connection connection = Jsoup.connect(path);

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
                page.setPath(path);
                page.setContent(html);
                pageRepository.save(page);
                List<String> linksForPage = getLinksForPage(document);
                linksForPage.forEach(task -> taskList.add(new LinkCrawler(site, task)));
                invokeAll(taskList);

            } catch (IOException e) {
                System.out.println("Что-то пошло не так");
                throw new RuntimeException(e);
            }
        }


        private List<String> getLinksForPage(Document document) {
            Elements links = document.select("a[href]");
            List<String> linksList = new ArrayList<>();
            for (Element link : links) {
                String href = link.attr("abs:href"); // Получаем абсолютный URL

                // Проверяем, что ссылка принадлежит текущему сайту и еще не посещена
                if (checkLinkOnCorrectFormat(href) && checkBaseURL(href, link.baseUri()) && !pageRepository.existsByPath(href)) {
                    System.out.println("Ссылка подходит: " + href);
                    linksList.add(href);
                }
            }
            return linksList;
        }


        private static boolean checkLinkOnCorrectFormat(String link) {
            String regex = "^(https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([\\/\\w \\.-]*)*\\/?$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(link);
            if (!matcher.matches()) {
                return false;
            }

            return (checkExtension(link));
        }

        private static boolean checkBaseURL(String link, String baseURL) {
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
