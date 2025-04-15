package searchengine.services.impl;

import java.util.Map;

public interface LemmaService {
    void saveLemmasAndIndex(int pageId, int siteId, Map<String, Integer> lemmas);
}

