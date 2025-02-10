package searchengine.services;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.config.Site;
import searchengine.model.SiteEntity;

public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {
    void deleteByName(String name);
    SiteEntity findByName(String name);
}
