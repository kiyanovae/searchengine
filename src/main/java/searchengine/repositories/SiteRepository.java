package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.entities.SiteEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {
    Optional<SiteEntity> findByUrl(String url);

    Optional<SiteEntity> findByUrlAndStatus(String url, SiteEntity.SiteStatus status);

    List<SiteEntity> findByStatus(SiteEntity.SiteStatus status);
}
