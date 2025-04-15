package searchengine.services.indexing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.services.ConfigurationServices.ServicesFlowManagement.IndexingStatusManager;
import searchengine.models.Site;
import searchengine.repositories.PageRepository;
import searchengine.services.ConfigurationServices.SiteServicesStatus;
import searchengine.utils.UrlUtils;

@Service
public class IndexingOnePage {
    private final SiteCrawler siteCrawler;
    private final SiteServicesStatus siteServicesStatus;
    private final PageRepository pageRepository;
    private final IndexingStatusManager indexingStatusManager;

    public IndexingOnePage(SiteCrawler siteCrawler,
                           SiteServicesStatus siteServicesStatus,
                           PageRepository pageRepository,
                           IndexingStatusManager indexingStatusManager) {
        this.siteCrawler = siteCrawler;
        this.siteServicesStatus = siteServicesStatus;
        this.pageRepository = pageRepository;
        this.indexingStatusManager = indexingStatusManager;
    }

    @Transactional
    public void indexingPage(String pageUrl) {

        Site site = siteServicesStatus.initializeSite(UrlUtils.removeProtocolAndSubdomain(pageUrl));

        if (pageRepository.existsByPath(pageUrl)) {
            System.out.println("Удаление: " + pageUrl);     //TODO Настроить лог.
            pageRepository.deletePageByPath(pageUrl);
        }

        siteCrawler.crawlPage(site, pageUrl);
        indexingStatusManager.stopIndexing();
        siteServicesStatus.completeSiteIndexing(site);
        indexingStatusManager.resetStopIndexing();
    }
}
