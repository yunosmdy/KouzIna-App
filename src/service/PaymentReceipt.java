package service;

import exception.ValidationException;
import util.Money;

import java.math.BigDecimal;

/** Result ng successful payment at table release, di na editable to. */
public record PaymentReceipt(
        String paymentId,
        String billId,
        BigDecimal amountApplied,
        BigDecimal change,
        String releasedTableId,
        String compatibleWaitlistEntryId) {

    public PaymentReceipt {
        paymentId = requireText(paymentId, "Payment ID");
        billId = requireText(billId, "Bill ID");
        amountApplied = Money.requireNonNegative(amountApplied, "Amount applied");
        change = Money.requireNonNegative(change, "Change");
        releasedTableId = requireText(releasedTableId, "Released table ID");
        if (compatibleWaitlistEntryId != null) {
            compatibleWaitlistEntryId = requireText(
                    compatibleWaitlistEntryId, "Compatible waitlist entry ID");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required.");
        }
        return value.trim();
    }
}
