package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.QueryResponse;
import searchengine.dto.statistics.QueryResponseDataItems;
import searchengine.logicClasses.Lemmatization;
import searchengine.model.IndexTable;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SiteTable;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;

    private String firstLemma;
    private float firstRelevance;
    private String lastQuery;
    private String lastSite;
    private LinkedHashSet<QueryResponseDataItems> listOfLastQuery = new LinkedHashSet<>();


    public QueryResponse search(String query, String site, int offset, int limit) throws IOException {
        LinkedHashSet<QueryResponseDataItems> queryResponseList = new LinkedHashSet<>();
        if (!query.equals(lastQuery) || !site.equals(lastSite)) {
            HashMap<String, Integer> map = Lemmatization.lemmatization(query);
            int totalPageCount = (int) pageRepository.count();
            HashMap<String, Integer> rareLemma = new HashMap<>();
            List<Lemma> lemmaList = new ArrayList<>();
            for (String lemma : map.keySet()) {
                int countPage = 0;
                lemmaList = lemmaRepository.findAllByLemma(lemma);
                for (Lemma l : lemmaList) {
                    countPage = countPage + l.getFrequency();
                }

                if ((double) countPage / totalPageCount <= 0.6) {
                    rareLemma.put(lemma, countPage);
                }
            }
            lemmaList.clear();
            Map<String, Integer> sortedMap = rareLemma.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));


            sortedMap.entrySet().stream().findFirst().ifPresent(entry -> firstLemma = entry.getKey());

            LinkedHashSet<String> linkedLemma = new LinkedHashSet<>(sortedMap.keySet());
            int siteId = 0;

            if (!site.isEmpty()) {
                siteId = siteRepository.findByUrl(site).getId();
            }

            HashSet<Integer> pageList = searchListWithPageId(sortedMap, siteId);

            queryResponseList = creationListOfResponseDataItem(linkedLemma, pageList, map.keySet() );
        } else queryResponseList = listOfLastQuery;

        QueryResponse queryResponse = new QueryResponse();
        queryResponse.setResult(true);
        queryResponse.setCount(queryResponseList.size());
        LinkedHashSet<QueryResponseDataItems> listToData = new LinkedHashSet<>();
        int i = 0;

        for (QueryResponseDataItems dataItems : queryResponseList) {
            if (i >= offset && i < offset + limit) {
                listToData.add(dataItems);
            }
            i++;
        }
        queryResponse.setData(listToData);

        lastQuery = query;
        listOfLastQuery = queryResponseList;
        lastSite = site;

        return queryResponse;

    }




    public HashSet<Integer> searchListWithPageId(Map<String, Integer> sortLemma, int siteId) {

        HashSet<Integer> firstPageList = new HashSet<>();
        if (siteId != 0) {
            for (String lemmas : sortLemma.keySet()) {
                List<IndexTable> indexTableList = new ArrayList<>();
                HashSet<Integer> secondPageList = new HashSet<>();

                if (lemmas.equals(firstLemma)) {
                    Lemma lemma = lemmaRepository.findByLemmaAndSiteId(lemmas, siteId);
                    if (lemma != null) {
                        indexTableList.addAll(indexRepository.findAllByLemmaId(lemma.getId()));
                        for (IndexTable index : indexTableList) {
                            firstPageList.add(pageRepository.findById(index.getPage().getId()).get().getId());
                        }
                    } else {
                        break;
                    }
                } else {
                    Lemma lemma = lemmaRepository.findByLemmaAndSiteId(lemmas, siteId);
                    if (lemma != null) {
                        indexTableList.addAll(indexRepository.findAllByLemmaId(lemma.getId()));
                        for (IndexTable index : indexTableList) {
                            secondPageList.add(pageRepository.findById(index.getPage().getId()).get().getId());
                        }
                        firstPageList.retainAll(secondPageList);
                        if (firstPageList.isEmpty()) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
        } else {
            for (String lemmas : sortLemma.keySet()) {
                List<IndexTable> indexTableList = new ArrayList<>();
                HashSet<Integer> secondPageList = new HashSet<>();

                if (lemmas.equals(firstLemma)) {
                    List<Lemma> lemmaList = lemmaRepository.findAllByLemma(lemmas);
                    if (!lemmaList.isEmpty()) {
                        for (Lemma lemma : lemmaList) {
                            indexTableList.addAll(indexRepository.findAllByLemmaId(lemma.getId()));
                        }

                        for (IndexTable index : indexTableList) {
                            firstPageList.add(pageRepository.findById(index.getPage().getId()).get().getId());
                        }
                    } else {
                        break;
                    }

                } else {
                    List<Lemma> lemmaList = lemmaRepository.findAllByLemma(lemmas);
                    if (!lemmaList.isEmpty()) {

                        for (Lemma lemma : lemmaList) {
                            indexTableList.addAll(indexRepository.findAllByLemmaId(lemma.getId()));
                        }
                        for (IndexTable index : indexTableList) {
                            secondPageList.add(pageRepository.findById(index.getPage().getId()).get().getId());
                        }

                        firstPageList.retainAll(secondPageList);
                        if (firstPageList.isEmpty()) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
        }
        return firstPageList;
    }




    public LinkedHashSet<QueryResponseDataItems> creationListOfResponseDataItem(LinkedHashSet<String> sortLemma, HashSet<Integer> pageList, Set <String> allLemma) throws IOException {

        HashMap<QueryResponseDataItems, Float> queryList = new HashMap<>();
        HashMap<String, String> mapWithTitleAndSnippet = new HashMap<>();
        for (Integer pageId : pageList) {
            Page page = pageRepository.findById(pageId).get();
            SiteTable site = page.getSite();
            float relevance = 0;
            for (String stringLemma : sortLemma) {
                Lemma lemma = lemmaRepository.findByLemmaAndSiteId(stringLemma, site.getId());
                IndexTable index = indexRepository.findByPageIdAndLemmaId(pageId, lemma.getId());
                mapWithTitleAndSnippet = createSnippet(pageId, allLemma);
                relevance = relevance + index.getRank();
            }

            QueryResponseDataItems query = new QueryResponseDataItems();
            query.setSite(site.getUrl());
            query.setSiteName(site.getName());
            query.setUri(page.getPath().substring(1));
            query.setTitle(mapWithTitleAndSnippet.get("title"));
            query.setSnippet(mapWithTitleAndSnippet.get("snippet"));
            query.setRelevance(relevance);
            queryList.put(query, relevance);
        }

        Map<QueryResponseDataItems, Float> sortedMap = queryList.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        sortedMap.entrySet().stream().findFirst().ifPresent(entry -> firstRelevance = entry.getValue());
        LinkedHashSet<QueryResponseDataItems> resultList = new LinkedHashSet<>();

        for (QueryResponseDataItems response : sortedMap.keySet()) {
            response.setRelevance(response.getRelevance() / firstRelevance);
            resultList.add(response);
        }

        return resultList;
    }




    public HashMap<String, String> createSnippet(Integer pageId, Set <String> allLemma) throws IOException {

        HashMap<String, String> map = new HashMap<>();

        String text = pageRepository.findById(pageId).get().getContent();
        LuceneMorphology russianMorphology = new RussianLuceneMorphology();
        String title = text.substring(text.indexOf("<title>") + 7, text.indexOf("</title>"));
        map.put("title", title);

        String plain = Jsoup.parse(text).text();
        String[] str = plain.split(" ");
        int k = 0;
        String snippet = "";
        boolean flagSnippet = false;

        for (String word : str) {
            boolean wordInSnippet = false;

            String newWord = word.replaceAll("[^\\sа-яА-ЯёЁ]", "");
            newWord = newWord.replaceAll("\\s+", " ").trim();

            if (!newWord.isEmpty()) {
                List<String> wordBase = russianMorphology.getNormalForms(newWord.toLowerCase());
                for (String w : wordBase) {
                    for (String lemma : allLemma) {
                        if (w.equals(lemma)) {
                            snippet = snippet + " " + "<b>" + word + "</b>";
                            wordInSnippet = true;
                            flagSnippet = true;
                            break;
                        }
                    }
                }

            }
            if (!wordInSnippet) snippet = snippet + " " + word;
            if (k == 30 && !flagSnippet) {
                snippet = "";
                k = 0;
            } else if (k == 30 && flagSnippet) {
                break;
            }
            ;
            k++;
        }
        map.put("snippet", snippet);
        return map;
    }
}
