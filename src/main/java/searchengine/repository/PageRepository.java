package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.SiteTable;

import java.util.List;
//import searchengine.model.PrimaryKeyPage;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    Page findByPath(String path);

    List<Page> findAllBySite (SiteTable site);

}
