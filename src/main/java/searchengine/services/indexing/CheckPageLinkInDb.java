package searchengine.services.indexing;

import org.springframework.stereotype.Service;
import searchengine.repositories.PageRepository;
import searchengine.utils.UrlUtils;

@Service
public class CheckPageLinkInDb {
    private final PageRepository pageRepository;
    public CheckPageLinkInDb(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    public boolean checkPage(String url){
        String removeSlash = UrlUtils.removeTrailingSlash(url);
        return !pageRepository.existsByPath(removeSlash);
    }
}