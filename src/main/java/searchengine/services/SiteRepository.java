package searchengine.services;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;
import searchengine.config.Site;
import searchengine.model.SiteEntity;

import javax.persistence.QueryHint;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {
    void deleteByName(String name);
    SiteEntity findByName(String name);
    Optional<SiteEntity> findByUrl(String url);
    @QueryHints({@QueryHint(name = "org.hibernate.cacheable", value = "false")})
    Optional<SiteEntity> findById(Integer id);
}
