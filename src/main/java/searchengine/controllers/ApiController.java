package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.ResponseWithError;
import searchengine.dto.statistics.QueryResponse;
import searchengine.dto.statistics.ResponseWithoutError;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexPageService;
import searchengine.services.SearchService;
import searchengine.services.StartIndexingService;
import searchengine.services.StatisticsService;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;
    private final StartIndexingService service;
    private final IndexPageService indexPageService;
    private final SearchService searchService;
    public static AtomicBoolean checkStartFlag=new AtomicBoolean(false);


    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
    @GetMapping("/startIndexing")
    public ResponseEntity startIndexing() {
        if (!checkStartFlag.get()) {
            service.startIndexing();
            checkStartFlag.set(true);
            return ResponseEntity.ok(new ResponseWithoutError(true));
        } else return ResponseEntity.status(404).body(new ResponseWithError(false,"Индексация уже запущена"));

    }
    @GetMapping("/stopIndexing")
    public ResponseEntity stopIndexing(){
        if (checkStartFlag.get()){
            checkStartFlag.set(false);
            return ResponseEntity.ok(new ResponseWithError(true,"")) ;
        }
        return ResponseEntity.status(400).body(new ResponseWithError(false,"Индексация не запущена")) ;
    }

    @PostMapping("/indexPage")
    public ResponseEntity indexPage(@RequestParam String url){
            try {
                indexPageService.indexPage(url);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
     return ResponseEntity.ok(new ResponseWithoutError(true));
    }
    @GetMapping("/search")
    public ResponseEntity search(@RequestParam(name = "query") String query,
                                 @RequestParam(name = "site", required = false,defaultValue = "") String site,
                                 @RequestParam(required = false,defaultValue = "0") int offset,
                                 @RequestParam(required = false,defaultValue = "10") int limit) throws IOException {
        if (query.equals("")){
            return ResponseEntity.status(400).body(new ResponseWithError(false,"Задан пустой поисковый запрос"));
        } else
            return ResponseEntity.ok(searchService.search(query,site,offset,limit));
    }

}
