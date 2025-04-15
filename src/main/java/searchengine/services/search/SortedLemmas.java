package searchengine.services.search;

import org.springframework.stereotype.Service;
import searchengine.services.indexing.LemmaAnalyzerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Service
public class SortedLemmas {    //TODO переименовать имя класса.
    private final LemmaAnalyzerService lemmaAnalyzerService;
    private final FrequencyFilter frequencyFilter;
    private final Logger logger = LoggerFactory.getLogger(SortedLemmas.class);


    public SortedLemmas(LemmaAnalyzerService lemmaAnalyzerService,
                        FrequencyFilter frequencyFilter) {
        this.lemmaAnalyzerService = lemmaAnalyzerService;
        this.frequencyFilter = frequencyFilter;
    }

    public void sortingLemmasByFrequency(String textFromApi, String siteUrl){

        Map<String, Integer> lemmasFromText = lemmaAnalyzerService.getLemmas(textFromApi);
        String[] sortedLemmasArr = lemmasFromText.keySet().toArray(new String[0]);
        Map<String, Integer> frequencyLemmasAndFilterBySite = frequencyFilter.filterCollection(sortedLemmasArr, siteUrl);

        logger.info("Леммы с отсортированной частотой: {}", frequencyLemmasAndFilterBySite);
    }
}
