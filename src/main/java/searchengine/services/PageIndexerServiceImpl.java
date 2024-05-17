package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    @Override
    public void handle(SiteEntity site, Connection.Response response) {
        if (statusService.isIndexingStoppedByUser() || statusService.isAdditionalTasksStoppedByIndexing()) {
            statusService.decrementAdditionalTaskCount();
            return;
        }
        String path = response.url().getPath();
        int code = response.statusCode();
        String content = response.body();
        if (!pageRepository.existsByPathAndSite(path, site)) {
            boolean isNewPage;
            synchronized (this) {
                isNewPage = saverService.savePage(site, path, code, content);
            }
            if (isNewPage) {
                indexPageWithLock(site, path, code);
            } else {
                updateAndIndexPageWithLock(site, path, code, content);
            }
        } else {
            updateAndIndexPageWithLock(site, path, code, content);
        }
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
        statusService.decrementAdditionalTaskCount();
        log.info(site.getUrl() + path + " has been indexed");
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

    void updateAndIndexPageWithLock(SiteEntity site, String path, int code, String content) {
        pageRepository.findForUpdateByPathAndSite(path, site).ifPresent(page -> {
            page.setCode(code);
            page.setContent(content);
            deletePageFromOtherTables(page);
            pageRepository.save(page);
            if (code < MAX_STATUS_CODE) {
                this.index(site, page);
            }
        });
    }

    void indexPageWithLock(SiteEntity site, String path, int code) {
        pageRepository.findForUpdateByPathAndSite(path, site).ifPresent(page -> {
            if (code < MAX_STATUS_CODE) {
                this.index(site, page);
            }
        });
    }

    private void deletePageFromOtherTables(PageEntity page) {
        List<Integer> lemmaIdsByPageId = indexRepository.findLemmaIdsByPageId(page.getId());
        updaterService.deleteIndexesContainsPage(page);
        lemmaIdsByPageId.forEach(updaterService::updateLemma);
    }
}