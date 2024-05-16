package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    boolean existsByPathAndSite(String path, SiteEntity site);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PageEntity> findForUpdateByPathAndSite(String path, SiteEntity site);

    int countBySite(SiteEntity site);

    @Query(value = "SELECT p.id FROM Page p WHERE p.site = :site")
    List<Integer> findIdsBySite(SiteEntity site);
}
