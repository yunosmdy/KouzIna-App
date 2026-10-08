package service;

import domain.CashPayment;
import domain.ElectronicPayment;
import domain.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Dito pipili kung cash or electronic yung payment object para di halo sa billing. */
public final class PaymentFactory {
    public Payment createCash(
            String id,
            String billId,
            BigDecimal amountApplied,
            BigDecimal tendered,
            LocalDateTime paidAt) {
        return new CashPayment(id, billId, amountApplied, tendered, paidAt);
    }

    public Payment createElectronic(
            String id,
            String billId,
            BigDecimal amountApplied,
            String reference,
            LocalDateTime paidAt) {
        return new ElectronicPayment(id, billId, amountApplied, reference, paidAt);
    }
}
