package searchengine.model;

import lombok.Data;

import javax.persistence.*;

import java.time.LocalDateTime;
import java.util.List;





@Table(name = "site")
@Entity(name = "site")
@Data
public class SitePage {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "site_sequence"
    )
    @SequenceGenerator(
            name = "site_sequence"
    )
    @Column(name = "id", nullable = false)
    private int id;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "ENUM('INDEXING', 'INDEXED')")
    private Status status;

    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "url", nullable = false, columnDefinition = "VARCHAR(255)")
    private String url;

    @Column(name = "name", nullable = false, columnDefinition = "VARCHAR(255)")
    private String name;

    @OneToMany (mappedBy="sitePage", orphanRemoval = true)
    private List<Page> page;

    @OneToMany(mappedBy = "sitePage", orphanRemoval = true)
    private List<Lemma> lemma;



}

