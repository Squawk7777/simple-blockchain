package casa.squawk7777.exceptions;

public class ChallengeExpiredException extends Exception {
    public ChallengeExpiredException(String message) {
        super(message);
    }

    public ChallengeExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
