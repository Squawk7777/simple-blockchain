package casa.squawk7777.exceptions;

public class CorruptedChainException extends Exception {
    public CorruptedChainException(String message) {
        super(message);
    }

    public CorruptedChainException(String message, Throwable cause) {
        super(message, cause);
    }
}
