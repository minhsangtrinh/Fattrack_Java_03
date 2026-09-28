package lab1.domain.exception;

/** Thrown when a wallet operation violates a domain rule. */
public final class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}