package searchengine.services;

import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

public interface LemmaService {
    LemmaEntity saveOrUpdateLemma(String lemma, SiteEntity site);
}
