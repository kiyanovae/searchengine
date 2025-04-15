package searchengine.services.indexing;

import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.models.PageResult;
import searchengine.models.Site;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class GetInformPageAndSaveInDB {
    private final LemmaAnalyzerService lemmaAnalyzerService;
    private final SaveLemmasAndIndexInBD saveLemmasAndIndexInBD;
    private final SavePageInDatabase savePageInDatabase;
    private final SiteRepository siteRepository;


    public GetInformPageAndSaveInDB(
            LemmaAnalyzerService lemmaAnalyzerService,
            SaveLemmasAndIndexInBD saveLemmasAndIndexInBD,
            SavePageInDatabase savePageInDatabase,
            SiteRepository siteRepository) {
        this.lemmaAnalyzerService = lemmaAnalyzerService;
        this.saveLemmasAndIndexInBD = saveLemmasAndIndexInBD;
        this.savePageInDatabase = savePageInDatabase;
        this.siteRepository = siteRepository;
    }

    public void getInformDataPageAndSave(Site site, String url, Document document) {
        PageResult result = savePageInDatabase.gettingInformationAboutAPage(site, document, url);

        if (result == null || result.getContent() == null){
            return;
        }

        Map<String, Integer> lemmasCount = lemmaAnalyzerService.getLemmas(result.getContent());
        saveLemmasInDB(site, result, lemmasCount);
    }
    private void saveLemmasInDB(Site site, PageResult result, Map<String , Integer> lemmasCount) {
        saveLemmasAndIndexInBD.saveLemmasAndIndex(result.getId(), site.getId(), lemmasCount);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }
}