package searchengine.services;

import searchengine.model.Page;
import searchengine.model.Site;

public interface LemmaService {

    void saveLemma(Site site, Page page);
}
