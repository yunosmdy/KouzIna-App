package exception;

/** Error kapag invalid ang input o domain value - basta validation to. */
public class ValidationException extends DomainException {
    public ValidationException(String message) {
        super(message);
    }
}
