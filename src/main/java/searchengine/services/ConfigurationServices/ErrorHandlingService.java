package searchengine.services.ConfigurationServices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.models.Site;
import searchengine.models.Status;

@Service
public class ErrorHandlingService {

    private final SiteServicesStatus siteServicesStatus;
    private final Logger logger = LoggerFactory.getLogger(ErrorHandlingService.class);

    public ErrorHandlingService(SiteServicesStatus siteServicesStatus) {
        this.siteServicesStatus = siteServicesStatus;
    }

    public void handleSiteIndexingError(Site site, Exception e) {
        String errorMessage = (e.getMessage() != null) ? e.getMessage() : "Неизвестная ошибка";
        errorMessage = errorMessage.length() > 255 ? errorMessage.substring(0, 255) : errorMessage;

        siteServicesStatus.setSiteStatus(site, Status.FAILED, errorMessage);
        logger.error("Ошибка при индексации сайта {}: {}", site.getUrl(), errorMessage, e);
    }
}
