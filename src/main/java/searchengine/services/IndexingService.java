package searchengine.services;

import searchengine.dto.SuccessfulResponse;

public interface IndexingService {
    SuccessfulResponse startIndexing();

    SuccessfulResponse stopIndexing();

    SuccessfulResponse individualPage(String url);
}
