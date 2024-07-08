package searchengine.model.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity(name = "Site")
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
    private volatile SiteStatus status;

    @CreationTimestamp
    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "url", columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;

    @Column(name = "name", columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;

    public SiteEntity(SiteStatus status, String url, String name) {
        this.status = status;
        this.url = url;
        this.name = name;
    }

    public enum SiteStatus {
        INDEXING,
        INDEXED,
        FAILED
    }
}
