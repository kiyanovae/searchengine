package searchengine.logicClasses;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.Page;
import searchengine.model.SiteTable;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.services.StartIndexingService;

import java.io.IOException;
import java.util.concurrent.RecursiveAction;

@RequiredArgsConstructor
public class FillingTablePage extends RecursiveAction {
    private String url;
    private final PageRepository pageRepository;
    private SiteTable site;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;

    public FillingTablePage(String url, SiteTable site, PageRepository pageRepository, IndexRepository indexRepository, LemmaRepository lemmaRepository) {
        this.url = url;
        this.site = site;
        this.pageRepository = pageRepository;
        this.indexRepository = indexRepository;
        this.lemmaRepository = lemmaRepository;
    }

    @Override
    protected void compute() {
        try {
            Thread.sleep(150);
            Document document = Jsoup.connect(url).
                    userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .get();

            Page page = new Page();
            page.setPath(url.substring(site.getUrl().length() - 1));
            page.setSite(site);
            page.setCode(200);
            page.setContent(String.valueOf(document));
            pageRepository.save(page);

            FillingLemmaAndIndex fillingLemmaAndIndex = new FillingLemmaAndIndex(indexRepository, lemmaRepository);
            fillingLemmaAndIndex.fillingLemmaIndex(page, StartIndexingService.flag);

            Elements elements = document.select("a");
            for (Element e : elements) {
                String newHref = e.absUrl("href").replaceAll("/$", "");
                if (pageRepository.findByPath(newHref.substring(site.getUrl().length() - 1)) != null || newHref.isEmpty() ||
                        !newHref.contains(site.getUrl()) ||
                        newHref.contains(".pdf") || newHref.contains(".png") ||
                        newHref.contains(".jpg") || newHref.contains("#") ||
                        newHref.contains(";") || newHref.contains(".com") || newHref.contains(".net") ||
                        newHref.contains(".sql") || newHref.contains(".zip")) {
                    continue;
                }

                if (StartIndexingService.flag) {
                    FillingTablePage pageIndexing = new FillingTablePage(newHref, site, pageRepository, indexRepository, lemmaRepository);
                    pageIndexing.fork();
                    pageIndexing.join();
                } else break;
            }
        } catch (RuntimeException | IOException | InterruptedException e) {
            e.getMessage();
        }
    }
}
