package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.PageEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UpdaterServiceImpl implements UpdaterService {
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void deleteIndexesContainsPage(PageEntity page) {
        indexRepository.deleteByPageId(page.getId());
    }

    @Retryable(maxAttempts = 14, backoff = @Backoff(delay = 200))
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void updateLemma(int id) {
        lemmaRepository.findById(id).ifPresent(lemma -> {
            int frequency = lemma.getFrequency() - 1;
            if (frequency == 0) {
                lemmaRepository.delete(lemma);
            } else {
                lemma.setFrequency(frequency);
                lemmaRepository.save(lemma);
            }
        });
    }

    @Transactional
    @Override
    public void cleanSite(String url) {
        siteRepository.findByUrl(url).ifPresent(site -> {
            List<Integer> pageIdsBySite = pageRepository.findIdsBySite(site);
            pageRepository.deleteAllByIdInBatch(pageIdsBySite);
            siteRepository.delete(site);
        });
    }
}
