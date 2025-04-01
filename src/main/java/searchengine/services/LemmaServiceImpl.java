package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LemmaServiceImpl implements LemmaService {

    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepositroy;
    private LemmaFinder lemmaFinder;
    private Map<String, Integer> wordCount;

    @Autowired
    public LemmaServiceImpl(LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.lemmaRepository = lemmaRepository;
        this.indexRepositroy = indexRepository;
    }


    @Override
    public void saveLemma(Site site, Page page) {
        try {
            lemmaFinder = LemmaFinder.getInstance();
            String textFromHtml = lemmaFinder.cleanHtmlOfTags(page.getContent());
            wordCount = lemmaFinder.collectLemmas(textFromHtml); // количество слов на данной странице

            List<Lemma> lemmasFromDb = (List)lemmaRepository.saveAll(lemmaInstance(site));

            List<Index> indexList = new ArrayList<>();

            for (Lemma lemma : lemmasFromDb) {
                Index index = new Index();
                int count = wordCount.get(lemma.getLemma());
                index.setPage(page);
                index.setLemma(lemma);
                index.setRank(count);
                indexList.add(index);
            }
            indexRepositroy.saveAll(indexList);

        } catch (IOException e) {
            log.error("Ошибка в обработке леммы {}", e.getMessage());
        } finally {
            wordCount.clear();

        }

    }

    private List<Lemma> lemmaInstance(Site site) {
        List<Lemma> result = new ArrayList<>();

        for (Map.Entry<String, Integer> word : wordCount.entrySet()) {
            Lemma tempLemma = lemmaRepository.findLemmaBySiteAndLemma(site, word.getKey())
                    .orElseGet(() -> {
                        final Lemma lemma = new Lemma();
                        lemma.setSite(site);
                        lemma.setLemma(word.getKey());
                        lemma.setFrequency(0);
                        return lemma;
                    });
            tempLemma.setFrequency(tempLemma.getFrequency() + 1);
            result.add(tempLemma);
        }
        return result;
    }
}
