package searchengine.services;

import searchengine.config.Page;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Set;

public interface PageSearcherService {
    List<Page> findPagesBySite(SiteEntity site, Set<String> queryLemmaSet);
}
