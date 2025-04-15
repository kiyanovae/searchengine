package searchengine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import searchengine.models.Site;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "indexing-settings")
public class IndexingSettings {

    private List<Site> sites;
    private String userAgent;
    private String referrer;
    private int delay;

    public void setSites(List<Site> sites) {
        this.sites = sites;
    }

    public List<Site> getSites(){
        return sites;
    }
}
