package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.entities.IndexEntity;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
    @Query(value = """
            SELECT new searchengine.model.Page(idx.pageId, CAST(SUM(idx.rank) as double)) \
            FROM Index idx \
            WHERE idx.pageId IN :pageIds AND idx.lemmaId IN :lemmaIds GROUP BY idx.pageId""")
    List<Page> getRankSumByPageIdsAndLemmaIds(List<Integer> pageIds, List<Integer> lemmaIds);

    @Query(value = "SELECT idx.pageId FROM Index idx WHERE idx.lemmaId = :lemmaId")
    List<Integer> findPageIdsByLemmaId(int lemmaId);

    @Query(value = "SELECT idx.lemmaId FROM Index idx WHERE idx.pageId = :pageId")
    List<Integer> findLemmaIdsByPageId(int pageId);

    @Modifying
    @Query(value = "DELETE FROM Index idx WHERE idx.pageId = :pageId")
    void deleteByPageId(int pageId);
}