package domain;

import domain.enums.BillStatus;
import exception.InvalidTransitionException;
import exception.ValidationException;
import util.Money;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Charges ng isang dining session.
 * Puwede pang itama ang discount at service charge habang UNPAID (kung nagkamali ng type);
 * pag PAID na, fixed na lahat. Status: one time from UNPAID to PAID.
 */
public final class Bill implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String diningSessionId;
    private final BigDecimal subtotal;
    private BigDecimal discountRate;
    private BigDecimal serviceCharge;
    private BigDecimal discountAmount;
    private BigDecimal grandTotal;
    private BillStatus status;

    public Bill(String id, String diningSessionId, BigDecimal subtotal,
                BigDecimal discountRate, BigDecimal serviceCharge) {
        this.id = requireNonBlank(id, "Bill ID");
        this.diningSessionId = requireNonBlank(diningSessionId, "Dining session ID");
        this.subtotal = Money.requireNonNegative(subtotal, "Subtotal");
        this.discountRate = requireValidDiscountRate(discountRate);
        this.serviceCharge = Money.requireNonNegative(serviceCharge, "Service charge");
        this.discountAmount = Money.multiply(this.subtotal, this.discountRate);
        this.grandTotal = Money.add(
                Money.subtract(this.subtotal, this.discountAmount),
                this.serviceCharge);
        this.status = BillStatus.UNPAID;
    }

    public String getId() {
        return id;
    }

    /** ID ng dining session na sakop ng bill. */
    public String getSessionId() {
        return diningSessionId;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public BigDecimal getServiceCharge() {
        return serviceCharge;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public BillStatus getStatus() {
        return status;
    }

    public boolean isPaid() {
        return status == BillStatus.PAID;
    }

    /** Itinatama ang discount at service charge; puwede lang habang hindi pa bayad. */
    public void changeCharges(BigDecimal newDiscountRate, BigDecimal newServiceCharge) {
        if (status != BillStatus.UNPAID) {
            throw new InvalidTransitionException("A paid bill can no longer be changed.");
        }
        BigDecimal rate = requireValidDiscountRate(newDiscountRate);
        BigDecimal charge = Money.requireNonNegative(newServiceCharge, "Service charge");
        discountRate = rate;
        serviceCharge = charge;
        discountAmount = Money.multiply(subtotal, rate);
        grandTotal = Money.add(Money.subtract(subtotal, discountAmount), charge);
    }

    /** Ginagawang PAID ng billing workflow matapos i-record ang successful payment. */
    public void markPaid() {
        if (status != BillStatus.UNPAID) {
            throw new InvalidTransitionException("Only an unpaid bill may be marked paid.");
        }
        status = BillStatus.PAID;
    }

    private static BigDecimal requireValidDiscountRate(BigDecimal discountRate) {
        if (discountRate == null) {
            throw new ValidationException("Discount rate is required.");
        }
        if (discountRate.compareTo(BigDecimal.ZERO) < 0
                || discountRate.compareTo(BigDecimal.ONE) > 0) {
            throw new ValidationException("Discount rate must be from 0.00 to 1.00.");
        }
        return discountRate;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required.");
        }
        return value.trim();
    }
}
