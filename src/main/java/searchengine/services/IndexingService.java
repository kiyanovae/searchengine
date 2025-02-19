package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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

import javax.persistence.OptimisticLockException;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private ForkJoinPool forkJoinPool = new ForkJoinPool();
    List<ForkJoinTask<?>> tasks = new ArrayList<>();

    private ThreadPoolExecutor executor;

    //private static AtomicBoolean startChecker = new AtomicBoolean(false);

    public Response startFullIndexing() {
        Response response = null;
            if (indexingStatusCheck(Status.INDEXING)) {
                ErrorResponse errorResponse = new ErrorResponse("Индексация уже запущена");
                errorResponse.setResult(false);
                response = errorResponse;
            } else {
                executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
                executor.setMaximumPoolSize(Runtime.getRuntime().availableProcessors());
                executor.execute(this::indexSite);
                //indexSite();
                IndexingResponse okResponse = new IndexingResponse();
                okResponse.setResult(true);
                response = okResponse;
            }

        return response;
    }

    public void indexSite() {
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

            Optional<SiteEntity> savedSite = siteRepository.findById(siteEntity.getId());
            if (savedSite.isEmpty()) {
                throw new RuntimeException("Не удалось сохранить сайт: " + site.getUrl());
            }

            try {
                log.info("Запуск обхода страниц для сайта с ID: {}", savedSite.get().getId());
                forkJoinPool.submit(() -> {
                    indexPage(siteEntity.getId(), "/");//запуск задачи
                }).join();
            } catch (Exception e) {
                siteEntity.setStatus(Status.FAILED);
                siteEntity.setLastError(e.getMessage());
            } finally {
                log.info("Индексация завершена для сайта: {}", site.getUrl());
                siteEntity.setStatus(Status.INDEXED);
                siteEntity.setStatusTime(Date.from(Instant.now()));
                siteRepository.save(siteEntity);
            }
        }
    }

    private SiteEntity createSiteEntity(Site site) {
        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setName(site.getName());
        siteEntity.setUrl(site.getUrl());
        siteEntity.setLastError(null);
        siteEntity.setStatus(Status.INDEXING);
        siteEntity.setStatusTime(Date.from(Instant.now()));
        return siteEntity;
    }

    @Transactional
    private void indexPage(Integer siteId, String path) {
        try {
            SiteEntity attachedSite = siteRepository.findById(siteId)
                    .orElseThrow(() -> new RuntimeException("Сайт с ID " + siteId + " не найден"));

            String fullUrl = attachedSite.getUrl() + path;
            Document doc = Jsoup.connect(fullUrl)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .timeout(5000)
                    .get();

            String content = doc.html();
            int status = doc.connection().response().statusCode();

            PageEntity page = new PageEntity();
            page.setSite(attachedSite);
            page.setPath(path);
            page.setCode(status);
            page.setContent(content);
            try {
                pageRepository.save(page);
                attachedSite.setStatusTime(Date.from(Instant.now()));
                siteRepository.save(attachedSite);
            } catch (OptimisticLockException e) {
                log.error("Конфликт при обновлении сайта: " + path, e);
                indexPage(siteId, path);
            }

            Elements links = doc.select("a[href]");
            ConcurrentSkipListSet<String> newPaths = new ConcurrentSkipListSet<>();
            for (Element link : links) {
                String nextUrl = link.attr("abs:href");
                if (nextUrl.startsWith(attachedSite.getUrl()) && isLink(nextUrl) && !isFile(nextUrl)) {
                    String newPath = nextUrl.substring(attachedSite.getUrl().length());
                    newPaths.add(newPath);
                }
            }


            for (String newPath : newPaths) {
                tasks.add(ForkJoinTask.adapt(() -> indexPage(siteId, newPath)));//создание задачи
            }

            ForkJoinTask.invokeAll(tasks);//запуск на параллельное выполнение
            Thread.sleep(1000);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Ошибка при обходе страницы: " + path, e);
        }
    }

    private static boolean isLink(String link) {
        String regex = "(^https:\\/\\/)(?:[^@\\/\\n]+@)?(?:www\\.)?([^:\\/\\n]+)";
        return link.matches(regex);
    }

    private static boolean isFile(String link) {
        link = link.toLowerCase();
        return link.contains(".jpg")
                || link.contains(".jpeg")
                || link.contains(".png")
                || link.contains(".gif")
                || link.contains(".webp")
                || link.contains(".pdf")
                || link.contains(".eps")
                || link.contains(".xlsx")
                || link.contains(".doc")
                || link.contains(".pptx")
                || link.contains(".docx")
                || link.contains("?_ga");
    }

    public boolean indexingStatusCheck(Status status) {
        for (Site site : sites.getSites()) {
            Optional<SiteEntity> savedSite = siteRepository.findByName(site.getName());
            if (savedSite.isPresent() && savedSite.get().getStatus() == status) {
                return true;
            }
        }
        return false;
    }

//    public Response stopIndexing() {
//        Response response = null;
//            if (isIndexingStarted()) {
//                ErrorResponse errorResponse = new ErrorResponse("Индексация не запущена");
//                errorResponse.setResult(false);
//                response = errorResponse;
//            } else {
//                stopFJPool();
//                ErrorResponse errorResponse = new ErrorResponse("Индексация остановлена пользователем");
//                errorResponse.setResult(true);
//                response = errorResponse;
//            }
//
//        return response;
//    }

    public Response stopIndexing() {
        Response response = null;
        try {
            if (!indexingStatusCheck(Status.INDEXING)) {
                ErrorResponse errorResponse = new ErrorResponse("Индексация не запущена");
                errorResponse.setResult(true);
                response = errorResponse;
                return response;
            }

            stopFJPool();
            if (executor != null) {
                executor.shutdownNow();
            }
            updateSiteStatuses(Status.FAILED);
            ErrorResponse successResponse = new ErrorResponse("Индексация остановлена пользователем");
            successResponse.setResult(true);
            response = successResponse;

        } catch (Exception e) {
            log.error("Ошибка при остановке индексации", e);
            ErrorResponse errorResponse = new ErrorResponse("Ошибка при остановке индексации");
            errorResponse.setResult(false);
            response = errorResponse;
        }

        return response;
    }

    @Transactional
    private void updateSiteStatuses(Status status) {
        List<SiteEntity> indexingSites = siteRepository.findByStatus(Status.INDEXING);
        for (SiteEntity site : indexingSites) {
            site.setStatus(status);
            site.setStatusTime(Date.from(Instant.now()));
            siteRepository.save(site);
        }
    }

    public void stopFJPool() {
        for (ForkJoinTask<?> task : tasks) {
            task.cancel(true);
        }
        forkJoinPool.shutdownNow(); // Останавливаем все выполняющиеся задачи
        tasks.clear();
        log.info("Индексация остановлена пользователем");
    }

}