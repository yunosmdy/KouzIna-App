package exception;

/** Error kapag valid ang resource pero hindi available para sa operation. */
public class UnavailableException extends DomainException {
    public UnavailableException(String message) {
        super(message);
    }
}
