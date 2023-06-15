package searchengine.model;

import lombok.Data;

import javax.persistence.*;

import java.util.List;

import static javax.persistence.GenerationType.IDENTITY;

@Table(name = "page",
        indexes = {@javax.persistence.Index(columnList = "path", name = "path_index")})
@Entity(name = "page")
@Data
public class Page {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "page_sequence"
    )
    @SequenceGenerator(
            name = "page_sequence",
            allocationSize = 100
    )
    @Column(name = "id", nullable = false)
    private int id;

    @ManyToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "site_id", nullable = false, foreignKey = @ForeignKey(name = "site_id_fk"))
    private Site site;

    @Column(name = "path", columnDefinition = "VARCHAR(255)", nullable = false)
    private String path;

    @Column(name = "code", nullable = false)
    private int code;

    @Column(name = "content", columnDefinition = "MEDIUMTEXT")
    private String content;

    @OneToMany(mappedBy = "page",
            fetch = FetchType.LAZY,
            cascade = javax.persistence.CascadeType.REMOVE,
            orphanRemoval = true)
    private List<Index> indexList;

}
