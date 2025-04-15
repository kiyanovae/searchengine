package searchengine.services.ConfigurationServices;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.config.IndexingSettings;
import searchengine.services.impl.JsoupConnectionService;

import java.io.IOException;

@Service
public class JsoupConnectionServiceImpl implements JsoupConnectionService {

    private final IndexingSettings indexingSettings;

    public JsoupConnectionServiceImpl(IndexingSettings indexingSettings) {
        this.indexingSettings = indexingSettings;
    }

    @Override
    public Document getDocument(String url) throws IOException {

        Connection connection = Jsoup.connect(url)
                .timeout(15_000)
                .userAgent(indexingSettings.getUserAgent())
                .followRedirects(true);
        return connection.get();
    }
}
