package searchengine.logicClasses;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import searchengine.model.IndexTable;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
@Component
@RequiredArgsConstructor
public class FillingLemmaAndIndex {
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;

    public void fillingLemmaIndex(Page page, boolean flag) throws IOException {
        if (flag) {
            HashMap<String, Integer> map = Lemmatization.lemmatization(page.getContent());
            List<Lemma> lemmaList = new ArrayList<>();
            List<IndexTable> indexList = new ArrayList<>();

            for (String lemma : map.keySet()) {
                IndexTable index = new IndexTable();
                index.setPage(page);
                Lemma detectedLemma = lemmaRepository.findByLemmaAndSiteId(lemma, page.getSite().getId());

                if (detectedLemma == null) {
                    Lemma newLemma = new Lemma();
                    newLemma.setSite(page.getSite());
                    newLemma.setLemma(lemma);
                    newLemma.setFrequency(1);
                    lemmaList.add(newLemma);
                    index.setLemma(newLemma);
                } else {
                    detectedLemma.setFrequency(detectedLemma.getFrequency() + 1);
                    lemmaList.add(detectedLemma);
                    index.setLemma(detectedLemma);
                }

                index.setRank(map.get(lemma));
                indexList.add(index);
                if (lemmaList.size() == 100) {
                    lemmaRepository.saveAll(lemmaList);
                    lemmaList.clear();
                }
                if (indexList.size() == 100) {
                    indexRepository.saveAll(indexList);
                    indexList.clear();
                }
            }
            lemmaRepository.saveAll(lemmaList);
            indexRepository.saveAll(indexList);
        }
    }
}
