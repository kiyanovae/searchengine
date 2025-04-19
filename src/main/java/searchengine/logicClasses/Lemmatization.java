package searchengine.logicClasses;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class Lemmatization {
    public static HashMap<String, Integer> lemmatization(String text) throws IOException {

        LuceneMorphology russianMorphology = new RussianLuceneMorphology();
        HashMap<String, Integer> map = new HashMap<>();

        String newText = text.replaceAll("[^\\sа-яА-Я]", "");
        newText = newText.replaceAll("\\s+", " ").trim();

        String[] str = newText.split(" ");
        for (String s : str) {
            if (s.isEmpty()) {
                continue;
            }

            List<String> wordBaseForms = russianMorphology.getNormalForms(s.toLowerCase());

            for (String word : wordBaseForms) {
                String wordInfo = String.valueOf(russianMorphology.getMorphInfo(word));
                if (wordInfo.contains("СОЮЗ") || wordInfo.contains("МЕЖД") ||
                        wordInfo.contains("МС") || wordInfo.contains("ПРЕДЛ") || wordInfo.contains("ЧАСТ")) {
                    continue;
                }
                if (map.containsKey(word)) {
                    map.put(word, map.get(word) + 1);
                } else {
                    map.put(word, 1);
                }
            }
        }
        return map;
    }
}
