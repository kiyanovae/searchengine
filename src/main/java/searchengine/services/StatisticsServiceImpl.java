package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteEntity;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final SitesList sites;
    private final SiteRepository siteRepository;

    @Override
    public StatisticsResponse getStatistics() {

        TotalStatistics total = new TotalStatistics();
        total.setSites(0);
        total.setIndexing(IndexingServiceImpl.getMainIndexingIsRunning());

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for(Site site : sitesList) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            Optional<SiteEntity> optionalSiteEntity = siteRepository.findByUrl(site.getUrl().concat("/"));
            if (optionalSiteEntity.isEmpty()) {
                continue;
            }
            SiteEntity siteEntity = optionalSiteEntity.get();
            total.setSites(total.getSites() + 1);
            item.setStatus(siteEntity.getStatus().name());
            item.setStatusTime(siteEntity.getStatusTime().toEpochSecond(ZoneOffset.UTC));
            String error = siteEntity.getLastError();
            if (error != null) {
                item.setError(error);
            }
            int pages = pageRepository.countBySiteId(siteEntity.getId());
            int lemmas = lemmaRepository.countBySiteId(siteEntity.getId());
            item.setPages(pages);
            item.setLemmas(lemmas);
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }
        return createResponse(total, detailed);
    }

    private StatisticsResponse createResponse(TotalStatistics total, List<DetailedStatisticsItem> detailed) {
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
