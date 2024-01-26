package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "lemma", indexes = @Index(name = "lemma_siteId_index", columnList = "lemma, site_id", unique = true))
public class LemmaEntity {
    //TODO : почему 100?
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "lemma_generator")
    @TableGenerator(name = "lemma_generator", table = "table_identifier", allocationSize = 100)
    @Column(name = "id", nullable = false)
    private int id;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;

    @Column(name = "lemma", columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(name = "frequency", nullable = false)
    private int frequency;

    @ManyToMany(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinTable(name = "`index`", joinColumns = {@JoinColumn(name = "lemma_id")}, inverseJoinColumns = {@JoinColumn(name = "page_id")})
    private List<PageEntity> pages;

    public LemmaEntity(SiteEntity site, String lemma, int frequency) {
        this.site = site;
        this.lemma = lemma;
        this.frequency = frequency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LemmaEntity that = (LemmaEntity) o;
        return site.getId() == that.site.getId() && Objects.equals(lemma, that.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(site.getId(), lemma);
    }
}
