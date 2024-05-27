package searchengine.services;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class QueryLemmas {
    private final Set<String> NonParticipantSet;
    private final Set<String> filteredSet;

    public QueryLemmas() {
        this.NonParticipantSet = new HashSet<>();
        this.filteredSet = new HashSet<>();
    }
}
