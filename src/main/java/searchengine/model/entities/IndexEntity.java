package searchengine.model.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity(name = "Index")
@NoArgsConstructor
@Getter
@Setter
@Table(name = "`index`")
public class IndexEntity {
    @TableGenerator(name = "index_generator", table = "table_identifier", allocationSize = 1000)
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "index_generator")
    @Column(name = "id", nullable = false)
    private int id;

    @Column(name = "page_id", nullable = false)
    private int pageId;

    @Column(name = "lemma_id", nullable = false)
    private int lemmaId;

    @Column(name = "`rank`", nullable = false)
    private int rank;

    public IndexEntity(int pageId, int lemmaId, int rank) {
        this.pageId = pageId;
        this.lemmaId = lemmaId;
        this.rank = rank;
    }
}
