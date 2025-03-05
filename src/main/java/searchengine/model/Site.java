package searchengine.model;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;
@Data
@Table
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Enumerated(EnumType.STRING)
    private Status status;
    private LocalDateTime statusTime;
    private String lastError;
    private String url;
    private String name;

    private enum Status{
        INDEXING, INDEXED, FAILED
    }
}
