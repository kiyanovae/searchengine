package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.SuccessfulResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class ApiController {
    private static final String DEFAULT_OFFSET = "0";
    private static final String DEFAULT_LIMIT = "20";

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
    public ResponseEntity<SearchResponse> search(@RequestParam(required = false) String query,
                                                 @RequestParam(required = false) String site,
                                                 @RequestParam(required = false,
                                                         defaultValue = DEFAULT_OFFSET) Integer offset,
                                                 @RequestParam(required = false,
                                                         defaultValue = DEFAULT_LIMIT) Integer limit) {
        return ResponseEntity.ok(searchService.search(query, site, offset, limit));
    }
}
