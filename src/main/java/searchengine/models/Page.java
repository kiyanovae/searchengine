package searchengine.models;

import lombok.*;

import javax.persistence.*;
import javax.persistence.Index;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "page", indexes = {
        @Index(name = "idx_path", columnList = "path")
})
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "path", nullable = false, unique = true)
    private String path;

    @Column(name = "code")
    private Integer code;

    @Lob
    @Column(name = "content", unique = true)
    private String content;
}