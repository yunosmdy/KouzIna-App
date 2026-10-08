package util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

//laging two decimal places, HALF_UP rounding.
public final class Money {
    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);

    private Money() {
    }

    //rounding sa non-null, pwede rin negative sa calc
    public static BigDecimal normalize(BigDecimal amount) {
        return requireNonNull(amount, "amount").setScale(SCALE, ROUNDING_MODE);
    }

    //no negative
    public static BigDecimal requireNonNegative(BigDecimal amount, String fieldName) {
        BigDecimal normalized = normalize(requireNonNull(amount, fieldName));
        if (normalized.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative.");
        }
        return normalized;
    }

    //greater than 0 valikdation
    public static BigDecimal requirePositive(BigDecimal amount, String fieldName) {
        BigDecimal normalized = normalize(requireNonNull(amount, fieldName));
        if (normalized.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero.");
        }
        return normalized;
    }

    public static BigDecimal add(BigDecimal first, BigDecimal second) {
        return normalize(requireNonNull(first, "first").add(requireNonNull(second, "second")));
    }

    public static BigDecimal subtract(BigDecimal first, BigDecimal second) {
        return normalize(requireNonNull(first, "first").subtract(requireNonNull(second, "second")));
    }

    public static BigDecimal multiply(BigDecimal amount, BigDecimal multiplier) {
        return normalize(requireNonNull(amount, "amount").multiply(requireNonNull(multiplier, "multiplier")));
    }

    private static BigDecimal requireNonNull(BigDecimal amount, String fieldName) {
        Objects.requireNonNull(fieldName, "fieldName must not be null.");
        return Objects.requireNonNull(amount, fieldName + " must not be null.");
    }
}
