package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.models.Site;

import java.time.LocalDateTime;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {
    boolean existsByUrl(String url);
    Site findByUrl(String url);

    @Query("SELECT s.statusTime FROM Site s WHERE s.url = :siteUrl")
    LocalDateTime findStatusTimeBySiteUrl(String siteUrl);
    @Query("SELECT CAST(s.status AS string) FROM Site s WHERE s.url = :url")
    String findStatusAsStringByUrl(@Param("url") String url);
    @Query("SELECT s.lastError FROM Site s WHERE s.url = :url")
    String findLastErrorByUrl(@Param("url") String url);
    @Query("SELECT s.id FROM Site s WHERE s.url = :url")
    Integer findIdByUrl(@Param("url") String url);
}
