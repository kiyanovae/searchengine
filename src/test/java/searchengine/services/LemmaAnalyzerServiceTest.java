package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import searchengine.config.IndexingSettings;
import searchengine.services.indexing.LemmaAnalyzerService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class LemmaAnalyzerServiceTest {

    @Mock
    private LuceneMorphology luceneMorphology;

    @Mock
    private IndexingSettings indexingSettings;

    private LemmaAnalyzerService lemmaAnalyzerService;

//    @BeforeEach
//    void setUp(){
//        MockitoAnnotations.openMocks(this);
//        when(indexingSettings.getSites()).thenReturn(List.of(
//                Site.createForTest("Test Site", "https://example.com")
//        ));
//        lemmaAnalyzerService = new LemmaAnalyzerService(indexingSettings);
//    }

    @Test
    @DisplayName("ValidTest")
    void testGetLemmas_validText(){
        when(luceneMorphology.getMorphInfo("пример")).thenReturn(List.of("пример|С"));
        when(luceneMorphology.getMorphInfo("тест")).thenReturn(List.of("тест|С"));

        String text = "Пример тест";

        Map<String, Integer> lemmas = lemmaAnalyzerService.getLemmas(text);

        assertNotNull(lemmas);
        assertEquals(4, lemmas.size());                    // 4 ввиду спицифичности библиотеки luceneMorphology.
        assertEquals(1, lemmas.get("пример"));
        assertEquals(1, lemmas.get("тест"));
    }

    @Test
    @DisplayName("EmptyTest")
    void testGetLemmas_emptyText(){
        Map<String, Integer> lemmas = lemmaAnalyzerService.getLemmas("");
        assertNotNull(lemmas);
        assertTrue(lemmas.isEmpty());
    }

    @Test
    @DisplayName("particlesAndUnions")
    void getLemmas_withStopWords(){
        when(luceneMorphology.getMorphInfo("и")).thenReturn(List.of("и|СОЮЗ"));
        when(luceneMorphology.getMorphInfo("пример")).thenReturn(List.of("пример|С"));

        String text = "и пример";

        Map<String, Integer> lemmas = lemmaAnalyzerService.getLemmas(text);

        assertNotNull(lemmas);
        assertEquals(2, lemmas.size());                    // 2 ввиду спицифичности библиотеки luceneMorphology.
        assertEquals(1, lemmas.get("пример"));
    }

    @Test
    @DisplayName("HyphenWordsTest")
    void getLemmas_withHyphenWords(){
        when(luceneMorphology.getMorphInfo("по-русски")).thenReturn(List.of("по-русски|Н"));
        when(luceneMorphology.getMorphInfo("пример")).thenReturn(List.of("пример|С"));

        String text = "пример по-русски";

        Map<String, Integer> lemmas = lemmaAnalyzerService.getLemmas(text);

        assertNotNull(lemmas);
        assertEquals(3, lemmas.size());                    // 3 ввиду спицифичности библиотеки luceneMorphology.
        assertEquals(1, lemmas.get("пример"));
    }
}