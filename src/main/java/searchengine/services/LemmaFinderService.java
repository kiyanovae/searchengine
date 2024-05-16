package searchengine.services;

import searchengine.config.QueryLemmas;

import java.util.HashMap;
import java.util.List;

public interface LemmaFinderService {
    HashMap<String, Integer> collectLemmas(String html);

    QueryLemmas getLemmaSet(String text);

    List<String> getLemmaList(String word);
}