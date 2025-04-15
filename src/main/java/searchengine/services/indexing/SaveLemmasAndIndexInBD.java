package searchengine.services.indexing;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.models.Lemma;
import searchengine.models.indexData;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.services.impl.LemmaIndexService;

import java.util.*;

@Service
public class SaveLemmasAndIndexInBD implements LemmaIndexService {
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final JdbcTemplate jdbcTemplate;


    public SaveLemmasAndIndexInBD(LemmaRepository lemmaRepository,
                                  PageRepository pageRepository,
                                  JdbcTemplate jdbcTemplate) {
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void saveLemmasAndIndex(Integer pageId, Integer siteId, Map<String, Integer> lemmas) {

        Set<String> countedLemmas = new HashSet<>(lemmas.keySet());

        List<Lemma> existingLemmas = lemmaRepository.findByLemmaListForUpdate(new ArrayList<>(countedLemmas), siteId);
        Map<String, Lemma> lemmaMap = new HashMap<>();
        existingLemmas.forEach(l -> lemmaMap.put(l.getLemma(), l));

        List<Lemma> newLemmas = new ArrayList<>();
        List<indexData> indexes = new ArrayList<>();

        int totalPages = pageRepository.countBySiteId(siteId);

        lemmas.forEach((lemmaText, rank) -> {
            Lemma lemma = lemmaMap.get(lemmaText);

            if (lemma == null) {
                lemma = new Lemma();
                lemma.setLemma(lemmaText);
                lemma.setSiteId(siteId);
                lemma.setFrequency(1);
                newLemmas.add(lemma);
            } else {
                lemma.setFrequency(Math.min(lemma.getFrequency() + 1, totalPages));
            }

            indexData index = new indexData();
            index.setLemmaId(lemma.getId());
            index.setPageId(pageId);
            index.setRank(rank.floatValue());
            indexes.add(index);
        });

        batchInsertLemmas(newLemmas);

        batchUpdateLemmaFrequencies(existingLemmas);

        batchInsertIndexes(indexes);
    }

    private void batchInsertLemmas(List<Lemma> lemmas) {
        if (lemmas.isEmpty()) return;

        String sql = "INSERT INTO lemma (lemma, site_id, frequency) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE frequency = frequency + 1";

        List<Object[]> batchArgs = lemmas.stream()
                .map(l -> new Object[]{l.getLemma(), l.getSiteId(), l.getFrequency()})
                .toList();

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    private void batchUpdateLemmaFrequencies(List<Lemma> lemmas) {

        if (lemmas.isEmpty()) return;

        String sql = "UPDATE lemma SET frequency = ? WHERE id = ?";

        List<Object[]> batchArgs = lemmas.stream()
                .map(i -> new Object[]{i.getFrequency(), i.getId()})
                .toList();

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    private void batchInsertIndexes(List<indexData> indexes) {

        List<indexData> validIndexes = indexes.stream()
                .filter(i -> i.getLemmaId() != null)
                .toList();

        if (validIndexes.isEmpty()) return;

        String sql = "INSERT INTO index_data (lemma_id, page_id, `rank`) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE `rank` = VALUES(`rank`)";

        List<Object[]> batchArgs = validIndexes.stream()
                .map(i -> new Object[]{i.getLemmaId(), i.getPageId(), i.getRank()})
                .toList();

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }
}

