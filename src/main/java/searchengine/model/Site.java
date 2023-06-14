package searchengine.model;

import lombok.Data;

import javax.persistence.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


import static javax.persistence.GenerationType.AUTO;

@Table(name = "site")
@Entity(name = "site")
@Data
public class Site {
    @Id
    @GeneratedValue(strategy = AUTO)
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

    @OneToOne (optional=false, mappedBy="site")
    private Page page;

    @ManyToMany(mappedBy = "sites")
    private Set<Lemma> lemma = new HashSet<>();
}

