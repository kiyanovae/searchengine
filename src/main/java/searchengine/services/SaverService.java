package searchengine.services;

import org.jsoup.Connection;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.io.IOException;

public interface SaverService {
    SiteEntity cleanUpAndSaveSite(String url, String name);

    SiteEntity saveSiteWithIndexedStatus(String url, String name);

    PageEntity savePage(SiteEntity site, String path, Connection.Response response) throws IOException;

    boolean savePage(SiteEntity site, String path, int code, String content);

    LemmaEntity saveLemma(String lemma, SiteEntity site);
}
