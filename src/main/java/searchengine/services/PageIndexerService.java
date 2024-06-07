package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class PageIndexerService {
    private static final int MAX_STATUS_CODE = 400;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaFinderService lemmaFinderService;
    private final StatusService statusService;
    private final LemmaRepository lemmaRepository;
    @Lazy
    private final PageIndexerService pageIndexerService;

    @Autowired
    public PageIndexerService(IndexRepository indexRepository, PageRepository pageRepository,
                              SiteRepository siteRepository, LemmaFinderService lemmaFinderService,
                              StatusService statusService, LemmaRepository lemmaRepository,
                              @Lazy PageIndexerService pageIndexerService) {
        this.indexRepository = indexRepository;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.lemmaFinderService = lemmaFinderService;
        this.statusService = statusService;
        this.lemmaRepository = lemmaRepository;
        this.pageIndexerService = pageIndexerService;
    }

    public void handle(String url, SiteEntity site, String path, int code, String content) {
        if (statusService.isIndexingStopped()) {
            statusService.decrementAdditionalTaskCount();
            return;
        }
        if (!pageRepository.existsByPathAndSite(path, site)) {
            PageEntity page;
            page = pageIndexerService.savePage(site, path, code, content);
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

    public void index(SiteEntity site, PageEntity page) {
        HashMap<String, Integer> lemmas = lemmaFinderService.collectLemmas(page.getContent());
        int pageId = page.getId();
        List<IndexEntity> indexList = new ArrayList<>();
        for (Map.Entry<String, Integer> lemmaEntry : lemmas.entrySet()) {
            String lemma = lemmaEntry.getKey();
            int rank = lemmaEntry.getValue();
            LemmaEntity lemmaEntity = pageIndexerService.saveOrUpdateLemma(lemma, site);
            indexList.add(new IndexEntity(pageId, lemmaEntity.getId(), rank));
        }
        indexRepository.saveAll(indexList);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }

    @Retryable(maxAttempts = 14, backoff = @Backoff(delay = 200))
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LemmaEntity saveOrUpdateLemma(String lemma, SiteEntity site) {
        Optional<LemmaEntity> optionalLemma = lemmaRepository.findByLemmaAndSite(lemma, site);
        if (optionalLemma.isEmpty()) {
            synchronized (this) {
                optionalLemma = lemmaRepository.findByLemmaAndSite(lemma, site);
                if (optionalLemma.isEmpty()) {
                    return pageIndexerService.saveLemma(lemma, site);
                }
            }
            return updateLemma(optionalLemma.get());
        }
        return updateLemma(optionalLemma.get());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LemmaEntity saveLemma(String lemma, SiteEntity site) {
        return lemmaRepository.save(new LemmaEntity(site, lemma, 1));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PageEntity savePage(SiteEntity site, String path, int code, String content) {
        synchronized (this) {
            if (pageRepository.existsByPathAndSite(path, site)) {
                return null;
            }
            PageEntity page = pageRepository.save(new PageEntity(site, path, code, content));
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
            return page;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PageEntity cleanAndUpdatePage(SiteEntity site, String path, int code, String content) {
        Optional<PageEntity> optionalPage = pageRepository.findByPathAndSite(path, site);
        if (optionalPage.isPresent()) {
            PageEntity page = optionalPage.get();
            List<Integer> lemmaIdsByPageId = indexRepository.findLemmaIdsByPageId(page.getId());
            indexRepository.deleteByPageId(page.getId());
            List<LemmaEntity> lemmas = lemmaRepository.findAllById(lemmaIdsByPageId);
            List<Integer> updateLemmasIds = new ArrayList<>();
            List<Integer> deleteLemmasIds = new ArrayList<>();
            lemmas.forEach(lemma -> {
                if (lemma.getFrequency() - 1 == 0) {
                    deleteLemmasIds.add(lemma.getId());
                } else {
                    updateLemmasIds.add(lemma.getId());
                }
            });
            lemmaRepository.updateFrequencyAllByIds(updateLemmasIds);
            lemmaRepository.deleteAllByIdInBatch(deleteLemmasIds);
            page.setCode(code);
            page.setContent(content);
            return pageRepository.save(page);
        }
        return null;
    }

    private void indexPage(SiteEntity site, PageEntity page) {
        if (page.getCode() < MAX_STATUS_CODE) {
            this.index(site, page);
        }
    }

    private void updateAndIndexPage(SiteEntity site, String path, int code, String content) {
        synchronized (this) {
            PageEntity page = pageIndexerService.cleanAndUpdatePage(site, path, code, content);
            if (page != null) {
                indexPage(site, page);
            }
        }
    }

    private LemmaEntity updateLemma(LemmaEntity lemma) {
        lemma.setFrequency(lemma.getFrequency() + 1);
        return lemmaRepository.save(lemma);
    }
}