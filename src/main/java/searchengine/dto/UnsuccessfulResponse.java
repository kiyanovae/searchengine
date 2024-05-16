package searchengine.dto;

import lombok.Getter;

@Getter
public class UnsuccessfulResponse {
    private final boolean result = false;
    private final String error;

    public UnsuccessfulResponse(String error) {
        this.error = error;
    }
}
