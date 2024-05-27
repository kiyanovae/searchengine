package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    Optional<LemmaEntity> findByLemmaAndSite(String lemma, SiteEntity site);

    List<LemmaEntity> findBySiteAndLemmaInAndFrequencyLessThanEqual(SiteEntity site, Set<String> lemmas,
                                                                    int maxFrequency);

    int countBySite(SiteEntity site);

    @Modifying
    @Query(value = "UPDATE Lemma l SET l.frequency = l.frequency - 1 WHERE l.id IN :ids")
    void updateFrequencyAllByIds(List<Integer> ids);
}
