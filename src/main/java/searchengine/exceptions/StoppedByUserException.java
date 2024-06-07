package searchengine.exceptions;

public class StoppedByUserException extends RuntimeException {
    public StoppedByUserException(String message) {
        super(message);
    }
}
