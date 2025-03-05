package searchengine.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;
    @Column(name = "lemma", nullable = false, length = 255)
    private String lemma;
    @Column(name = "frequency", nullable = false)
    private int frequency;
}
