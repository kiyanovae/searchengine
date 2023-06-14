package searchengine.model;

import lombok.Data;

import javax.persistence.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static javax.persistence.GenerationType.AUTO;

@Table(name = "lemma")
@Entity(name = "lemma")
@Data
public class Lemma {

    @Id
    @GeneratedValue(strategy = AUTO)
    @Column(name = "id", nullable = false)
    private int id;

    @ManyToMany
    @JoinTable(name = "site_id",
            joinColumns = @JoinColumn(name = "site_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "lemma_id", referencedColumnName = "id"))
    private Set<Site> sites = new HashSet<>();
}
