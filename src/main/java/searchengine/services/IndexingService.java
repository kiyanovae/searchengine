package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.Response;
import searchengine.dto.indexing.ErrorResponse;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.services.parser.SiteMap;
import searchengine.services.parser.SiteMapRecursiveAction;

import javax.persistence.NonUniqueResultException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class IndexingService {
    private static final Logger log = LoggerFactory.getLogger(IndexingService.class);

    @Autowired
    private final SiteRepository siteRepository;

    @Autowired
    private final PageRepository pageRepository;

    @Value("${indexing-settings.user-agent}")
    private String userAgent;

    @Value("${indexing-settings.referrer}")
    private String referrer;

    @Autowired
    private final SitesList sites;

    private ForkJoinPool forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors() * 2);
    List<ForkJoinTask<?>> tasks = new CopyOnWriteArrayList<>();

    private ThreadPoolExecutor executor;

    private AtomicBoolean isStopped = new AtomicBoolean(false);
    private SiteMapRecursiveAction task;

    public Response startFullIndexing() {
        Response response;
        isStopped.set(false);
            if (indexingStatusCheck(Status.INDEXING)) {
                ErrorResponse errorResponse = new ErrorResponse("Индексация уже запущена");
                log.warn("Индексация уже запущена");
                errorResponse.setResult(false);
                response = errorResponse;
            } else {
                log.info("Запущена индексация");
                forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors() * 2);
                executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
                executor.setMaximumPoolSize(Runtime.getRuntime().availableProcessors());
                executor.execute(this::indexSite);
                IndexingResponse okResponse = new IndexingResponse();
                okResponse.setResult(true);
                response = okResponse;
            }
        return response;
    }

    public void indexSite() {
        if (isStopped.get()) {
            task.stopRecursiveAction();
            return;
        }
        List<Site> sitesList = sites.getSites();
        for (Site site : sitesList) {
            Optional<SiteEntity> existingSite = siteRepository.findByUrl(site.getUrl());
            if (existingSite.isPresent()) {
                pageRepository.deleteBySiteId(existingSite.get().getId());
                siteRepository.delete(existingSite.get());
            }
            SiteEntity siteEntity = createSiteEntity(site);
            siteRepository.save(siteEntity);
            siteRepository.flush();

            try {
                forkJoinPool.submit(() -> {
                    indexPage(siteEntity.getId());//запуск задачи
                }).join();
                siteEntity.setStatus(Status.INDEXED);
                siteEntity.setStatusTime(Date.from(Instant.now()));
            } catch (Exception e) {
                siteEntity.setStatus(Status.FAILED);
                siteEntity.setLastError(e.getMessage());
                //log.warn("Статус индексации FAILED: {}", String.valueOf(e));
                return;
            } finally {
                siteEntity.setStatusTime(Date.from(Instant.now()));
                siteRepository.save(siteEntity);
            }
            if (siteEntity.getStatus().equals(Status.INDEXED) && !isStopped.get()) {
                log.info("Индексация завершена для сайта: {}", site.getUrl());
            }
        }
    }

    private SiteEntity createSiteEntity(Site site) {
        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setName(site.getName());
        siteEntity.setUrl(site.getUrl());
        if (site.getUrl().contains("www.")) {
            siteEntity.setUrl(site.getUrl().replace("www.",""));
        }
        siteEntity.setLastError(null);
        siteEntity.setStatus(Status.INDEXING);
        siteEntity.setStatusTime(Date.from(Instant.now()));
        return siteEntity;
    }

    @Transactional
    private void indexPage(Integer siteId) {
        if (isStopped.get()) {
            task.stopRecursiveAction();
            return;
        }
        log.info("Запуск обхода страниц для сайта с ID: {}", siteId);
        try {
            SiteEntity attachedSite = siteRepository.findById(siteId)
                    .orElseThrow(() -> new RuntimeException("Сайт с ID " + siteId + " не найден"));

            task = createTask(attachedSite);
            forkJoinPool.invoke(task);
            if (!task.getPageBuffer().isEmpty()) {
                pageRepository.saveAll(task.getPageBuffer());
                task.getPageBuffer().clear();
            }
            attachedSite.setStatusTime(Date.from(Instant.now()));
            siteRepository.save(attachedSite);

        } catch (Exception e) {
            if (!isStopped.get()) {
                //log.error("Ошибка при обходе страницы: {}", path, e);
                log.error("Ошибка при обходе страницы: ", e);
            }
        }
    }

    private SiteMapRecursiveAction createTask(SiteEntity attachedSite) {
        Set<String> linksPool = ConcurrentHashMap.newKeySet();
        Set<PageEntity> sitePageBuffer = ConcurrentHashMap.newKeySet();
        SiteMap siteMap = new SiteMap(attachedSite.getUrl());
        return new SiteMapRecursiveAction(siteMap, attachedSite, pageRepository, isStopped,
                sitePageBuffer, linksPool);
    }

    public static String getProtocolAndDomain(String url) {
        String regEx = "(^https:\\/\\/)(?:[^@\\/\\n]+@)?(?:www\\.)?([^:\\/\\n]+)";
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(regEx);
        String utf8EncodedString = StandardCharsets.UTF_8.decode(buffer).toString();
        Pattern pattern = Pattern.compile(utf8EncodedString);
        return pattern.matcher(url)
                .results()
                .map(m -> m.group(1) + m.group(2))
                .findFirst()
                .orElseThrow();
    }

    public boolean indexingStatusCheck(Status status) {
        for (Site site : sites.getSites()) {
            try {
                Optional<SiteEntity> savedSite = siteRepository.findByName(site.getName());
                if (savedSite.isPresent() && savedSite.get().getStatus() == status) {
                    return true;
                }
            } catch (Exception e) {
                List<SiteEntity> savedSite = siteRepository.findAll();
                savedSite.forEach(siteRepository::delete);
                return false;
            }

        }
        return false;
    }

    public Response stopIndexing() {
        Response response;
        if (!indexingStatusCheck(Status.INDEXING)) {
            ErrorResponse errorResponse = new ErrorResponse("Индексация не запущена");
            log.warn("Индексация не запущена");
            errorResponse.setResult(true);
            response = errorResponse;
            return response;
        }

        updateSiteStatuses(Status.INDEXING,Status.FAILED);
        isStopped.set(true);
        stopForkJoinPool();

        if (executor != null) {
            executor.shutdownNow();
            log.info(executor.isShutdown() ? "ThreadPoolExecutor успешно остановлен." : "ThreadPoolExecutor не был остановлен.");
        }
        ErrorResponse successResponse = new ErrorResponse("Индексация остановлена пользователем");
        log.info("Индексация остановлена пользователем");
        successResponse.setResult(true);
        response = successResponse;
        return response;
    }

    @Transactional
    private void updateSiteStatuses(Status from,Status to) {
        List<SiteEntity> indexingSites = siteRepository.findByStatus(from);
        for (SiteEntity site : indexingSites) {
            site.setStatus(to);
            site.setStatusTime(Date.from(Instant.now()));
            siteRepository.save(site);
        }
    }

    public void stopForkJoinPool() {
        for (ForkJoinTask<?> task : tasks) {
            task.cancel(true);
        }
        if (forkJoinPool != null && !forkJoinPool.isShutdown()) {
            forkJoinPool.shutdownNow();
            try {
                if (!forkJoinPool.awaitTermination(3, TimeUnit.SECONDS)) {
                    log.error("ForkJoinPool не завершился за 3 секунды");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        tasks.clear();
        log.info(forkJoinPool.isShutdown() ? "ForkJoinPool успешно остановлен." : "ForkJoinPool не был остановлен.");
    }

}