package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.ResponseWithError;
import searchengine.dto.statistics.ResponseWithoutError;
import searchengine.logicClasses.DeleteLemma;
import searchengine.logicClasses.FillingLemmaAndIndex;
import searchengine.model.Page;
import searchengine.model.SiteStatus;
import searchengine.model.SiteTable;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexPageService {
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    public void indexPage(String url) throws IOException {

        String siteFromUrl = url;
        String http = siteFromUrl.substring(0, siteFromUrl.indexOf("://") + 3);
        siteFromUrl = siteFromUrl.substring(url.indexOf("://") + 3);
        siteFromUrl = siteFromUrl.substring(0, siteFromUrl.indexOf("/") + 1);
        siteFromUrl = http + siteFromUrl;

        SiteTable site = siteRepository.findByUrl(siteFromUrl);
        if (site == null) {
            for (Site s : sites.getSites()) {
                if (url.contains(s.getUrl())) {
                    site = new SiteTable();
                    site.setName(s.getName());
                    site.setUrl(s.getUrl());
                    site.setStatusTime(new Date());
                    site.setStatus(SiteStatus.INDEXED);
                    siteRepository.save(site);
                    break;
                }
            }
        }
        if (site == null) {
            Page page=new Page();
            page.setPath(url);
            page.setCode(400);
            page.setContent("Данная страница находится за пределами сайтов, \n" +
                    "указанных в конфигурационном файле");
            pageRepository.save(page);
            return;
        }

        Document document;
        try {
            document = Jsoup.connect(url).
                    userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com").get();
        } catch (IOException e) {
            Page page=new Page();
            page.setPath(url.substring(site.getUrl().length() - 1));
            page.setCode(500);
            page.setSite(site);
            e.getMessage();
            return;
        }

        String path = url.substring(site.getUrl().length() - 1);

        Page detectedPage = pageRepository.findByPath(path);
        if (detectedPage != null) {
            deletePageLemmaIndex(detectedPage);
        }

        Page page = new Page();
        page.setPath(path);
        page.setSite(site);
        page.setCode(200);
        page.setContent(String.valueOf(document));
        pageRepository.save(page);
        FillingLemmaAndIndex fillingLemmaAndIndex = new FillingLemmaAndIndex(indexRepository, lemmaRepository);
        fillingLemmaAndIndex.fillingLemmaIndex(page, true);
    }



    public void deletePageLemmaIndex(Page page) {
        ForkJoinPool pool = new ForkJoinPool();
        DeleteLemma delete = new DeleteLemma(indexRepository, lemmaRepository, page);
        pool.invoke(delete);
        pageRepository.delete(page);
    }

}
