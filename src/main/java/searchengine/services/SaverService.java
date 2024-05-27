package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.exceptions.StoppedByUserException;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SaverService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final UpdaterService updaterService;
    private final StatusService statusService;

    @Transactional
    public SiteEntity cleanUpAndSaveSite(String url, String name) {
        updaterService.cleanSite(url);
        if (statusService.isIndexingStoppedByUser()) {
            throw new StoppedByUserException("");
        }
        return siteRepository.save(new SiteEntity(SiteEntity.SiteStatus.INDEXING, url, name));
    }

    public SiteEntity saveSiteWithIndexedStatus(String url, String name) {
        return siteRepository.save(new SiteEntity(SiteEntity.SiteStatus.INDEXED, url, name));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
}
