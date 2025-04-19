package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexTable;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexTable, Integer> {
    List<IndexTable> findAllByPageId(int pageId);

    List<IndexTable> findAllByLemmaId(int lemmaId);

    IndexTable findByPageIdAndLemmaId(int pageId, int lemmaId);
}
