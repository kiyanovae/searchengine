package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PageEntity savePage(SiteEntity site, Connection.Response response) {
        String path = response.url().getPath();
        if (pageRepository.existsByPathAndSite(path, site)) {
            return null;
        }
        return pageRepository.save(new PageEntity(site, path, response.statusCode(), response.body()));
    }

    @Retryable(maxAttempts = 14, backoff = @Backoff(delay = 200))
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateSiteStatusTime(SiteEntity site) {
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }
}
