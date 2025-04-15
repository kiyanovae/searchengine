package searchengine.services.indexing;

import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExtractAndFilterLinks {
    public Set<String> extractAndFilterLink(Document document) {
        return document.select("a[href]")
                .stream()
                .map(element -> element.attr("abs:href"))
                .filter(link -> !link.contains("#"))
                .filter(link -> link.startsWith("http://") || link.startsWith("https://"))
                .collect(Collectors.toSet());
    }
}
