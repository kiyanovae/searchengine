package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class LemmaFinderService {
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "ЧАСТ"};
    private final LuceneMorphology luceneMorphology;

    public HashMap<String, Integer> collectLemmas(String html) {
        String plainText = trimHtmlTags(html);
        String[] words = textToArrayContainsRussianWords(plainText);
        HashMap<String, Integer> lemmas = new HashMap<>();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) {
                continue;
            }
            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }
            String normalWord = normalForms.get(0);
            lemmas.put(normalWord, lemmas.getOrDefault(normalWord, 0) + 1);
        }
        return lemmas;
    }

    public QueryLemmas getLemmaSet(String text) {
        String[] words = textToArrayContainsRussianWords(text);
        QueryLemmas queryLemmas = new QueryLemmas();
        Set<String> nonParticipantLemmaSet = queryLemmas.getNonParticipantSet();
        Set<String> filteredLemmaSet = queryLemmas.getFilteredSet();
        for (String word : words) {
            if (!word.isBlank()) {
                List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
                List<String> normalForms = luceneMorphology.getNormalForms(word);
                if (anyWordBaseBelongToParticle(wordBaseForms)) {
                    nonParticipantLemmaSet.addAll(normalForms);
                    continue;
                }
                filteredLemmaSet.addAll(normalForms);
            }
        }
        return queryLemmas;
    }

    public List<String> getLemmaList(String word) {
        List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
        if (anyWordBaseBelongToParticle(wordBaseForms)) {
            return Collections.emptyList();
        }
        return luceneMorphology.getNormalForms(word);
    }

    private String trimHtmlTags(String html) {
        return Jsoup.parse(html).text();
    }

    private String[] textToArrayContainsRussianWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }

    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }

    private boolean hasParticleProperty(String wordBase) {
        for (String property : particlesNames) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }
}