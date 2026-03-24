package launchpath.userservice.co.exception;

public class InsufficientCreditsException extends RuntimeException {
    public InsufficientCreditsException(String message) {
        super(message);
    }
}