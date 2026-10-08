package exception;

/** Error kapag hindi naka auth ang employee, failed login or kulang na permission. */
public class AuthorizationException extends DomainException {
    public AuthorizationException(String message) {
        super(message);
    }
}
