package searchengine.services;

import org.jsoup.Connection;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

public interface PageIndexerService {
    void handle(SiteEntity site, Connection.Response response);

    void index(SiteEntity site, PageEntity page);
}