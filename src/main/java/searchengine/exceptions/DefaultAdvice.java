package searchengine.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.UnsuccessfulResponse;

@ControllerAdvice
public class DefaultAdvice {
    @ExceptionHandler(RequestException.class)
    public ResponseEntity<UnsuccessfulResponse> requestException(RequestException e) {
        UnsuccessfulResponse response = new UnsuccessfulResponse(false, e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }
}
