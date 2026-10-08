package domain;

import domain.enums.PaymentMethod;
import util.Money;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Successful simulated e-payment na may reference number. */
public final class ElectronicPayment extends Payment {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String reference;

    public ElectronicPayment(String id, String billId, BigDecimal amountApplied,
                             String reference, LocalDateTime paidAt) {
        super(id, billId, PaymentMethod.ELECTRONIC, amountApplied, paidAt);
        this.reference = requireNonBlank(reference, "Electronic payment reference");
    }

    public String getReference() {
        return reference;
    }

    @Override
    public BigDecimal getChange() {
        return Money.ZERO;
    }
}
