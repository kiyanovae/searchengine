package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class QueryLemmas {
    private final Set<String> nonParticipantSet;
    private final Set<String> filteredSet;

    public QueryLemmas() {
        this.nonParticipantSet = new HashSet<>();
        this.filteredSet = new HashSet<>();
    }
}
