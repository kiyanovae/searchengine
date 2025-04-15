//package searchengine.services;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.ArgumentCaptor;
//import searchengine.models.Lemma;
//import searchengine.models.indexData;
//import searchengine.repositories.LemmaIndexRepository;
//import searchengine.repositories.LemmaRepository;
//import searchengine.repositories.PageRepository;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.mockito.Mockito.*;
//
//class saveLemmasAndIndexInBDTest {
//
//    private LemmaRepository lemmaRepository;
//    private LemmaIndexRepository lemmaIndexRepository;
//    private PageRepository pageRepository;
//    private SaveLemmasAndIndexInBD lemmaIndexService;
//
//    @BeforeEach
//    void setUp() {
//        lemmaRepository = mock(LemmaRepository.class);
//        lemmaIndexRepository = mock(LemmaIndexRepository.class);
//        pageRepository = mock(PageRepository.class);
//        lemmaIndexService = new SaveLemmasAndIndexInBD(lemmaRepository, lemmaIndexRepository, pageRepository);
//    }
//
//    @Test
//    void saveLemmasAndIndex_shouldSaveNewLemmaAndIndex() {
//        Integer pageId = 1;
//        Integer siteId = 1;
//        Map<String, Integer> lemmas = createLemmas();
//
//        Lemma newLemma = createLemma(0);
//
//
////        when(lemmaRepository.findByLemma("example")).thenReturn(Optional.empty());
//        when(lemmaRepository.save(any(Lemma.class))).thenReturn(newLemma);
//        when(lemmaIndexRepository.existsByLemmaIdAndPageId(1, pageId)).thenReturn(false);
//        when(pageRepository.countBySiteId(siteId)).thenReturn(100);
//
//        lemmaIndexService.saveLemmasAndIndex(pageId, siteId, lemmas);
//
//        ArgumentCaptor<Lemma> lemmaCaptor = ArgumentCaptor.forClass(Lemma.class);
//        verify(lemmaRepository, times(2)).save(lemmaCaptor.capture());
//        Lemma savedLemma = lemmaCaptor.getValue();
//        assertEquals("example", savedLemma.getLemma());
//        assertEquals(1, savedLemma.getFrequency());
//
//        ArgumentCaptor<indexData> lemmaIndexCaptor = ArgumentCaptor.forClass(indexData.class);
//        verify(lemmaIndexRepository).save(lemmaIndexCaptor.capture());
//        indexData savedIndex = lemmaIndexCaptor.getValue();
//        assertEquals(1, savedIndex.getLemmaId());
//        assertEquals(pageId, savedIndex.getPageId());
//        assertEquals(10.0f, savedIndex.getRank());
//    }
//
//    @Test
//    void saveLemmasAndIndex_shouldNotSaveDuplicateIndex() {
//        Integer pageId = 1;
//        Integer siteId = 1;
//        Map<String, Integer> lemmas = createLemmas();
//
//        Lemma existingLemma = createLemma(1);
//
////        when(lemmaRepository.findByLemma("example")).thenReturn(Optional.of(existingLemma));
//        when(lemmaIndexRepository.existsByLemmaIdAndPageId(1, pageId)).thenReturn(true);
//
//        lemmaIndexService.saveLemmasAndIndex(pageId, siteId, lemmas);
//
//        verify(lemmaRepository, never()).save(any(Lemma.class));
//        verify(lemmaIndexRepository, never()).save(any(indexData.class));
//    }
//
//    @Test
//    void save_shouldNotSaveDuplicateLemmaIndex() {
//        Integer pageId = 1;
//        Integer siteId = 1;
//        Map<String, Integer> lemmas = createLemmas();
//
//        Lemma existingLemma = createLemma(1);
//
////        when(lemmaRepository.findByLemma("example")).thenReturn(Optional.of(existingLemma));
//        when(lemmaIndexRepository.existsByLemmaIdAndPageId(1, 1)).thenReturn(true);
//
//        lemmaIndexService.saveLemmasAndIndex(pageId, siteId, lemmas);
//
//        verify(lemmaIndexRepository, never()).save(any(indexData.class));
//    }
//
//    private Map<String, Integer> createLemmas(){
//        Map<String, Integer> lemmas = new HashMap<>();
//        lemmas.put("example", 10);
//        return lemmas;
//    }
//
//    private Lemma createLemma(Integer frequency) {
//        Lemma lemma = new Lemma();
//        lemma.setId(1);
//        lemma.setLemma("example");
//        lemma.setSiteId(1);
//        lemma.setFrequency(frequency);
//        return lemma;
//    }
//}