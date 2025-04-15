package searchengine.services.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;

import java.util.Iterator;
import java.util.Map;

@Service
public class FilterLemmasBySiteName {
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final Logger logger = LoggerFactory.getLogger(FilterLemmasBySiteName.class);

    public FilterLemmasBySiteName(LemmaRepository lemmaRepository,
                                  SiteRepository siteRepository) {
        this.lemmaRepository = lemmaRepository;
        this.siteRepository = siteRepository;
    }

    public Map<String, Integer> filterLemmas(Map<String, Integer> lemmasMap, String siteUrl){

        if (siteUrl.equals("ALL")){
            logger.info("Фильтрация не требуется.");  //TODO Удалить.
            return lemmasMap;
        }

        int siteId = siteRepository.findIdByUrl(siteUrl);

        Iterator<Map.Entry<String, Integer>> iterator = lemmasMap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String, Integer> entry = iterator.next();
            String lemma = entry.getKey();

            int lemmaSiteId = lemmaRepository.findSiteIdByLemmaName(lemma);
            logger.info("Получили ID сайта для леммы: {}", lemmaSiteId);

            if (lemmaSiteId != siteId){
                logger.info("Удаление леммы, так как она не проходит филтрацию по ID сайта: {}", lemma);  //TODO Протестить.
                iterator.remove();
            }
        }

        return lemmasMap;
    }
}
