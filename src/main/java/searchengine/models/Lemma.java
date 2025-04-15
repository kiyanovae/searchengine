package searchengine.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "lemma")
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "site_id",nullable = false)
    private Integer siteId;

    @Column(name = "lemma", nullable = false, unique = true)
    private String lemma;

    @Column(name = "frequency", nullable = false)
    private Integer frequency;

}
