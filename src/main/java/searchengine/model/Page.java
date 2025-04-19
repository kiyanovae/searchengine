package searchengine.model;

import lombok.Data;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "Page", indexes = @Index(columnList = "path"))
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String path;


    @ManyToOne
    @JoinColumn(name = "site_id")
    private SiteTable site;

    private int code;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @OneToMany(mappedBy = "page",cascade = CascadeType.ALL)
    private List<IndexTable> indexes;
}
