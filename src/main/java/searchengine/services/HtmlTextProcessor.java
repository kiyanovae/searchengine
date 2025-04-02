package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class HtmlTextProcessor {
    private final LemmaFinder lemmaFinder;

    public HtmlTextProcessor(LemmaFinder lemmaFinder) {
        this.lemmaFinder = lemmaFinder;
    }

    @Autowired
    public Map<String, Integer> extractLemmas(String textHtml) {
        String text = lemmaFinder.cleanHtmlOfTags(textHtml);
        return lemmaFinder.collectLemmas(text);
    }
}
