package searchengine.dto;

import lombok.*;

@Getter
public class UnsuccessfulResponse extends SuccessfulResponse {
    private final String error;
    public UnsuccessfulResponse(boolean result, String error) {
        super(result);
        this.error = error;
    }
}
