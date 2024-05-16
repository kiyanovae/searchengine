package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexEntity;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
    @Query(value = "SELECT SUM(idx.rank) FROM Index idx WHERE idx.pageId = :pageId AND idx.lemmaId IN :lemmaIds")
    Long getRankSumByPageIdAndLemmaIds(int pageId, List<Integer> lemmaIds);

    @Query(value = "SELECT idx.pageId FROM Index idx WHERE idx.lemmaId = :lemmaId")
    List<Integer> findPageIdsByLemmaId(int lemmaId);

    @Query(value = "SELECT idx.lemmaId FROM Index idx WHERE idx.pageId = :pageId")
    List<Integer> findLemmaIdsByPageId(int pageId);

    void deleteByPageId(int pageId);
}