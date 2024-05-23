package searchengine.services;

import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

public interface UpdaterService {
    PageEntity cleanAndUpdatePage(SiteEntity site, String path, int code, String content);

    void cleanSite(String url);
}
