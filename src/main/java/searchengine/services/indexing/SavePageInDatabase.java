package searchengine.services.indexing;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.LockAcquisitionException;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.models.Page;
import searchengine.models.PageResult;
import searchengine.models.Site;
import searchengine.repositories.PageRepository;
import searchengine.utils.UrlUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Service
public class SavePageInDatabase {
    @PersistenceContext
    private EntityManager entityManager;
    private final PageRepository pageRepository;
    private final CheckPageLinkInDb checkPageLinkInDb;
    private final Logger logger = LoggerFactory.getLogger(SavePageInDatabase.class);

    public SavePageInDatabase(PageRepository pageRepository, CheckPageLinkInDb checkPageLinkInDb) {
        this.pageRepository = pageRepository;
        this.checkPageLinkInDb = checkPageLinkInDb;
    }

    public PageResult gettingInformationAboutAPage(Site site, Document document, String url) {
        String content = document.text();
        int statusCode = document.connection().response().statusCode();
        Integer pageId = savePageInDatabase(site, url, statusCode, content);

        if (pageId == null){
            logger.debug("Ошибка обработки: страница не была сохранена в БД, так как уже там находится: {}", url);
            return new PageResult(null, null);
        }

        return new PageResult(content, pageId);
    }

    @Transactional
    private Integer savePageInDatabase(Site site, String url, int statusCode, String content) {

        try {
            if (!checkPageLinkInDb.checkPage(url)){
                return null;
            }

            Page page = new Page();
            page.setContent(content);
            page.setSite(site);
            page.setPath(UrlUtils.removeTrailingSlash(url));
            page.setCode(statusCode);
            site.addPage(page);

            Page savePage = pageRepository.save(page);
            logger.info("Успешно обработана и сохранена страница: {}", url);

            return savePage.getId();
        }catch (LockAcquisitionException e){
            logger.error("Deadlock detected when processing URL: {}.", url);
            logger.error("Transaction details: {}.", getTransactionDetails());
            logger.error("Error details: ", e);
            return null;
        }
    }


    private String getTransactionDetails() {   //TEST!
        try {
            // Получаем сессию Hibernate из EntityManager
            Session session = entityManager.unwrap(Session.class);
            Transaction transaction = session.getTransaction();

            // Проверяем, активна ли транзакция
            String transactionStatus = transaction.isActive() ? "Active" : "Not active";

            // Для логирования SQL-запросов можно включить Hibernate логирование
            String currentSqlQuery = "SQL query logging can be enabled for debugging";  // Это можно настроить в конфигурации Hibernate или использовать логирование запросов

            return String.format("Transaction Status: %s, Current SQL: %s", transactionStatus, currentSqlQuery);
        } catch (Exception e) {
            logger.error("Ошибка при получении деталей транзакции", e);
            return "Ошибка при получении деталей транзакции";
        }
    }
}
