package searchengine.services;

import searchengine.model.PageEntity;

public interface UpdaterService {
    void deleteIndexesContainsPage(PageEntity page);

    void updateLemma(int id);

    void cleanSite(String url);
}
