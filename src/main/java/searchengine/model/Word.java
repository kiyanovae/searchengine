package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class Word {
    private String value;
    private int index;
    private boolean isMatches;
    private String lemma;
}
