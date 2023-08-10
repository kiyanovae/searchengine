package searchengine.model;

import lombok.Data;

import javax.persistence.*;

import java.util.List;

import static javax.persistence.FetchType.EAGER;

@Table(name = "lemma")
@Entity(name = "lemma")
@Data
public class Lemma {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "lemma_sequence"
    )
    @SequenceGenerator(
            name = "lemma_sequence",
            allocationSize = 100
    )
    @Column(name = "id", nullable = false)
    private int id;

    @ManyToOne(fetch = EAGER, cascade = {CascadeType.ALL})
    @JoinColumn(name = "site_id", foreignKey = @ForeignKey(name = "site_lemma_fk"))
    private SitePage sitePage;

    @Column(name = "lemma", nullable = false)
    private String lemma;

    @Column(name = "frequency", nullable = false)
    private int frequency;

    @OneToMany(mappedBy = "lemma",
            fetch = FetchType.LAZY,
            cascade = javax.persistence.CascadeType.REMOVE,
            orphanRemoval = true)
    private List<Index> indexList;


}
