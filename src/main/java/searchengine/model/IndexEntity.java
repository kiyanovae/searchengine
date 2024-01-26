package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "`index`")
public class IndexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "index_generator")
    @TableGenerator(name = "index_generator", table = "table_identifier", pkColumnName = "table_name", allocationSize = 1000)
    @Column(name = "id", nullable = false)
    private int id;

    @Column(name = "page_id", nullable = false)
    private int pageId;

    @Column(name = "lemma_id", nullable = false)
    private int lemmaId;

    @Column(name = "`rank`", nullable = false)
    private float rank;

    public IndexEntity(int pageId, float rank) {
        this.pageId = pageId;
        this.rank = rank;
    }

    public IndexEntity(int pageId, int lemmaId, float rank) {
        this.pageId = pageId;
        this.lemmaId = lemmaId;
        this.rank = rank;
    }
}
