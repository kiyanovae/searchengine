package searchengine.logicClasses;

import lombok.RequiredArgsConstructor;
import searchengine.model.IndexTable;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;

@RequiredArgsConstructor
public class DeleteLemma extends RecursiveAction {
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final Page page;

    @Override
    protected void compute() {
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
