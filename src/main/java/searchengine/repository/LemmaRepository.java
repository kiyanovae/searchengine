package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    Lemma findById(int id);

    Lemma findByLemmaAndSiteId(String lemma, int siteId);

    List<Lemma> findBySiteId(int siteId);

    List<Lemma> findAllByLemma(String lemma);
}
