package searchengine.services.indexing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.services.ConfigurationServices.ErrorHandlingService;
import searchengine.services.ConfigurationServices.ServicesFlowManagement.IndexingStatusManager;
import searchengine.models.Site;
import searchengine.repositories.PageRepository;

import java.util.List;
import java.util.Set;
import java.util.concurrent.RecursiveTask;

public class SiteIndexingTask extends RecursiveTask<Void> {

    private final SiteCrawler siteCrawler;
    private final String url;
    private final ErrorHandlingService errorHandlingService;
    private final IndexingStatusManager indexingStatusManager;
    private final RecursionStopper recursionStopper;
    private final PageRepository pageRepository;
    private final Site site;
    private final CheckPageLinkInDb checkPageLinkInDb;
    private final CheckPageLinkInDb registrationAndLinkManagement;


    private final Logger logger = LoggerFactory.getLogger(SitesIndexingService.class);

    public SiteIndexingTask(SiteCrawler siteCrawler,
                            Site site,
                            String url,
                            ErrorHandlingService errorHandlingService,
                            IndexingStatusManager indexingStatusManager,
                            RecursionStopper recursionStopper,
                            PageRepository pageRepository,
                            CheckPageLinkInDb checkPageLinkInDb,
                            CheckPageLinkInDb registrationAndLinkManagement) {
        this.siteCrawler = siteCrawler;
        this.site = site;
        this.url = url;
        this.errorHandlingService = errorHandlingService;
        this.indexingStatusManager = indexingStatusManager;
        this.recursionStopper = recursionStopper;
        this.pageRepository = pageRepository;
        this.checkPageLinkInDb = checkPageLinkInDb;
        this.registrationAndLinkManagement = registrationAndLinkManagement;
    }

    @Override
    protected Void compute(){

        if (recursionStopper.shouldStopRecursion()){
            indexingStatusManager.stopIndexing();
            logger.info("Остановка рекурсии по времени обновления.");
            return null;
        }

        try {

            Set<String> newLinks = siteCrawler.crawlPage(site, url);
            List<SiteIndexingTask> tasks = newLinks.stream()
                    .filter(checkPageLinkInDb::checkPage)
                    .map(link -> new SiteIndexingTask(siteCrawler,
                            site,
                            link,
                            errorHandlingService,
                            indexingStatusManager,
                            recursionStopper,
                            pageRepository,
                            checkPageLinkInDb,
                            registrationAndLinkManagement))
                    .toList();

            for (SiteIndexingTask task : tasks){
                if (indexingStatusManager.shouldIsStopIndexing()){
                    return null;
                }

                Thread.yield();
                task.fork();
            }

            if (!indexingStatusManager.shouldIsStopIndexing()) {
                tasks.forEach(SiteIndexingTask::join);
            }

        }catch (Exception e){
            errorHandlingService.handleSiteIndexingError(site, e);
            logger.warn("Ошибка при индексаци страницы: {}", url, e);
        }
        return null;
    }
}
