package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SaverServiceImpl implements SaverService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final UpdaterService updaterService;

    @Transactional
    @Override
    public SiteEntity cleanUpAndSaveSite(String url, String name) {
        updaterService.cleanSite(url);
        return siteRepository.save(new SiteEntity(SiteEntity.SiteStatus.INDEXING, url, name));
    }

    @Override
    public SiteEntity saveSiteWithIndexedStatus(String url, String name) {
        return siteRepository.save(new SiteEntity(SiteEntity.SiteStatus.INDEXED, url, name));
    }

    @Transactional
    @Override
    public PageEntity savePage(SiteEntity site, Connection.Response response) {
        String path = response.url().getPath();
        if (pageRepository.existsByPathAndSite(path, site)) {
            return null;
        }
        PageEntity page = pageRepository.save(new PageEntity(site, path, response.statusCode(), response.body()));
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
        return page;
    }

    @Override
    public boolean savePage(SiteEntity site, String path, int code, String content) {
        if (pageRepository.existsByPathAndSite(path, site)) {
            return false;
        }
        pageRepository.save(new PageEntity(site, path, code, content));
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public LemmaEntity saveLemma(String lemma, SiteEntity site) {
        return lemmaRepository.save(new LemmaEntity(site, lemma, 1));
    }
}
