package searchengine.model;

import lombok.Data;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "lemma")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "site_id")
    private SiteTable site;

    @Column( columnDefinition = "VARCHAR(255)")
    private String lemma;

    private int frequency;

    @OneToMany(mappedBy = "lemma",cascade = CascadeType.ALL)
    private List<IndexTable> indexes;
}
