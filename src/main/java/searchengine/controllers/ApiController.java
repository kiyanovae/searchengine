package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.SuccessfulResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.exceptions.RequestException;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import java.io.IOException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class ApiController {

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
        return ResponseEntity.ok(indexingService.indexPage(url));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestParam String query, @RequestParam String site, @RequestParam int offset, @RequestParam int limit) {
        try {
            SearchResponse response = searchService.search(query, site, offset, limit);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            throw new RequestException(e.getMessage());
        }
    }
}
