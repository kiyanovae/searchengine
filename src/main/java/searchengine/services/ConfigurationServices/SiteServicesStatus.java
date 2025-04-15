package searchengine.services.ConfigurationServices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.services.ConfigurationServices.ServicesFlowManagement.IndexingStatusManager;
import searchengine.models.Site;
import searchengine.models.Status;
import searchengine.repositories.SiteRepository;
import searchengine.utils.UrlUtils;

import java.time.LocalDateTime;

@Service
public class SiteServicesStatus {
    private final SiteRepository siteRepository;
    private final IndexingStatusManager indexingStatusManager;
    private final Logger logger = LoggerFactory.getLogger(SiteServicesStatus.class);

    public SiteServicesStatus(
            SiteRepository siteRepository,
            IndexingStatusManager indexingStatusManager){
        this.siteRepository = siteRepository;
        this.indexingStatusManager = indexingStatusManager;
    }

    public void completeSiteIndexing(Site site) {
            if (!indexingStatusManager.shouldIsStopIndexing()) {
                setSiteStatus(site, Status.FAILED, "Индексация сайта остановлена. Сайт: " + site.getUrl());
                logger.warn("Индексация сайта остановлена по \"stopIndexing\": {}", site.getUrl());
            } else {
                setSiteStatus(site, Status.INDEXED, null);
            }
        }


    public void stoppingByTheUser(Site site) {
        logger.warn("Индексация остановлена пользователем.       stoppingByTheUser");
        logger.info("Фиксация статуса сайта {}", site.getName());
        setSiteStatus(site, Status.FAILED, "«Индексация остановлена пользователем.");
    }

    public void setSiteStatus(Site site, Status status, String errorMessage) {
        site.setStatus(status);
        site.setStatusTime(LocalDateTime.now());
        site.setLastError(errorMessage);
        siteRepository.save(site);
    }

    public Site initializeSite(String siteUrl) {

        if (siteRepository.existsByUrl(siteUrl)){
            return siteRepository.findByUrl(siteUrl);
        }else{
            Site site = new Site();
            site.setUrl(siteUrl);
            site.setStatus(Status.INDEXING);
            site.setStatusTime(LocalDateTime.now());
            site.setName(UrlUtils.getDomainFromUrl(siteUrl));
            logger.info("Инициализация сайта {} прошла успешно.", siteUrl);
            return siteRepository.save(site);
        }
    }
}
