package searchengine.services.indexing;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.IndexingSettings;
import searchengine.models.Site;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

@Service
public class LemmaAnalyzerService {

    private static final Pattern WORD_PATTERN = Pattern.compile("[\\p{IsCyrillic}]+(?:[-'][\\p{IsCyrillic}]+)*");
    private final LuceneMorphology luceneMorph;
    private final List<Site> sites;
    private static final Logger logger = LoggerFactory.getLogger(LemmaAnalyzerService.class);

    @Autowired
    public LemmaAnalyzerService(IndexingSettings indexingSettings) {
        try {
            this.luceneMorph = new RussianLuceneMorphology();
            this.sites = indexingSettings.getSites();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка инициализации LuceneMorphology: проверьте библиотеку и ресурсы", e);
        }
    }

    public Map<String, Integer> getLemmas(String text) {

        if (text == null || text.trim().isEmpty()) {
            logger.info("Передаваемый текст для обработки лемм - пустой." +
                    "Отмена обработки настоящей страницы.");
            return Collections.emptyMap();
        }

        Map<String, Integer> lemmaCount = new HashMap<>();
        String[] words = WORD_PATTERN.matcher(removeHTMLTags(text).toLowerCase()).results()
                .map(MatchResult::group)
                .toArray(String[]::new);

        for (String word : words) {
            List<String> wordBaseForms = luceneMorph.getMorphInfo(word);

            for (String morphInfo : wordBaseForms) {
                String[] parts = morphInfo.split("\\|");
                String lemma = parts[0];

                if (isTargetPartOfSpeech(parts) && !isStopWord(parts)) {
                    lemmaCount.put(lemma, lemmaCount.getOrDefault(lemma, 0) + 1);
                }
            }
        }
        return lemmaCount;
    }


    private boolean isStopWord(String[] parts) {
        return (parts[1].contains("СОЮЗ") || parts[1].contains("ПРЕДЛ")
                || parts[1].contains("МЕЖД") || parts[1].contains("ЧАСТ"));
    }

    private boolean isTargetPartOfSpeech(String[] parts) {
        return parts.length > 1 && (parts[1].contains("С") || parts[1].contains("П") || parts[1].contains("Г"));
    }

    private String removeHTMLTags(String HTMLContent){
        return HTMLContent.replaceAll("<.*?>", "").trim();
    }
}