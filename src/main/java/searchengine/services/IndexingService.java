package searchengine.services;

import searchengine.dto.SuccessfulResponse;
import searchengine.model.SiteEntity;

public interface IndexingService {
    SuccessfulResponse startIndexing();

    SuccessfulResponse stopIndexing();

    SuccessfulResponse individualPage(String url);

    SiteEntity cleanUpAndSaveSite(String url, String name);

    void cleanSite(String url);
}
