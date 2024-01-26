package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexEntity;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
    List<IndexEntity> findByPageId(int id);
    List<IndexEntity> findByLemmaId(int id);
    List<IndexEntity> findAllByPageId(Iterable<Integer> ids);
}
