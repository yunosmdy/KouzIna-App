package exception;

/** Error kapag may conflict sa existing data, layk overlapping reservations. */
public class ConflictException extends DomainException {
    public ConflictException(String message) {
        super(message);
    }
}
