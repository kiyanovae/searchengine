package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
@Getter
@Setter
@Entity
@Table(name = "Site")

public class SiteTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')")
    private SiteStatus status;

    @Column(name = "status_time")
    private Date statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column( columnDefinition = "VARCHAR(255)")
    private String url;

    @Column( columnDefinition = "VARCHAR(255)")
    private String name;

    @OneToMany(mappedBy = "site",cascade = CascadeType.ALL)
    private List<Page> pages;

    @OneToMany(mappedBy = "site",cascade = CascadeType.ALL)
    private List<Lemma> lemmas;




}
