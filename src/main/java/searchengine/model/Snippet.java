package searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
public class Snippet {
    private final StringBuilder value;
    private int beginIndex;
    private int endIndex;
    private final Set<String> lemmas;
    private int totalLemmaCount;
}
