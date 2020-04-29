package casa.squawk7777.exceptions;

public class InvalidBlockException extends Exception {
    public InvalidBlockException(String message) {
        super(message);
    }

    public InvalidBlockException(String message, Throwable cause) {
        super(message, cause);
    }
}
