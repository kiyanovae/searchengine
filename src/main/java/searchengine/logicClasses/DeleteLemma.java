package searchengine.logicClasses;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import searchengine.model.IndexTable;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;
@Component
@RequiredArgsConstructor
public class DeleteLemma {
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;

    public void deleteLemmaAndIndex(Page page) {
        List<IndexTable> list = indexRepository.findAllByPageId(page.getId());
        List<Lemma> lemmaDeleteList = new ArrayList<>();
        List<Lemma> lemmaSaveList = new ArrayList<>();
        if (!list.isEmpty()) {
            for (IndexTable index : list) {
                Lemma lemma = lemmaRepository.findById(index.getLemma().getId());
                int frequency = lemma.getFrequency() - 1;

                if (frequency == 0) {
                    lemmaDeleteList.add(lemma);
                } else {
                    lemma.setFrequency(frequency);
                    lemmaSaveList.add(lemma);
                }
                if (lemmaDeleteList.size() == 200) {
                    lemmaRepository.deleteAll(lemmaDeleteList);
                    lemmaDeleteList.clear();
                }
                if (lemmaSaveList.size() == 200) {
                    lemmaRepository.saveAll(lemmaSaveList);
                    lemmaSaveList.clear();
                }
            }
            lemmaRepository.deleteAll(lemmaDeleteList);
            lemmaRepository.saveAll(lemmaSaveList);
        }
    }
}
