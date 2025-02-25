package searchengine.services.parser;


import lombok.Getter;

import java.util.concurrent.CopyOnWriteArrayList;

import static searchengine.services.IndexingService.getProtocolAndDomain;

@Getter
public class SiteMap {
    private String url;
    private String domain;
    private CopyOnWriteArrayList<SiteMap> siteMapChildrens;

    public SiteMap(String url) {
        siteMapChildrens = new CopyOnWriteArrayList<>();
        this.url = url;
        this.domain = getProtocolAndDomain(url);
    }

    public void addChildren(SiteMap children) {
        siteMapChildrens.add(children);
    }

}
