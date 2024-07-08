package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.entities.SiteEntity;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.StatisticsService;
import searchengine.services.StatusService;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final StatusService statusService;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(statusService.isIndexingRunning());
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        for (Site configurationSite : sites.getSites()) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            String siteUrl = configurationSite.getUrl();
            item.setUrl(siteUrl);
            item.setName(configurationSite.getName());
            Optional<SiteEntity> optionalSiteEntity = siteRepository.findByUrl(siteUrl);
            if (optionalSiteEntity.isEmpty()) {
                continue;
            }
            SiteEntity site = optionalSiteEntity.get();
            item.setStatus(site.getStatus().name());
            item.setStatusTime(site.getStatusTime().toEpochSecond(ZoneOffset.UTC) * 1000);
            String error = site.getLastError();
            item.setError(Objects.requireNonNullElse(error, ""));
            int pageCount = pageRepository.countBySite(site);
            item.setPages(pageCount);
            total.setPages(total.getPages() + pageCount);
            int lemmaCount = lemmaRepository.countBySite(site);
            item.setLemmas(lemmaCount);
            total.setLemmas(total.getLemmas() + lemmaCount);
            detailed.add(item);
        }
        return createResponse(total, detailed);
    }

    private StatisticsResponse createResponse(TotalStatistics total, List<DetailedStatisticsItem> detailed) {
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        StatisticsResponse response = new StatisticsResponse();
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}