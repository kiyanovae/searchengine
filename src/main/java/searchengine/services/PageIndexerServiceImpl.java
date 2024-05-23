package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
@Slf4j
public class PageIndexerServiceImpl implements PageIndexerService {
    private static final int MAX_STATUS_CODE = 400;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaFinderService lemmaFinderService;
    private final StatusService statusService;
    private final UpdaterService updaterService;
    private final SaverService saverService;
    private final LemmaService lemmaService;

    @Override
    public void handle(String url, SiteEntity site, String path, int code, String content) {
        if (statusService.isIndexingStoppedByUser() || statusService.isAdditionalTasksStoppedByIndexing()) {
            statusService.decrementAdditionalTaskCount();
            return;
        }
        if (!pageRepository.existsByPathAndSite(path, site)) {
            PageEntity page;
            synchronized (this) {
                page = saverService.savePage(site, path, code, content);
            }
            if (page != null) {
                indexPage(site, page);
            } else {
                updateAndIndexPage(site, path, code, content);
            }
        } else {
            updateAndIndexPage(site, path, code, content);
        }
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
        statusService.decrementAdditionalTaskCount();
        log.info("{}  indexed", url);
    }

    @Override
    public void index(SiteEntity site, PageEntity page) {
        HashMap<String, Integer> lemmas = lemmaFinderService.collectLemmas(page.getContent());
        int pageId = page.getId();
        List<IndexEntity> indexList = new ArrayList<>();
        for (Map.Entry<String, Integer> lemmaEntry : lemmas.entrySet()) {
            String lemma = lemmaEntry.getKey();
            int rank = lemmaEntry.getValue();
            LemmaEntity lemmaEntity = lemmaService.saveOrUpdateLemma(lemma, site);
            indexList.add(new IndexEntity(pageId, lemmaEntity.getId(), rank));
        }
        indexRepository.saveAll(indexList);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }

    private void indexPage(SiteEntity site, PageEntity page) {
        if (page.getCode() < MAX_STATUS_CODE) {
            this.index(site, page);
        }
    }

    private void updateAndIndexPage(SiteEntity site, String path, int code, String content) {
        synchronized (this) {
            PageEntity page = updaterService.cleanAndUpdatePage(site, path, code, content);
            if (page != null) {
                indexPage(site, page);
            }
        }
    }
}