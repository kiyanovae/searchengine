package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;

import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    Optional<LemmaEntity> findByLemmaAndSiteId(String lemma, int siteId);
    int countBySiteId(int siteId);
}
