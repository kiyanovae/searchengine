package searchengine.services;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class Page {
    private int id;
    private double relevance;
}
