package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PageSearcherService {
    private static final double THRESHOLD = 0.8;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;

    @Transactional
    public List<Page> findPagesBySite(SiteEntity site, Set<String> queryLemmaSet) {
        List<LemmaEntity> filteredLemmas = getFilteredLemmas(queryLemmaSet, site);
        if (filteredLemmas.isEmpty()) {
            return Collections.emptyList();
        }
        filteredLemmas.sort(Comparator.comparing(LemmaEntity::getFrequency));
        List<Integer> pageIds = searchPageIds(filteredLemmas);
        if (pageIds.isEmpty()) {
            return Collections.emptyList();
        }
        return getSitePageList(pageIds, filteredLemmas);
    }

    private List<LemmaEntity> getFilteredLemmas(Set<String> queryLemmaSet, SiteEntity site) {
        int pageCount = pageRepository.countBySite(site);
        int siteThreshold = (int) (pageCount * THRESHOLD);
        return lemmaRepository.findBySiteAndLemmaInAndFrequencyLessThanEqual(site, queryLemmaSet, siteThreshold);
    }

    private List<Integer> searchPageIds(List<LemmaEntity> filteredLemmas) {
        int lemmaId = filteredLemmas.get(0).getId();
        List<Integer> cumulativePageIds = new LinkedList<>(indexRepository.findPageIdsByLemmaId(lemmaId));
        for (int i = 1; i < filteredLemmas.size(); ++i) {
            lemmaId = filteredLemmas.get(i).getId();
            List<Integer> pageIds = indexRepository.findPageIdsByLemmaId(lemmaId);
            cumulativePageIds.retainAll(pageIds);
            if (cumulativePageIds.isEmpty()) {
                return Collections.emptyList();
            }
        }
        return cumulativePageIds;
    }

    private List<Page> getSitePageList(List<Integer> pageIds, List<LemmaEntity> filteredLemmas) {
        List<Page> pageList = calculateAbsoluteRelevancePages(pageIds, filteredLemmas);

        double maxRelevance = pageList.stream().mapToDouble(Page::getRelevance).max().orElse(1.0);
        for (Page page : pageList) {
            page.setRelevance(page.getRelevance() / maxRelevance);
        }
        return pageList;
    }

    private List<Page> calculateAbsoluteRelevancePages(List<Integer> pageIds, List<LemmaEntity> filteredLemmas) {
        return indexRepository.getRankSumByPageIdsAndLemmaIds(pageIds,
                filteredLemmas.stream().map(LemmaEntity::getId).toList());
    }
}
