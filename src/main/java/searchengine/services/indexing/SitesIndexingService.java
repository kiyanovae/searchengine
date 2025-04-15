package searchengine.services.indexing;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.services.ConfigurationServices.ErrorHandlingService;
import searchengine.services.ConfigurationServices.ServicesFlowManagement.IndexingStatusManager;
import searchengine.models.Site;
import searchengine.repositories.PageRepository;
import searchengine.services.ConfigurationServices.SiteServicesStatus;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

@Service
@Getter
public class SitesIndexingService {

    private final IndexingStatusManager indexingStatusManager;
    private final ErrorHandlingService errorHandlingService;
    private final CheckPageLinkInDb registrationAndLinkManagement;
    private final SiteServicesStatus siteServicesStatus;
    private final SiteCrawler siteCrawler;
    private final PageRepository pageRepository;
    private final RecursionStopper recursionStopper;
    private final CheckPageLinkInDb checkPageLinkInDb;

    private final StatisticsServiceImpl statisticsService;

    private static final Logger logger = LoggerFactory.getLogger(SitesIndexingService.class);
    private final Set<String> linksForTheWebSite;
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();

    public SitesIndexingService(
            IndexingStatusManager indexingStatusManager,
            ErrorHandlingService errorHandlingService,
            CheckPageLinkInDb registrationAndLinkManagement,
            SiteServicesStatus siteServicesStatus,
            SiteCrawler siteCrawler,
            PageRepository pageRepository, RecursionStopper recursionStopper,
            CheckPageLinkInDb checkPageLinkInDb, StatisticsServiceImpl statisticsService, Set<String> linksForTheWebSite) {
        this.indexingStatusManager = indexingStatusManager;
        this.errorHandlingService = errorHandlingService;
        this.registrationAndLinkManagement = registrationAndLinkManagement;
        this.siteServicesStatus = siteServicesStatus;
        this.siteCrawler = siteCrawler;
        this.pageRepository = pageRepository;
        this.recursionStopper = recursionStopper;
        this.checkPageLinkInDb = checkPageLinkInDb;
        this.statisticsService = statisticsService;
        this.linksForTheWebSite = linksForTheWebSite;
    }

    //TODO Прописать остановку по требованию. Протестить.


    public void indexSites(List<String> siteUrls) {

        for (String siteUrl : siteUrls) {

            logger.info("Индексация сайта {}", siteUrl);

            if (indexingStatusManager.shouldIsStopIndexing()){
                logger.info("Индексация остановлена 1.");
                return;
            }

            try {

                statisticsService.sendStatisticsUpdate();
                Site site = siteServicesStatus.initializeSite(siteUrl);
                SiteIndexingTask task = new SiteIndexingTask(siteCrawler,
                        site,
                        siteUrl,
                        errorHandlingService,
                        indexingStatusManager,
                        recursionStopper,
                        pageRepository,
                        checkPageLinkInDb,
                        registrationAndLinkManagement);

                forkJoinPool.submit(task).join();
                logger.info("Завершена индексация сайта {}", siteUrl);
                siteServicesStatus.completeSiteIndexing(site);

                indexingStatusManager.resetStopIndexing();
                logger.info("Ресет StopIndexing..");


            } catch (Exception e) {
                logger.warn("Ошибка при инициализации сайта {}: {}.", siteUrl, e.getMessage(), e);
            }
        }
    }
}