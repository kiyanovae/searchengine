package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.SiteEntity;
import searchengine.model.Status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IndexingService {
    private final SiteRepository siteRepo;
    private final PageRepository pageRepo;

    private final SitesList sites;

    public IndexingResponse startFullIndexing() {
        List<Site> sitesList = sites.getSites();
        List<SiteEntity> siteEntities = new ArrayList<>();
        //deleteAllRecords();

        for (int i = 0; i < sitesList.size(); i++) {
            Site site = sitesList.get(i);
            //siteEntities.add(createSite(i,site));
            //siteRepo.deleteById(i);
            siteRepo.save(createSite(i + 1,site));
        }

        IndexingResponse response = new IndexingResponse();
        response.setResult(true);
        return response;
    }

    public void deleteAllRecords() {
        siteRepo.deleteAll();
        pageRepo.deleteAll();
    }

    public SiteEntity createSite(int id, Site site) {
        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setId(id);
        siteEntity.setName(site.getName());
        siteEntity.setUrl(site.getUrl());
        siteEntity.setLastError(null);
        siteEntity.setStatus(Status.INDEXING);
        siteEntity.setStatusTime(Date.from(Instant.now()));
        return siteEntity;
    }


//    private final ExecutorService executorService;
//    private final ConcurrentHashMap<String, AtomicReference<Status>> indexingTasks;
//
//    public IndexingService() {
//        this.executorService = Executors.newSingleThreadExecutor();
//        this.indexingTasks = new ConcurrentHashMap<>();
//    }
//
//    public Status startFullIndexing(boolean forceReindex) {
//        String taskId = UUID.randomUUID().toString();
//        AtomicReference<Status> status = new AtomicReference<>(Status.INDEXING);
//        indexingTasks.put(taskId, status);
//
//        executorService.submit(() -> {
//            try {
//                status.set(Status.INDEXING);
//                performFullIndexing(forceReindex);
//                status.set(Status.INDEXED);
//            } catch (Exception e) {
//                status.set(Status.FAILED);
//                //status.setError(e.getMessage());
//            }
//        });
//
//        return status.get();
//    }
//
//    private void performFullIndexing(boolean forceReindex) {
//        // Здесь реализация полной индексации
//        // Пример:
//        SitesList sitesList = new SitesList();
//        sitesList.getSites().forEach(site -> {
//            if (forceReindex || site.getLastIndexed() == null) {
//                indexingManager.indexSite(site);
//            }
//        });
//    }
//
//    public AtomicReference<Status> getIndexingStatus(String id) {
//        return indexingTasks.get(id);
//    }
}
