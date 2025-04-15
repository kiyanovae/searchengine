package searchengine.dto.statistics;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DetailedStatisticsItem {
    private String url;
    private String name;
    private String status;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime statusTime;
    private String error;
    private int pages;
    private int lemmas;
}
