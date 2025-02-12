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
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

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

    public IndexingResponse startFullIndexing() {
        List<Site> sitesList = sites.getSites();
        IndexingResponse response = new IndexingResponse();

        for (Site site : sitesList) {
            try {
                indexSite(site);
            } catch (Exception e) {
                response.setResult(false);
                //response.setError("Ошибка при индексации сайта: " + site.getUrl());
            }
        }

        response.setResult(true);
        return response;
    }

    public void indexSite(Site site) {
        // Удаляем старые данные
        Optional<SiteEntity> existingSite = siteRepository.findByUrl(site.getUrl());
        if (existingSite.isPresent()) {
            pageRepository.deleteBySiteId(existingSite.get().getId());
            siteRepository.delete(existingSite.get());
        }

        // Создаем новую запись
        SiteEntity siteEntity = createSite(site);
        siteRepository.save(siteEntity);
        siteRepository.flush();

        // Проверяем, что сущность сохранена
        Optional<SiteEntity> savedSite = siteRepository.findById(siteEntity.getId());
        if (savedSite.isEmpty()) {
            throw new RuntimeException("Не удалось сохранить сайт: " + site.getUrl());
        }

        try {
            log.info("Запуск обхода страниц для сайта с ID: {}", savedSite.get().getId());
            // Запускаем обход страниц
            forkJoinPool.submit(() -> {
                    indexPage(siteEntity.getId(), "/");
            }).join();
            siteEntity.setStatus(Status.INDEXED);
        } catch (Exception e) {
            siteEntity.setStatus(Status.FAILED);
            siteEntity.setLastError(e.getMessage());
        } finally {
            siteEntity.setStatusTime(Date.from(Instant.now()));
            siteRepository.save(siteEntity);
        }
    }

    private SiteEntity createSite(Site site) {
        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setName(site.getName());
        siteEntity.setUrl(site.getUrl());
        siteEntity.setLastError(null);
        siteEntity.setStatus(Status.INDEXING);
        siteEntity.setStatusTime(Date.from(Instant.now()));
        return siteEntity;
    }

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

            // Сохраняем страницу
            PageEntity page = new PageEntity();
            page.setSite(attachedSite);
            page.setPath(path);
            page.setCode(status);
            page.setContent(content);
            pageRepository.save(page);

            // Обновляем время статуса
            attachedSite.setStatusTime(Date.from(Instant.now()));
            siteRepository.save(attachedSite);

            // Находим новые ссылки
            Elements links = doc.select("a[href]");
            ConcurrentSkipListSet<String> newPaths = new ConcurrentSkipListSet<>();
            for (Element link : links) {
                String nextUrl = link.attr("abs:href");
                if (nextUrl.startsWith(attachedSite.getUrl())) {
                    String newPath = nextUrl.substring(attachedSite.getUrl().length());
                    if (!pageRepository.findBySiteIdAndPath(attachedSite.getId(), newPath).isPresent()) {
                        newPaths.add(newPath);
                    }
                }
            }

            // Рекурсивно обходим новые ссылки
            List<ForkJoinTask<?>> tasks = new ArrayList<>();
            for (String newPath : newPaths) {
                tasks.add(ForkJoinTask.adapt(() -> indexPage(siteId, newPath)));
            }

            ForkJoinTask.invokeAll(tasks);

            // Задержка между запросами
            Thread.sleep(1000); // 1 секунда
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Ошибка при обходе страницы: " + path, e);
        }
    }

}