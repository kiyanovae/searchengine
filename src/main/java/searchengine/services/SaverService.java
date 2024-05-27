package searchengine.services;

import org.jsoup.Connection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.exceptions.StoppedByUserException;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SaverService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final StatusService statusService;
    @Lazy
    private final SaverService saverService;

    @Autowired
    public SaverService(SiteRepository siteRepository, PageRepository pageRepository, StatusService statusService,
                        @Lazy SaverService saverService) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.statusService = statusService;
        this.saverService = saverService;
    }

    @Transactional
    public SiteEntity cleanUpAndSaveSite(String url, String name) {
        saverService.cleanSite(url);
        if (statusService.isIndexingStoppedByUser()) {
            throw new StoppedByUserException("");
        }
        return siteRepository.save(new SiteEntity(SiteEntity.SiteStatus.INDEXING, url, name));
    }

    @Transactional
    public void cleanSite(String url) {
        siteRepository.findByUrl(url).ifPresent(site -> {
            List<Integer> pageIdsBySite = pageRepository.findIdsBySite(site);
            for (int pageId : pageIdsBySite) {
                pageRepository.customDeleteById(pageId);
                if (statusService.isIndexingStoppedByUser()) {
                    throw new StoppedByUserException("");
                }
            }
            siteRepository.delete(site);
        });
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
