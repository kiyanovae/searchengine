package searchengine.services.indexing;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.config.IndexingSettings;

import java.io.IOException;

@Service
public class HtmlContentPageChecker
{
    private final IndexingSettings indexingSettings;
    private static final Logger logger = LoggerFactory.getLogger(HtmlContentPageChecker.class);

    public HtmlContentPageChecker(IndexingSettings indexingSettings)
    {
        this.indexingSettings = indexingSettings;
    }

    public boolean isPageValid(String url) {

        if (indexingSettings == null) {
            throw new IllegalArgumentException("indexingSettings не был инициализирован.");
        }

        try {
            Connection.Response response = Jsoup.connect(url)
                    .method(Connection.Method.GET)
                    .userAgent(indexingSettings.getUserAgent())
                    .referrer(indexingSettings.getReferrer())
                    .execute();

            return isValidContentType(response.contentType());

        } catch (IOException e) {
            logger.debug("Ошибка обработки страницы: неподдерживаемый тип контента {}", url);
            return false;
        }
    }

    private boolean isValidContentType(String contentType) {

        if (contentType == null){
            logger.warn("На данной странице контент отсутствует, отмена индексации.");
            return false;
        }

        contentType = contentType.replaceAll("[^\\x20-\\x7E]", "").trim().toLowerCase();

        if (!contentType.startsWith("text/") || contentType.contains("xml")) {
            logger.info("Ссылка отклонена, так как она не прошла провреку на валидность контента.");
            return false;
        }
        return true;
    }
}
