package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.models.Page;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

    boolean existsByPath(String path);
    void deletePageByPath(String path);
    int countBySiteId(Integer siteId);
    @Query("SELECT COUNT(p) FROM Page p WHERE p.site.url = :siteUrl")
    int countPagesBySiteUrl(String siteUrl);

}