import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class Main {
    public static void main(String[] args) throws IOException {
        String WORD_TYPE_REGEX = "\\W\\w&&[^а-яА-Я\\s]";
        LuceneMorphology luceneMorph = new RussianLuceneMorphology();
        List<String> wordInfo = luceneMorph.getMorphInfo("б");
        for (String morphInfo : wordInfo) {
            System.out.println(morphInfo);
            System.out.println(morphInfo.matches(WORD_TYPE_REGEX));
        }
        System.out.println("яa&&p".matches(WORD_TYPE_REGEX));
        Document doc = Jsoup.connect("https://www.playback.ru/product/1124393.html").get();
        System.out.println(doc.title());
        String text = doc.text();
        System.out.println(text);
        System.out.println(textToArrayContainsWords(text));
    }

    private static String textToArrayContainsWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ");
    }
}
