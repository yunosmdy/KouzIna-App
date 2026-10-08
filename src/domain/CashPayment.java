package domain;

import domain.enums.PaymentMethod;
import exception.PaymentException;
import util.Money;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Successful cash payment na naka-record yung cash at change. */
public final class CashPayment extends Payment {
    @Serial
    private static final long serialVersionUID = 1L;

    private final BigDecimal tendered;
    private final BigDecimal change;

    public CashPayment(String id, String billId, BigDecimal amountApplied,
                       BigDecimal tendered, LocalDateTime paidAt) {
        super(id, billId, PaymentMethod.CASH, amountApplied, paidAt);
        this.tendered = Money.requireNonNegative(tendered, "Cash tendered");
        if (this.tendered.compareTo(getAmountApplied()) < 0) {
            throw new PaymentException("Cash tendered must cover the amount applied.");
        }
        this.change = Money.subtract(this.tendered, getAmountApplied());
    }

    public BigDecimal getTendered() {
        return tendered;
    }

    @Override
    public BigDecimal getChange() {
        return change;
    }
}
