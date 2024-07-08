package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.entities.PageEntity;
import searchengine.model.entities.SiteEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    boolean existsByPathAndSite(String path, SiteEntity site);

    Optional<PageEntity> findByPathAndSite(String path, SiteEntity site);

    int countBySite(SiteEntity site);

    @Query(value = "SELECT p.id FROM Page p WHERE p.site = :site")
    List<Integer> findIdsBySite(SiteEntity site);

    @Modifying
    @Query(value = "DELETE FROM Page p WHERE p.id = :pageId")
    void customDeleteById(int pageId);
}