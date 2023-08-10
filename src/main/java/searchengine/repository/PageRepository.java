package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

    @Query(value = "select * from page p where p.site_id = :siteId and p.path = :path limit 1", nativeQuery = true)
    Page findPageBySiteIdAndPath(@Param("path") String path, @Param("siteId") Integer siteId);

    @Query(value = "select count(*) from page p where p.id = :siteId", nativeQuery = true)
    Integer findCountRecordBySiteId(@Param("siteId") Integer siteId);
}
