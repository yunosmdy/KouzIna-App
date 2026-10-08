package exception;

/** Error kapag bawal tanggapin yung payment. */
public class PaymentException extends DomainException {
    public PaymentException(String message) {
        super(message);
    }
}
