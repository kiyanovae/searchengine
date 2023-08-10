package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;


@Repository
public interface IndexSearchRepository extends JpaRepository<Index, Integer> {

    @Query(value = "select * from `index` i where i.page_id = :pageId and i.lemma_id = :lemmaId", nativeQuery = true)
    Index indexSearchExists(@Param("pageId") String pageId, @Param("lemmaId") String lemmaId);
}
