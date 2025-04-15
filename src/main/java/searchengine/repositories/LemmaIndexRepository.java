package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.models.indexData;

public interface LemmaIndexRepository extends JpaRepository<indexData, Long> {
    boolean existsByLemmaIdAndPageId(int lemmaId, int pageId);
}
