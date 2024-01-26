package searchengine.services;

import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.io.IOException;

public interface PageIndexerService {
    void indexPage(SiteEntity siteEntity, String path, int code, String content) throws IOException;
    void indexPage(int siteId, int pageId, String content) throws IOException;
    //void flushLemmaIndex();
    //void flushIndexEntities();
    PageEntity getNewPage(int siteId ,String path, int code, String content);
    boolean isUrlUnexplored(String url, int siteId);
}
