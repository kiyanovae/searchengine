package searchengine.services;

import org.jsoup.Connection;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

public interface SaverService {
    SiteEntity cleanUpAndSaveSite(String url, String name);

    SiteEntity saveSiteWithIndexedStatus(String url, String name);

    PageEntity savePage(SiteEntity site, Connection.Response response);

    PageEntity savePage(SiteEntity site, String path, int code, String content);

    LemmaEntity saveLemma(String lemma, SiteEntity site);
}
