package domain;

import domain.enums.WaitlistStatus;
import exception.InvalidTransitionException;
import exception.ValidationException;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** Walk-in group na naghihintay ng table. */
public final class WaitlistEntry implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String customerId;
    private final int partySize;
    private final LocalDateTime joinedAt;
    private WaitlistStatus status;

    public WaitlistEntry(String id, String customerId, int partySize, LocalDateTime joinedAt) {
        this.id = requireNonBlank(id, "Waitlist entry ID");
        this.customerId = requireNonBlank(customerId, "Customer ID");
        if (partySize <= 0) {
            throw new ValidationException("Party size must be positive.");
        }
        if (joinedAt == null) {
            throw new ValidationException("Waitlist join time is required.");
        }
        this.partySize = partySize;
        this.joinedAt = joinedAt;
        this.status = WaitlistStatus.WAITING;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public int getPartySize() {
        return partySize;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public WaitlistStatus getStatus() {
        return status;
    }

    public void seat() {
        requireWaiting("seat");
        status = WaitlistStatus.SEATED;
    }

    public void cancel() {
        requireWaiting("cancel");
        status = WaitlistStatus.CANCELLED;
    }

    private void requireWaiting(String action) {
        if (status != WaitlistStatus.WAITING) {
            throw new InvalidTransitionException(
                    "Cannot " + action + " waitlist entry while status is " + status + ".");
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required.");
        }
        return value.trim();
    }
}
