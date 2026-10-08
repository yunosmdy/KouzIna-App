package domain;

import domain.enums.PaymentMethod;
import exception.ValidationException;
import util.Money;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Base class ng successful simulated payment para sa isang bill. */
public abstract class Payment implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String billId;
    private final PaymentMethod method;
    private final BigDecimal amountApplied;
    private final LocalDateTime paidAt;

    protected Payment(String id, String billId, PaymentMethod method,
                      BigDecimal amountApplied, LocalDateTime paidAt) {
        this.id = requireNonBlank(id, "Payment ID");
        this.billId = requireNonBlank(billId, "Bill ID");
        if (method == null) {
            throw new ValidationException("Payment method is required.");
        }
        this.method = method;
        this.amountApplied = Money.requireNonNegative(amountApplied, "Amount applied");
        if (paidAt == null) {
            throw new ValidationException("Payment time is required.");
        }
        this.paidAt = paidAt;
    }

    public String getId() {
        return id;
    }

    public String getBillId() {
        return billId;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public BigDecimal getAmountApplied() {
        return amountApplied;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    /** Sukli ng customer; laging zero para sa electronic payment. */
    public abstract BigDecimal getChange();

    protected static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required.");
        }
        return value.trim();
    }
}
