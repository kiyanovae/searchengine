package repositories;

import org.junit.jupiter.api.Test;
import searchengine.repositories.LemmaIndexRepository;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class indexDataRepositoryTest {

    @Test
    void existByLemmaIdAndPageId_shouldReturnTrue(){

        LemmaIndexRepository lemmaIndexRepository = mock(LemmaIndexRepository.class);

        when(lemmaIndexRepository.existsByLemmaIdAndPageId(1,1)).thenReturn(true);

        boolean result = lemmaIndexRepository.existsByLemmaIdAndPageId(1,1);

        assertTrue(result);

        verify(lemmaIndexRepository, times(1)).existsByLemmaIdAndPageId(1,1);
    }
}
