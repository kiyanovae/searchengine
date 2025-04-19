package searchengine.dto.statistics;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseWithError {
    private boolean result;
    private String error;


    public ResponseWithError(boolean result, String error) {
        this.result = result;
        this.error = error;
    }

}
