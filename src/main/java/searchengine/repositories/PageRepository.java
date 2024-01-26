package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    boolean existsByPathAndSiteId(String path, int siteId);

    Optional<PageEntity> findByPathAndSiteId(String path, int siteId);

    int countBySiteId(int siteId);
}
