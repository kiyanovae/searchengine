package searchengine.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;

import javax.transaction.Transactional;
import java.time.LocalDateTime;

@Repository
public interface SiteRepository extends CrudRepository<Site, Integer> {
    @Transactional
    @Modifying
    @Query(value = "UPDATE Site s SET s.statusTime=:statusTime WHERE s.id=:siteId")
    void updateStatusTime(@Param("siteId") int siteId, @Param("statusTime") LocalDateTime statusTime);

}
