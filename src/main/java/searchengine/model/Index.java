package searchengine.model;

import lombok.Data;

import javax.persistence.*;

@Table(name = "`index`")
@Entity(name = "`index`")
@Data
public class Index {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "index_sequence"
    )
    @SequenceGenerator(
            name = "index_sequence",
            allocationSize = 100
    )
    private int id;

    @Column(name = "`rank`")
    private float rank;

    @ManyToOne
    @JoinColumn(name = "page_id", foreignKey = @ForeignKey(name = "page_index_fk"))
    private Page page;

    @ManyToOne
    @JoinColumn(name = "lemma_id", foreignKey = @ForeignKey(name = "lemma_index_fk"))
    private Lemma lemma;

}
