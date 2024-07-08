package searchengine.model.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity(name = "Lemma")
@NoArgsConstructor
@Getter
@Setter
@Table(name = "lemma", indexes = @Index(name = "idx_lemma", columnList = "lemma"))
public class LemmaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;

    @Column(name = "lemma", columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Version
    @Column(name = "frequency", nullable = false)
    private int frequency;

    public LemmaEntity(SiteEntity site, String lemma, int frequency) {
        this.site = site;
        this.lemma = lemma;
        this.frequency = frequency;
    }
}
