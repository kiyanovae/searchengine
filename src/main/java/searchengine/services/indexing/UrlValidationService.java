package searchengine.services.indexing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.models.Site;
import searchengine.services.ConfigurationServices.ServicesFlowManagement.IndexingStatusManager;
import searchengine.services.ConfigurationServices.SiteServicesStatus;

import java.net.URI;

@Service
public class UrlValidationService {
    private final IndexingStatusManager indexingStatusManager;
    private final SiteServicesStatus siteServicesStatus;
    private final Logger logger = LoggerFactory.getLogger(UrlValidationService.class);

    public UrlValidationService(IndexingStatusManager indexingStatusManager,
                                SiteServicesStatus siteServicesStatus) {
        this.indexingStatusManager = indexingStatusManager;
        this.siteServicesStatus = siteServicesStatus;
    }

    boolean isValidUrlForProcessing(Site site, String url) {

        if (!isSameDomain(site.getUrl(), url)) {
            logger.debug("Ссылка не принадлежит текущему домену: {}", url);
            return false;
        }

        if (indexingStatusManager.isStoppingByTheUser()) {
            siteServicesStatus.stoppingByTheUser(site);
            logger.info("Остановка обхода страницы сайта, по флагу юзера.");
            return false;
        }

        if (indexingStatusManager.shouldIsStopIndexing()) {
            siteServicesStatus.completeSiteIndexing(site);
            return false;
        }
        return true;
    }

    private boolean isSameDomain(String siteUrl, String pageUrl) {
        try {
            URI siteUri = encodeUri(siteUrl);
            URI pageUri = encodeUri(pageUrl);

            String siteDomain = siteUri.getHost();
            String pageDomain = pageUri.getHost();

            if (siteDomain == null || pageDomain == null) {
                logger.warn("Ошибка: Один из URL не содержит домен. siteUrl={}, pageUrl={}", siteUrl, pageUrl);
                return false;
            }

            siteDomain = siteDomain.replaceFirst("^www\\.", "");
            pageDomain = pageDomain.replaceFirst("^www\\.", "");

            return pageDomain.equals(siteDomain);

        } catch (Exception e) {
            logger.error("Ошибка разбора URL: {} или {}", siteUrl, pageUrl, e);
            return false;
        }
    }
    private URI encodeUri(String url) { // Рабочий вариант, но проблема с лишними кавычками.
        try {
            String encodedUrl = url.replace(" ", "%20");
            return new URI(encodedUrl);
        } catch (Exception e) {
            logger.error("Ошибка разбора URL: {}", url, e);
            return URI.create(url);
        }
    }
}
