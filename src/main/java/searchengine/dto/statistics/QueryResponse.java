package searchengine.dto.statistics;

import lombok.Data;

import java.util.LinkedHashSet;


@Data
public class QueryResponse {
    private boolean result;
    private int count;
    private LinkedHashSet<QueryResponseDataItems> data;
}
