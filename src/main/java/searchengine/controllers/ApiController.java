package searchengine.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.IndexingSettings;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.models.Site;
import searchengine.services.ConfigurationServices.ServicesFlowManagement.IndexingStatusManager;
import searchengine.services.impl.StatisticsService;
import searchengine.services.indexing.CheckLinkInScopeLinks;
import searchengine.services.indexing.IndexingOnePage;
import searchengine.services.indexing.SitesIndexingService;
import searchengine.services.search.SortedLemmas;
import searchengine.utils.UrlUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;
    private final SitesIndexingService sitesIndexingService;
    private final IndexingSettings indexingSettings;
    private final IndexingStatusManager indexingStatusManager;
    private final IndexingOnePage indexingOnePage;
    private final CheckLinkInScopeLinks checkLinkInScopeLinks;
    private final SortedLemmas sortedLemmas;

    public static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    public ApiController(StatisticsService statisticsService,
                         SitesIndexingService sitesIndexingService,
                         IndexingSettings indexingSettings,
                         IndexingStatusManager indexingStatusManager,
                         IndexingOnePage indexingOnePage,
                         CheckLinkInScopeLinks checkLinkInScopeLinks,
                         SortedLemmas sortedLemmas) {
        this.statisticsService = statisticsService;
        this.sitesIndexingService = sitesIndexingService;
        this.indexingSettings = indexingSettings;
        this.indexingStatusManager = indexingStatusManager;
        this.indexingOnePage = indexingOnePage;
        this.checkLinkInScopeLinks = checkLinkInScopeLinks;
        this.sortedLemmas = sortedLemmas;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Map<String, Object>> startIndexing(){
        Map<String, Object> response = new HashMap<>();

        if (!indexingStatusManager.startIndexing()){
            response.put("result", false);
            response.put("error", "Индексация уже запущена. ");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        try {
            List<String> siteUrl = indexingSettings.getSites().stream()
                            .map(Site::getUrl)
                            .toList();

            sitesIndexingService.indexSites(siteUrl);
            response.put("result", true);
            return ResponseEntity.ok(response);

        }catch (Exception e) {
            response.put("result", false);
            response.put("error", "Не удалось запустить индексацию: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Map<String, Object>> stopIndexing() {      //TODO Настроить стоп по требованию.
                                                                     // кнопка появляется, если дождаться окончания полной иднексации*.


        logger.info("Остановка по кнопке 'stop indexing'...");

        Map<String, Object> response = new HashMap<>();
        if (indexingStatusManager.isIndexingRunning()) {
            try {
                indexingStatusManager.stoppingByTheUser();
                logger.warn("Остановка пользователем. API/CONTR");
                response.put("result", true);
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                response.put("result", false);
                response.put("error", "Не удалось остановить индексацию: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        }else{
            response.put("result", false);
            response.put("error", "Индексация не запущена.");
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Map<String, Object>> indexPage(@RequestParam String url) {
        Map<String, Object> response = new HashMap<>();

        if (url == null || url.trim().isEmpty()) {
            response.put("result", false);
            response.put("error", "Передаваемая строка сайта пуста.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            if (checkLinkInScopeLinks.checkLink(UrlUtils.removeProtocolAndSubdomain(url))){
                indexingOnePage.indexingPage(url);
                response.put("result", true);
                return ResponseEntity.ok(response);
            } else {
                response.put("result", false);
                response.put("error", "Данная страница находится за пределами сайтов, " +
                        "указанных в конфигурационном файле.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("result", false);
            response.put("error", "Произошла ошибка в процессе индексации отдельной страницы: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "ALL") String site){

        System.out.println("Сайт: " + site);

        Map<String, Object> response = new HashMap<>();

        if (query == null || query.trim().isEmpty()){
            response.put("result", false);
            response.put("error", "Передаваемая строка пуста или не валидна.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {

                sortedLemmas.sortingLemmasByFrequency(query, site);

            response.put("result", true);
            return ResponseEntity.ok(response);
        }catch (Exception e){
            response.put("result", false);
            response.put("error", "Произошла ошибка в процессе сбора информации: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
