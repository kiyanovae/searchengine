package searchengine.dto.statistics;

import lombok.Data;

import java.util.LinkedHashSet;

@Data

public class QueryResponseDataItemsList {
    private LinkedHashSet<QueryResponseDataItems> queryResponsesList;
}
