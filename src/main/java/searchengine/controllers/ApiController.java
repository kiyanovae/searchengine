package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.SuccessfulResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.exceptions.BadRequestException;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class ApiController {
    private static final int DEFAULT_OFFSET = 0;
    private static final int DEFAULT_LIMIT = 20;

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> getStatistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<SuccessfulResponse> startIndexing() {
        return ResponseEntity.ok(indexingService.startIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<SuccessfulResponse> stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<SuccessfulResponse> indexPage(@RequestParam String url) {
        return ResponseEntity.ok(indexingService.individualPage(url));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestParam(required = false) String query, @RequestParam(required = false) String site,
                                                 @RequestParam(required = false) Integer offset, @RequestParam(required = false) Integer limit) {
        if (query == null || query.isEmpty()) {
            throw new BadRequestException("Search query must not be empty");
        }
        if (offset == null) {
            offset = DEFAULT_OFFSET;
        } else if (offset < 0) {
            throw new BadRequestException("The 'OFFSET' parameter must not be negative");
        }
        if (limit == null) {
            limit = DEFAULT_LIMIT;
        } else if (limit < 0) {
            throw new BadRequestException("The 'LIMIT' parameter must not be negative");
        }
        SearchResponse response;
        if (site != null) {
            response = searchService.search(query, site, offset, limit);
        } else {
            response = searchService.searchAll(query, offset, limit);
        }
        return ResponseEntity.ok(response);
    }
}
