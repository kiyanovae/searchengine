package searchengine.dto.statistics;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseWithoutError {
    private boolean result;
    public ResponseWithoutError(boolean result) {
        this.result = result;
    }

}
