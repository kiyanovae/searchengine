package searchengine.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "site")
public class SiteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')", nullable = false)
    private SiteStatus status;

    @UpdateTimestamp
    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "url",columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;

    @Column(name = "name",columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;

    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "site")
    private List<LemmaEntity> lemmas;

    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "site")
    private List<PageEntity> pages;

    public SiteEntity(SiteStatus status, String url, String name) {
        this.status = status;
        statusTime = LocalDateTime.now();
        this.url = url;
        this.name = name;
    }
}
