package searchengine.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseCleaner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseCleaner.class);
    private final JdbcTemplate jdbcTemplate;

    public DatabaseCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

//    @PreDestroy
//    @Transactional
//    public void cleanDatabase() {
//        try {
//            List<String> tables = jdbcTemplate.queryForList("SHOW TABLES", String.class);
//
//            int rowsDeletedSite = deleteIfExists("site", tables);
//            int rowsDeletedPage = deleteIfExists("page", tables);
//            int rowsDeletedLemma = deleteIfExists("lemma", tables);
//            int rowsDeletedIndex = deleteIfExists("index_data", tables);
//
//            logger.info("Удалено записей: site = {}, page = {}, lemma = {}, index_data = {}",
//                    rowsDeletedSite, rowsDeletedPage, rowsDeletedLemma, rowsDeletedIndex);
//
//            logger.info("База данных успешно очищена.");
//        } catch (Exception e) {
//            logger.error("Ошибка при очистке базы данных: ", e);
//        }
//    }
//
//    private int deleteIfExists(String tableName, List<String> tables) {
//        if (tables.contains(tableName)) {
//            return jdbcTemplate.update("DELETE FROM " + tableName);
//        } else {
//            logger.warn("Таблица '{}' отсутствует в базе данных, пропускаем удаление.", tableName);
//            return 0;
//        }
//    }
}
