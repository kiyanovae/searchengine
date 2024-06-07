package searchengine.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.UnsuccessfulResponse;
import searchengine.exceptions.BadRequestException;
import searchengine.exceptions.ConflictRequestException;
import searchengine.exceptions.InternalServerException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(ConflictRequestException.class)
    public ResponseEntity<UnsuccessfulResponse> handleConflictRequestException(ConflictRequestException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new UnsuccessfulResponse(e.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<UnsuccessfulResponse> handleBadRequestException(BadRequestException e) {
        log.error(e.getMessage());
        return ResponseEntity.badRequest().body(new UnsuccessfulResponse(e.getMessage()));
    }

    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<UnsuccessfulResponse> handleInternalServerException(InternalServerException e) {
        log.error(e.getMessage(), e);
        return ResponseEntity.internalServerError().body(new UnsuccessfulResponse(e.getMessage()));
    }
}
