package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.exceptions.RequestException;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class PageIndexerServiceImpl implements PageIndexerService {
    private static final AtomicInteger queuedTaskCount;
    //private static final Map<LemmaEntity, FrequencyIndex> lemmaIndexMap = new HashMap<>();
    //private static final List<IndexEntity> indexList = Collections.synchronizedList(new ArrayList<>());
    //private static final int BATCH_SIZE = 1000;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;

    static {
        queuedTaskCount = new AtomicInteger();
    }

    public static AtomicInteger getQueuedTaskCount() {
        return queuedTaskCount;
    }

    @Override
    public void indexPage(SiteEntity siteEntity, String path, int code, String content) throws IOException {
        synchronized (pageRepository) {
            int pageId = createOrUpdatePageInfo(siteEntity, path, code, content);
            if (code < 400) {
                indexingProcess(siteEntity, pageId, content);
            }
        }
        if (siteEntity.getStatus().equals(SiteStatus.INDEXING) && queuedTaskCount.get() == 1) {
            siteEntity.setStatus(SiteStatus.INDEXED);
        }
        siteEntity.setStatusTime(LocalDateTime.now());
        siteRepository.save(siteEntity);
        log.info(siteEntity.getUrl().concat(path.substring(1)) + " Done");
        if (queuedTaskCount.decrementAndGet() == 0) {
            IndexingServiceImpl.setSubIndexingIsRunning(false);
        }
    }

    @Override
    public void indexPage(int siteId, int pageId, String content) throws IOException {
        Optional<SiteEntity> optionalSiteEntity = siteRepository.findById(siteId);
        if (optionalSiteEntity.isPresent()) {
            SiteEntity site = optionalSiteEntity.get();
            indexingProcess(site, pageId, content);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        }
    }

    /*@Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void flushIndexEntities() {
        indexRepository.saveAll(indexList);
        indexList.clear();
    }*/

    @Override
    public PageEntity getNewPage(int siteId, String path, int code, String content) {
        SiteEntity site = getSite(siteId);
        PageEntity page;
        synchronized (pageRepository) {
            if (pageRepository.existsByPathAndSiteId(path, siteId)) {
                return null;
            }
            page = pageRepository.save(new PageEntity(site, path, code, content));
        }
        siteRepository.save(site);
        return page;
    }

    @Override
    public boolean isUrlUnexplored(String url, int siteId) {
        return !pageRepository.existsByPathAndSiteId(url, siteId);
    }

    private void indexingProcess(SiteEntity siteEntity, int pageId, String content) throws IOException {
        List<Integer> ranks = new ArrayList<>();
        List<LemmaEntity> existingLemmaEntities = new ArrayList<>();
        List<LemmaEntity> newLemmaEntities = new ArrayList<>();
        List<IndexEntity> indexList = new ArrayList<>();
        LemmaFinder lemmaFinder = LemmaFinder.getInstance();
        HashMap<String, Integer> lemmas = lemmaFinder.collectLemmas(lemmaFinder.stripHtmlTags(content));
        synchronized (lemmaRepository) {
            for (Map.Entry<String, Integer> lemmaEntry : lemmas.entrySet()) {
                if (IndexingServiceImpl.getIsStoppedByUser()) {
                    throw new RequestException("Индексация остановлена пользователем");
                }
                String lemma = lemmaEntry.getKey();
                int rank = lemmaEntry.getValue();
                Optional<LemmaEntity> optionalLemmaEntity = lemmaRepository.findByLemmaAndSiteId(lemma, siteEntity.getId());
                if (optionalLemmaEntity.isPresent()) {
                    LemmaEntity lemmaEntity = optionalLemmaEntity.get();
                    lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
                    existingLemmaEntities.add(lemmaEntity);
                    //addIndexEntityToList(new IndexEntity(pageId, lemmaEntity.getId(), rank));
                    indexList.add(new IndexEntity(pageId, lemmaEntity.getId(), rank));
                } else {
                    newLemmaEntities.add(new LemmaEntity(siteEntity, lemma, 1));
                    ranks.add(rank);
                }
            }
            lemmaRepository.saveAllAndFlush(existingLemmaEntities);
            newLemmaEntities = lemmaRepository.saveAllAndFlush(newLemmaEntities);
            siteRepository.saveAndFlush(siteEntity);
        }
        for (int i = 0; i < newLemmaEntities.size(); ++i) {
            //addIndexEntityToList(new IndexEntity(pageId, newLemmaEntities.get(i).getId(), ranks.get(i)));
            indexList.add(new IndexEntity(pageId, newLemmaEntities.get(i).getId(), ranks.get(i)));
        }
        //log.info("indexes : " + indexList.size());
        indexRepository.saveAll(indexList);
        //flushIndexEntities();
    }

    /*private void addIndexEntityToList(IndexEntity index) {
            indexList.add(index);
             if (indexList.size() >= BATCH_SIZE) {
            flushIndexEntities();
        }
    }*/

    private SiteEntity getSite(int siteId) {
        Optional<SiteEntity> optionalSite = siteRepository.findById(siteId);
        return optionalSite.orElse(null);
    }

    private int createOrUpdatePageInfo(SiteEntity site, String path, int code, String content) {
        Optional<PageEntity> optionalPageEntity = pageRepository.findByPathAndSiteId(path, site.getId());
        optionalPageEntity.ifPresent(page -> {
            deletePageInfoFromOtherTables(page.getId());
            pageRepository.delete(page);
        });
        PageEntity pageEntity = pageRepository.save(new PageEntity(site, path, code, content));
        siteRepository.save(site);
        return pageEntity.getId();
    }

    private void deletePageInfoFromOtherTables(int pageId) {
        List<IndexEntity> indexEntities = indexRepository.findByPageId(pageId);
        indexEntities.forEach(indexEntity -> {
            indexRepository.delete(indexEntity);
            updateLemmaInfo(indexEntity.getLemmaId());
        });
    }

    private void updateLemmaInfo(int lemmaId) {
        Optional<LemmaEntity> optionalLemmaEntity = lemmaRepository.findById(lemmaId);
        optionalLemmaEntity.ifPresent(lemmaEntity -> {
            int frequency = lemmaEntity.getFrequency();
            frequency--;
            if (frequency == 0) {
                lemmaRepository.delete(lemmaEntity);
            } else {
                lemmaEntity.setFrequency(frequency);
                lemmaRepository.save(lemmaEntity);
            }
        });
    }
}

/*private void indexingProcess(SiteEntity siteEntity, int pageId, String content) throws IOException {
        LemmaFinder lemmaFinder = LemmaFinder.getInstance();
        HashMap<String, Integer> lemmas = lemmaFinder.collectLemmas(lemmaFinder.stripHtmlTags(content));
        for (Map.Entry<String, Integer> lemmaEntry : lemmas.entrySet()) {
            if (IndexingServiceImpl.getIsStoppedByUser()) {
                throw new RequestException("Индексация остановлена пользователем");
            }
            String lemma = lemmaEntry.getKey();
            LemmaEntity lemmaEntity = new LemmaEntity(siteEntity, lemma, 0);
            synchronized (lemmaIndexMap) {
                FrequencyIndex frequencyIndex = lemmaIndexMap.get(lemmaEntity);
                if (frequencyIndex != null) {
                    frequencyIndex.setFrequency(frequencyIndex.getFrequency() + 1);
                } else {
                    frequencyIndex = new FrequencyIndex(1, new ArrayList<>());
                }
                frequencyIndex.getIndexes().add(new IndexEntity(pageId, lemmaEntry.getValue()));
                lemmaIndexMap.put(lemmaEntity, frequencyIndex);
            }
        }
    }*/

    /*@Transactional
    @Override
    public void flushLemmaIndex() {
        List<LemmaEntity> lemmaEntities = new ArrayList<>();
        for (Map.Entry<LemmaEntity, FrequencyIndex> entry : lemmaIndexMap.entrySet()) {
            LemmaEntity lemma = entry.getKey();
            lemma.setFrequency(entry.getValue().getFrequency());
            lemmaEntities.add(lemma);
        }
        log.info(String.valueOf(lemmaEntities.size()));
        lemmaEntities = lemmaRepository.saveAll(lemmaEntities);
        Iterator<LemmaEntity> itrLemmaEntities = lemmaEntities.iterator();
        List<IndexEntity> indexEntities = new ArrayList<>();
        for (FrequencyIndex entry : lemmaIndexMap.values()) {
            int lemmaId = itrLemmaEntities.next().getId();
            List<IndexEntity> indexes = entry.getIndexes();
            indexes.forEach(index -> index.setLemmaId(lemmaId));
            indexEntities.addAll(indexes);
        }
        log.info(String.valueOf(indexEntities.size()));
        indexRepository.saveAll(indexEntities);
        lemmaEntities.clear();
        indexEntities.clear();
    }*/
