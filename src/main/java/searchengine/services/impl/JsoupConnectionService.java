package searchengine.services.impl;

import org.jsoup.nodes.Document;

import java.io.IOException;

public interface JsoupConnectionService {
    Document getDocument(String url) throws IOException;
}
