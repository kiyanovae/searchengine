package searchengine.services.indexing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import searchengine.config.IndexingSettings;
import searchengine.config.WebSocketConfig.StatisticsWebSocketHandler;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.models.Site;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.impl.StatisticsService;

import java.util.ArrayList;
import java.util.List;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final IndexingSettings indexingSettings;
    private final StatisticsWebSocketHandler statisticsWebSocketHandler;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final Logger logger = LoggerFactory.getLogger(StatisticsServiceImpl.class);

    public StatisticsServiceImpl(IndexingSettings indexingSettings,
                                 StatisticsWebSocketHandler statisticsWebSocketHandler,
                                 PageRepository pageRepository,
                                 LemmaRepository lemmaRepository,
                                 SiteRepository siteRepository,
                                 ThreadPoolTaskScheduler taskScheduler) {
        this.indexingSettings = indexingSettings;
        this.statisticsWebSocketHandler = statisticsWebSocketHandler;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.siteRepository = siteRepository;
        this.taskScheduler = taskScheduler;
    }


    @Override
    public StatisticsResponse getStatistics() {

        TotalStatistics total = new TotalStatistics();
        total.setSites(indexingSettings.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = indexingSettings.getSites();

        for (Site site : sitesList) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());

            int lemmas = lemmaRepository.countLemmasBySiteUrl(site.getUrl());
            int pages = pageRepository.countPagesBySiteUrl(site.getUrl());

            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(siteRepository.findStatusAsStringByUrl(site.getUrl()));
            item.setError(getLastErrorMessages(site.getUrl()));
            item.setStatusTime(siteRepository.findStatusTimeBySiteUrl(site.getUrl()));
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    private String getLastErrorMessages(String url){
        String error = siteRepository.findLastErrorByUrl(url);
        return error != null ? error : "";
    }

    @Scheduled(fixedRate = 5000)
    public void sendStatisticsUpdate() {
        taskScheduler.submit(() -> {
            statisticsWebSocketHandler.sendStatisticsToAll();
            logger.info("Отправка статистики на сайт..");
        });
    }
}
