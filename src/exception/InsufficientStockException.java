package exception;

/** Error kapag kulang ang stock para sa order. */
public class InsufficientStockException extends DomainException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
