package searchengine.dto;

import lombok.Data;

@Data
public class UnsuccessfulResponse {
    private final boolean result = false;
    private final String error;

    public UnsuccessfulResponse(String error) {
        this.error = error;
    }
}
