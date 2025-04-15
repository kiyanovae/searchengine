package searchengine.services.indexing;

import org.springframework.stereotype.Service;
import searchengine.config.IndexingSettings;
import searchengine.models.Site;

import java.util.List;

@Service
public class CheckLinkInScopeLinks {
    private final IndexingSettings indexingSettings;
    public CheckLinkInScopeLinks(IndexingSettings indexingSettings) {
        this.indexingSettings = indexingSettings;
    }

    public boolean checkLink(String link){

        List<Site> sites = indexingSettings.getSites();
        return sites.stream()
                .anyMatch(site -> site.getUrl().equals(link));
    }
}
