package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.exceptions.StoppedByUserException;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UpdaterService {
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final StatusService statusService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PageEntity cleanAndUpdatePage(SiteEntity site, String path, int code, String content) {
        Optional<PageEntity> optionalPage = pageRepository.findByPathAndSite(path, site);
        if (optionalPage.isPresent()) {
            PageEntity page = optionalPage.get();
            List<Integer> lemmaIdsByPageId = indexRepository.findLemmaIdsByPageId(page.getId());
            indexRepository.deleteByPageId(page.getId());
            List<LemmaEntity> lemmas = lemmaRepository.findAllById(lemmaIdsByPageId);
            List<Integer> updateLemmasIds = new ArrayList<>();
            List<Integer> deleteLemmasIds = new ArrayList<>();
            lemmas.forEach(lemma -> {
                if (lemma.getFrequency() - 1 == 0) {
                    deleteLemmasIds.add(lemma.getId());
                } else {
                    updateLemmasIds.add(lemma.getId());
                }
            });
            lemmaRepository.updateFrequencyAllByIds(updateLemmasIds);
            lemmaRepository.deleteAllByIdInBatch(deleteLemmasIds);
            page.setCode(code);
            page.setContent(content);
            return pageRepository.save(page);
        }
        return null;
    }


    @Transactional
    public void cleanSite(String url) {
        siteRepository.findByUrl(url).ifPresent(site -> {
            List<Integer> pageIdsBySite = pageRepository.findIdsBySite(site);
            for (int pageId : pageIdsBySite) {
                pageRepository.customDeleteById(pageId);
                if (statusService.isIndexingStoppedByUser()) {
                    throw new StoppedByUserException("");
                }
            }
            siteRepository.delete(site);
        });
    }
}
