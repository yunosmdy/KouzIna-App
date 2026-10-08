package domain;

import domain.enums.ReservationStatus;
import exception.InvalidTransitionException;
import exception.ValidationException;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** Requested dining schedule ng customer at status ng reservation. */
public final class Reservation implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String customerId;
    private String tableId;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final int partySize;
    private ReservationStatus status;

    public Reservation(String id, String customerId, LocalDateTime startTime,
                       LocalDateTime endTime, int partySize) {
        this.id = requireNonBlank(id, "Reservation ID");
        this.customerId = requireNonBlank(customerId, "Customer ID");
        validateInterval(startTime, endTime);
        if (partySize <= 0) {
            throw new ValidationException("Party size must be positive.");
        }
        this.startTime = startTime;
        this.endTime = endTime;
        this.partySize = partySize;
        this.tableId = null;
        this.status = ReservationStatus.PENDING;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getTableId() {
        return tableId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public int getPartySize() {
        return partySize;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void confirm(String tableId) {
        requireStatus(ReservationStatus.PENDING, "confirm");
        this.tableId = requireNonBlank(tableId, "Table ID");
        status = ReservationStatus.CONFIRMED;
    }

    public void checkIn() {
        requireStatus(ReservationStatus.CONFIRMED, "check in");
        status = ReservationStatus.CHECKED_IN;
    }

    public void complete() {
        requireStatus(ReservationStatus.CHECKED_IN, "complete");
        status = ReservationStatus.COMPLETED;
    }

    /** Cancel active reservation, kasama kapag vinoid yung dining session */
    public void cancel() {
        if (status != ReservationStatus.PENDING
                && status != ReservationStatus.CONFIRMED
                && status != ReservationStatus.CHECKED_IN) {
            throw invalidTransition("cancel");
        }
        status = ReservationStatus.CANCELLED;
    }

    public void markNoShow(LocalDateTime now) {
        if (now == null) {
            throw new ValidationException("Current time is required.");
        }
        requireStatus(ReservationStatus.CONFIRMED, "mark as no-show");
        if (!now.isAfter(startTime)) {
            throw new InvalidTransitionException(
                    "Reservation cannot be marked as no-show until after its start time.");
        }
        status = ReservationStatus.NO_SHOW;
    }

    /**
     * May overlap kapag nagsasapawan ang oras.
     * Walang overlap kung magka connect lang end ng isa at start nung isa.
     */
    public boolean overlaps(LocalDateTime otherStart, LocalDateTime otherEnd) {
        validateInterval(otherStart, otherEnd);
        return startTime.isBefore(otherEnd) && otherStart.isBefore(endTime);
    }

    public boolean overlaps(Reservation other) {
        if (other == null) {
            throw new ValidationException("Other reservation is required.");
        }
        return overlaps(other.startTime, other.endTime);
    }

    private void requireStatus(ReservationStatus expected, String action) {
        if (status != expected) {
            throw invalidTransition(action);
        }
    }

    private InvalidTransitionException invalidTransition(String action) {
        return new InvalidTransitionException(
                "Cannot " + action + " reservation while status is " + status + ".");
    }

    private static void validateInterval(LocalDateTime start, LocalDateTime end) {
        if (start == null) {
            throw new ValidationException("Reservation start time is required.");
        }
        if (end == null) {
            throw new ValidationException("Reservation end time is required.");
        }
        if (!start.isBefore(end)) {
            throw new ValidationException("Reservation end time must be after start time.");
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required.");
        }
        return value.trim();
    }
}
