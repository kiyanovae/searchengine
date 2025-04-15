package searchengine.config;

import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class LuceneConfig {

    @Bean
    public RussianLuceneMorphology luceneMorphology() throws IOException {
        try {
            return new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка инициализации LuceneMorphology", e);
        }
    }
}
