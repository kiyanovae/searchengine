package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.exceptions.RequestException;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {
    private static final double THRESHOLD = 0.8;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;

    @Override
    public SearchResponse search(String query, String site, int offset, int limit) throws IOException {
        if (query == null) {
            throw new RequestException("Задан пустой поисковый запрос");
        }
        LemmaFinder lemmaFinder = LemmaFinder.getInstance();
        Set<String> lemmaSet = lemmaFinder.getLemmaSet(query);
        List<Integer> siteIdList = getSiteIdList(site);
        List<SearchData> searchDataList = new ArrayList<>();

        for (int siteId : siteIdList) {
            List<LemmaIdFrequency> lemmaIdFrequencyList = getLemmaIdFrequencyList(lemmaSet, siteId);
            if (lemmaIdFrequencyList.isEmpty()) {
                log.info(siteId + " пусто");
                continue;
            }
            log.info("Before sort:\n" + lemmaIdFrequencyList);
            lemmaIdFrequencyList.sort(Comparator.comparing(LemmaIdFrequency::frequency));
            log.info("After sort:\n" + lemmaIdFrequencyList);

            List<IndexEntity> indexList = getIndexList(lemmaIdFrequencyList);
            log.info("Index list:\n" + indexList);

            if (indexList.isEmpty()) {
                searchDataList.addAll(new ArrayList<>());
            }

            Map<Integer, Double> pageIdRelevanceMap = indexList.stream().collect(Collectors.groupingBy(IndexEntity::getPageId, Collectors.summingDouble(IndexEntity::getRank)));
            log.info("siteIdRelevance:\n" + pageIdRelevanceMap);

            double maxAbsRelevance = pageIdRelevanceMap.values().stream().max(Double::compare).orElse(0.0);
            log.info("max:\n" + maxAbsRelevance);

            pageIdRelevanceMap.replaceAll((k, v) -> v / maxAbsRelevance);
            log.info("siteIdRelevance:\n" + pageIdRelevanceMap);

            List<SearchData> dataList = createData(pageIdRelevanceMap, siteId);
            searchDataList.addAll(dataList);
        }
        //TODO: сделать
        return null;
    }

    private List<SearchData> createData(Map<Integer, Double> pageIdRelevanceMap, int siteId) {
        List<SearchData> dataList = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : pageIdRelevanceMap.entrySet()) {
            SearchData data = new SearchData();
            setSiteAndSiteName(data, siteId);
            setUriTitleAndSnippet(data, entry.getKey());
            data.setRelevance(entry.getValue());
            dataList.add(data);
        }
        return dataList;
    }

    private void setUriTitleAndSnippet(SearchData data, int pageId) {
        Optional<PageEntity> optionalPage = pageRepository.findById(pageId);
        if (optionalPage.isPresent()) {
            PageEntity pageEntity = optionalPage.get();
            data.setUri(pageEntity.getPath());
            String content = pageEntity.getContent();
            data.setTitle(Jsoup.parse(content).title());
            setSnippet(data, content);
        }
    }

    private void setSnippet(SearchData data, String content) {
        String text = Jsoup.parse(content).text();
        String[] textArray = textToArrayContainsWords(text);

    }

    private String[] textToArrayContainsWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .split("\\s+");
    }

    private void setSiteAndSiteName(SearchData data, int siteId) {
        Optional<SiteEntity> optionalSite = siteRepository.findById(siteId);
        if (optionalSite.isPresent()) {
            SiteEntity siteEntity = optionalSite.get();
            String site = siteEntity.getUrl();
            site = site.substring(0, site.length() - 1);
            data.setSite(site);
            data.setSiteName(siteEntity.getName());
        }
    }

    private List<LemmaIdFrequency> getLemmaIdFrequencyList(Set<String> lemmaSet, int siteId) {
        int pageCount = pageRepository.countBySiteId(siteId);
        double siteThreshold = pageCount * THRESHOLD;
        List<LemmaIdFrequency> lemmaIdFrequencyList = new ArrayList<>();
        for (String lemma : lemmaSet) {
            Optional<LemmaEntity> optional = lemmaRepository.findByLemmaAndSiteId(lemma, siteId);
            if (optional.isPresent()) {
                LemmaEntity lemmaEntity = optional.get();
                int lemmaId = lemmaEntity.getId();
                int frequency = lemmaEntity.getFrequency();
                if (frequency <= siteThreshold) {
                    lemmaIdFrequencyList.add(new LemmaIdFrequency(lemmaId, frequency));
                }
            }
        }
        return lemmaIdFrequencyList;
    }

    private List<Integer> getSiteIdList(String site) {
        if (site == null) {
            List<SiteEntity> siteEntities = siteRepository.findAll();
            siteEntities = siteEntities.stream().filter(item -> item.getStatus().equals(SiteStatus.INDEXED)).collect(Collectors.toList());
            if (siteEntities.isEmpty()) {
                throw new RequestException("Не найдено проиндексированных сайтов");
            }
            return siteEntities.stream().map(SiteEntity::getId).toList();
        }
        Optional<SiteEntity> optionalSite = siteRepository.findByUrl(site.concat("/"));
        if (optionalSite.isEmpty()) {
            throw new RequestException("Задан непроиндексированный сайт");
        }
        SiteEntity siteEntity = optionalSite.get();
        if (!siteEntity.getStatus().equals(SiteStatus.INDEXED)) {
            throw new RequestException("Задан непроиндексированный сайт");
        }
        List<Integer> result = new ArrayList<>();
        result.add(siteEntity.getId());
        return result;

    }

    private List<IndexEntity> getIndexList(List<LemmaIdFrequency> lemmaIdFrequencyList) {
        List<IndexEntity> indexList = indexRepository.findByLemmaId(lemmaIdFrequencyList.get(0).lemmaId());
        indexList = getNewIndexList(indexList);
        for (int i = 1; i < lemmaIdFrequencyList.size(); ++i) {
            if (indexList.isEmpty()) {
                break;
            }
            indexList = filterIndexList(indexList, lemmaIdFrequencyList.get(i).lemmaId());
            indexList = getNewIndexList(indexList);
            log.info("After cull:\n" + indexList);
        }
        return indexList;
    }

    private List<IndexEntity> filterIndexList(List<IndexEntity> indexList, int lemmaId) {
        return indexList.stream()
                .filter(entity -> entity.getLemmaId() == lemmaId)
                .collect(Collectors.toList());
    }

    private List<IndexEntity> getNewIndexList(List<IndexEntity> indexList) {
        List<Integer> pageIdList = getPageIdList(indexList);
        log.info("PageId list :\n" + pageIdList);
        if (pageIdList.isEmpty()) {
            return new ArrayList<>();
        }
        return indexRepository.findAllByPageId(pageIdList);
    }

    private List<Integer> getPageIdList(List<IndexEntity> indexList) {
        return indexList.stream().map(IndexEntity::getPageId).collect(Collectors.toList());
    }

    private record LemmaIdFrequency(int lemmaId, int frequency) {
    }
}
