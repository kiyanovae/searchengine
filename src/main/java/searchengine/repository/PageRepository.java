package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    @Query(value = "select COUNT(*) > 0 from page p where p.path=:path", nativeQuery = true)
    boolean existsByPath(@Param("path") String path);
}
