package service;

import util.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** Summary ng payments for the day, pang reports lang at di na editable. */
public final class DailySalesReport {
    private final LocalDate date;
    private final int paymentCount;
    private final BigDecimal totalAmount;

    public DailySalesReport(LocalDate date, int paymentCount, BigDecimal totalAmount) {
        this.date = Objects.requireNonNull(date, "Report date is required.");
        if (paymentCount < 0) {
            throw new IllegalArgumentException("Payment count cannot be negative.");
        }
        this.paymentCount = paymentCount;
        this.totalAmount = Money.requireNonNegative(totalAmount, "Sales total");
    }

    public LocalDate getDate() {
        return date;
    }

    public int getPaymentCount() {
        return paymentCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

}
