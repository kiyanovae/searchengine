package searchengine.services;

import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

public interface PageIndexerService {
    void handle(SiteEntity site, String path, int code, String content);

    void index(SiteEntity site, PageEntity page);
}