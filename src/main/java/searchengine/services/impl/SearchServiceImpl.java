package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.exceptions.BadRequestException;
import searchengine.exceptions.ConflictRequestException;
import searchengine.model.Page;
import searchengine.model.QueryLemmas;
import searchengine.model.Snippet;
import searchengine.model.Word;
import searchengine.model.entities.PageEntity;
import searchengine.model.entities.SiteEntity;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.LemmaFinderService;
import searchengine.services.PageSearcherService;
import searchengine.services.SearchService;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {
    private static final int MAX_SNIPPET_LENGTH = 200;
    private static final int MAX_PREFIX_WORD_COUNT = 5;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaFinderService lemmaFinderService;
    private final PageSearcherService pageSearcherService;

    @Override
    public SearchResponse search(String query, String site, int offset, int limit) {
        if (query == null || query.isEmpty()) {
            throw new BadRequestException("Search query must not be empty");
        }
        if (offset < 0) {
            throw new BadRequestException("The 'OFFSET' parameter must not be negative");
        }
        if (limit < 0) {
            throw new BadRequestException("The 'LIMIT' parameter must not be negative");
        }
        if (site != null) {
            return searchByOneSite(query, site, offset, limit);
        } else {
            return searchByAllSites(query, offset, limit);
        }
    }

    private SearchResponse searchByOneSite(String query, String siteUrl, int offset, int limit) {
        log.info("The «{}» search query has been started", query);
        Optional<SiteEntity> optionalSite = getIndexedSite(siteUrl);
        SiteEntity site = optionalSite.orElseThrow(() ->
                new ConflictRequestException("The site must have an indexed status"));
        QueryLemmas queryLemmas = lemmaFinderService.getLemmaSet(query);
        List<Page> foundPages = pageSearcherService.findPagesBySite(site, queryLemmas.getFilteredSet());
        if (foundPages.isEmpty()) {
            return new SearchResponse(true, 0, Collections.emptyList());
        }
        Map<Integer, SiteEntity> foundSites = new HashMap<>();
        foundSites.put(site.getId(), site);
        List<SearchData> dataList = createSearchDataList(foundSites, foundPages, offset, limit, queryLemmas);
        log.info("The «{}» search query has been executed", query);
        return new SearchResponse(true, foundPages.size(), dataList);
    }

    private SearchResponse searchByAllSites(String query, int offset, int limit) {
        log.info("The «{}» search query has been started", query);
        List<SiteEntity> siteEntityList = getIndexedSiteList();
        if (siteEntityList.isEmpty()) {
            throw new ConflictRequestException("No indexed sites found");
        }
        QueryLemmas queryLemmas = lemmaFinderService.getLemmaSet(query);
        Map<Integer, SiteEntity> foundSites = new HashMap<>();
        List<Page> foundPages = new ArrayList<>();
        for (SiteEntity site : siteEntityList) {
            List<Page> foundPagesBySite = pageSearcherService.findPagesBySite(site, queryLemmas.getFilteredSet());
            if (foundPagesBySite.isEmpty()) {
                continue;
            }
            foundPages.addAll(foundPagesBySite);
            foundSites.put(site.getId(), site);
        }
        if (foundPages.isEmpty()) {
            return new SearchResponse(true, 0, Collections.emptyList());
        }
        List<SearchData> dataList = createSearchDataList(foundSites, foundPages, offset, limit, queryLemmas);
        log.info("The «{}» query has been executed", query);
        return new SearchResponse(true, foundPages.size(), dataList);
    }

    private Optional<SiteEntity> getIndexedSite(String url) {
        return siteRepository.findByUrlAndStatus(url, SiteEntity.SiteStatus.INDEXED);
    }

    private List<SearchData> createSearchDataList(Map<Integer, SiteEntity> foundSites, List<Page> foundPages,
                                                  int offset, int limit, QueryLemmas queryLemmas) {
        int size = foundPages.size();
        if (offset >= size) {
            return Collections.emptyList();
        }
        int toIndex = offset + limit;
        if (toIndex < offset || toIndex > size) {
            toIndex = size;
        }
        foundPages.sort(Comparator.comparing(Page::getRelevance).reversed());
        List<Page> pageSubList = new ArrayList<>(foundPages.subList(offset, toIndex));
        pageSubList.sort(Comparator.comparing(Page::getId));
        List<Integer> pageIds = pageSubList.stream().map(Page::getId).toList();
        List<PageEntity> pageEntityList = pageRepository.findAllById(pageIds);
        List<SearchData> dataList = new ArrayList<>();
        int i = 0;
        for (PageEntity page : pageEntityList) {
            SiteEntity site = foundSites.get(page.getSite().getId());
            var data = new SearchData();
            data.setSite(site.getUrl());
            data.setSiteName(site.getName());
            data.setUri(page.getPath());
            data.setRelevance(pageSubList.get(i++).getRelevance());
            Document parseContent = Jsoup.parse(page.getContent());
            data.setTitle(parseContent.title());
            data.setSnippet(createSnippet(parseContent.text(), queryLemmas));
            dataList.add(data);
        }
        dataList.sort(Comparator.comparing(SearchData::getRelevance).reversed());
        return dataList;
    }

    private List<SiteEntity> getIndexedSiteList() {
        return siteRepository.findByStatus(SiteEntity.SiteStatus.INDEXED);
    }

    private String createSnippet(String content, QueryLemmas queryLemmas) {
        Set<String> contentWordSet = findWords(content);
        Map<String, String> matchesWords = new HashMap<>();
        Set<String> filteredQueryLemmaSet = queryLemmas.getFilteredSet();
        contentWordSet.forEach((word) -> {
            List<String> lemmaList = lemmaFinderService.getLemmaList(word);
            for (String lemma : lemmaList) {
                if (filteredQueryLemmaSet.contains(lemma)) {
                    matchesWords.put(word, lemma);
                    break;
                }
            }
        });
        Pattern pattern = Pattern.compile("\\p{L}+");
        if (content.length() < MAX_SNIPPET_LENGTH) {
            return getNormalFormSnippet(content, matchesWords, pattern, queryLemmas.getNonParticipantSet());
        }
        List<Word> wordList = contentToWordList(content, matchesWords, pattern);
        String snippet = findMaxRelevantSnippet(content, wordList);
        return getNormalFormSnippet(snippet, matchesWords, pattern, queryLemmas.getNonParticipantSet());
    }

    private List<Word> contentToWordList(String content, Map<String, String> matchesWords, Pattern pattern) {
        List<Word> wordList = new ArrayList<>();
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String strWord = content.substring(start, end);
            Word word = new Word();
            word.setValue(strWord);
            word.setIndex(start);
            String lowerCaseStrWord = strWord.toLowerCase(Locale.ROOT);
            boolean isRussianWord = lowerCaseStrWord.matches("[а-я]+");
            if (isRussianWord && matchesWords.containsKey(lowerCaseStrWord)) {
                word.setMatches(true);
                word.setLemma(matchesWords.get(lowerCaseStrWord));
            } else {
                word.setMatches(false);
            }
            wordList.add(word);
        }
        return wordList;
    }

    private String findMaxRelevantSnippet(String content, List<Word> wordList) {
        int maxCount = Integer.MIN_VALUE;
        int maxDiffLemmaCount = Integer.MIN_VALUE;
        String snippetStr = "";
        for (int wordIndex = 0; wordIndex < wordList.size(); wordIndex++) {
            Word word = wordList.get(wordIndex);
            String wordValue = word.getValue();
            if (!word.isMatches() || wordValue.length() > MAX_SNIPPET_LENGTH) {
                continue;
            }
            Snippet candidateSnippet = collectSnippet(content, wordList, word, wordValue, wordIndex);
            int diffLemmaCount = candidateSnippet.getLemmas().size();
            int totalLemmaCount = candidateSnippet.getTotalLemmaCount();
            if (diffLemmaCount > maxDiffLemmaCount) {
                snippetStr = candidateSnippet.getValue().toString();
                maxDiffLemmaCount = diffLemmaCount;
                maxCount = totalLemmaCount;
            } else if (diffLemmaCount == maxDiffLemmaCount && totalLemmaCount > maxCount) {
                snippetStr = candidateSnippet.getValue().toString();
                maxCount = totalLemmaCount;
            }
        }
        return "..." + snippetStr + "...";
    }

    private Snippet collectSnippet(String content, List<Word> wordList, Word word, String wordValue, int wordIndex) {
        Snippet snippet = createSnippetPrefix(content, wordList, word, wordValue, wordIndex);
        collectSnippetBasePart(content, snippet, wordList, wordIndex);
        return snippet;
    }

    private Snippet createSnippetPrefix(String content, List<Word> wordList, Word word, String wordValue, int wordIndex) {
        int beginSnippetIndex = word.getIndex();
        int endSnippetIndex = beginSnippetIndex + wordValue.length();
        int totalLemmaCount = 1;
        Set<String> lemmas = new HashSet<>();
        lemmas.add(word.getLemma());
        int prefixWordCount = 1;
        int index = wordIndex - 1;
        while (prefixWordCount <= MAX_PREFIX_WORD_COUNT && index >= 0) {
            Word currentWord = wordList.get(index);
            int currentWordIndex = currentWord.getIndex();
            int expectedLength = endSnippetIndex - currentWordIndex;
            if (expectedLength > MAX_SNIPPET_LENGTH) {
                break;
            }
            beginSnippetIndex = currentWordIndex;
            if (currentWord.isMatches()) {
                totalLemmaCount++;
                lemmas.add(currentWord.getLemma());
            }
            prefixWordCount++;
            index--;
        }
        StringBuilder snippetValue = new StringBuilder(content.substring(beginSnippetIndex, endSnippetIndex));
        return new Snippet(snippetValue, beginSnippetIndex, endSnippetIndex, lemmas, totalLemmaCount);
    }

    private void collectSnippetBasePart(String content, Snippet snippet, List<Word> wordList, int wordIndex) {
        Set<String> lemmas = snippet.getLemmas();
        int totalLemmaCount = snippet.getTotalLemmaCount();
        int snippetLength = snippet.getValue().length();
        int endSnippetIndex = snippet.getEndIndex();
        int newEndSnippetIndex = endSnippetIndex;
        for (int j = wordIndex + 1; j < wordList.size(); j++) {
            Word currentWord = wordList.get(j);
            String currentWordValue = currentWord.getValue();
            int expectedEndSnippetIndex = currentWord.getIndex() + currentWordValue.length();
            int expectedSnippetLength = snippetLength + expectedEndSnippetIndex - endSnippetIndex;
            if (expectedSnippetLength > MAX_SNIPPET_LENGTH) {
                break;
            }
            if (currentWord.isMatches()) {
                totalLemmaCount++;
                lemmas.add(currentWord.getLemma());
            }
            newEndSnippetIndex = expectedEndSnippetIndex;
        }
        snippet.getValue().append(content, endSnippetIndex, newEndSnippetIndex);
        snippet.setEndIndex(newEndSnippetIndex);
        snippet.setTotalLemmaCount(totalLemmaCount);
    }

    private String getNormalFormSnippet(String text, Map<String, String> matchesWords, Pattern pattern, Set<String> nonParticipantLemmaSet) {
        StringBuilder snippet = new StringBuilder();
        Matcher matcher = pattern.matcher(text);
        int index = 0;
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String word = text.substring(start, end);
            String lowerCaseWord = word.toLowerCase(Locale.ROOT);
            if (matchesWords.containsKey(lowerCaseWord) || nonParticipantLemmaSet.contains(lowerCaseWord)) {
                snippet.append(text, index, start);
                snippet.append("<b>").append(word).append("</b>");
            } else {
                snippet.append(text, index, end);
            }
            index = end;
        }
        snippet.append(text, index, text.length());
        return snippet.toString();
    }

    private Set<String> findWords(String text) {
        text = text.toLowerCase(Locale.ROOT);
        Set<String> wordSet = new HashSet<>();
        Pattern pattern = Pattern.compile("[а-я]+");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            wordSet.add(matcher.group());
        }
        return wordSet;
    }
}