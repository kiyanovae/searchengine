package searchengine.services.indexing;

import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.models.Site;
import searchengine.models.Status;
import searchengine.services.ConfigurationServices.SiteServicesStatus;
import searchengine.services.impl.JsoupConnectionService;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Set;
@Service
public class SiteCrawler {

    private final JsoupConnectionService jsoupConnectionService;
    private final GetInformPageAndSaveInDB getInformPageAndSaveInDB;
    private final ExtractAndFilterLinks extractAndFilterLink;
    private final HtmlContentPageChecker htmlContentPageChecker;
    private final UrlValidationService urlValidationService;
    private final SiteServicesStatus siteServicesStatus;
    private final Logger logger = LoggerFactory.getLogger(SiteCrawler.class);
    private static final int MAX_ERROR_MESSAGE_LENGTH = 255;

    public SiteCrawler(JsoupConnectionService jsoupConnectionService,
                       GetInformPageAndSaveInDB getInformPageAndSaveInDB,
                       ExtractAndFilterLinks extractAndFilterLink,
                       HtmlContentPageChecker htmlContentPageChecker,
                       UrlValidationService urlValidationService,
                       SiteServicesStatus siteServicesStatus) {
        this.jsoupConnectionService = jsoupConnectionService;
        this.getInformPageAndSaveInDB = getInformPageAndSaveInDB;
        this.extractAndFilterLink = extractAndFilterLink;
        this.htmlContentPageChecker = htmlContentPageChecker;
        this.urlValidationService = urlValidationService;
        this.siteServicesStatus = siteServicesStatus;
    }

    public Set<String> crawlPage(Site site, String pageUrl) {

        if (!isPageValidForProcessing(site, pageUrl)){
            return Collections.emptySet();
        }

        try {
            Document document = jsoupConnectionService.getDocument(pageUrl);
            Set<String> newLinks = extractAndFilterLink.extractAndFilterLink(document);
            getInformPageAndSaveInDB.getInformDataPageAndSave(site, pageUrl, document);
            return newLinks;

        } catch (SocketTimeoutException t){
            logger.warn("Таймаут при загрузке страницы: {} {}", pageUrl, t.getMessage());
        } catch (IOException e) {
            handlePageIndexingError(site, pageUrl, e);
        }
        return Collections.emptySet();
    }

    private boolean isPageValidForProcessing(Site site, String pageUrl){
        return urlValidationService.isValidUrlForProcessing(site, pageUrl)
                && htmlContentPageChecker.isPageValid(pageUrl);
    }

    private void handlePageIndexingError(Site site, String pageUrl, IOException e){
        String errorMessage = e.getMessage().length() > MAX_ERROR_MESSAGE_LENGTH
                ? e.getMessage().substring(0, MAX_ERROR_MESSAGE_LENGTH)
                : e.getMessage();
        String lastError = "Ошибка индексации: " + errorMessage;

        siteServicesStatus.setSiteStatus(site, Status.FAILED, lastError);
        logger.error("Ошибка индексации страницы: {}", pageUrl, e);
    }
}

