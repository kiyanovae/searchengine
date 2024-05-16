package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.LemmaRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LemmaServiceImpl implements LemmaService {
    private final LemmaRepository lemmaRepository;
    private final SaverService saverService;

    @Retryable(maxAttempts = 14, backoff = @Backoff(delay = 200))
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public LemmaEntity saveOrUpdateLemma(String lemma, SiteEntity site) {
        Optional<LemmaEntity> optionalLemma = lemmaRepository.findByLemmaAndSite(lemma, site);
        if (optionalLemma.isEmpty()) {
            synchronized (this) {
                optionalLemma = lemmaRepository.findByLemmaAndSite(lemma, site);
                if (optionalLemma.isEmpty()) {
                    return saverService.saveLemma(lemma, site);
                }
            }
            return updateLemma(optionalLemma.get());
        }
        return updateLemma(optionalLemma.get());
    }

    private LemmaEntity updateLemma(LemmaEntity lemma) {
        lemma.setFrequency(lemma.getFrequency() + 1);
        return lemmaRepository.save(lemma);
    }
}