package exception;

/** Error to kapag bawal yung paglipat mula sa current status papunta sa requested status. */
public class InvalidTransitionException extends DomainException {
    public InvalidTransitionException(String message) {
        super(message);
    }
}
