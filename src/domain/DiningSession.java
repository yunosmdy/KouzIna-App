package domain;

import domain.enums.SessionStatus;
import exception.InvalidTransitionException;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * One dining visit ng customer sa occupied table.
 * Puwede galing siya sa reservation o waitlist, pero not both dapat.
 * Sa service workflow, close kapag bayad na or voided na if cancelled ang order.
 */
public final class DiningSession implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String customerId;
    private final String tableId;
    private final String waiterId;
    private final String orderId;
    private final String reservationId;
    private final String waitlistEntryId;
    private final LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private SessionStatus status;

    public DiningSession(String id, String customerId, String tableId, String waiterId, String orderId,
                         String reservationId, String waitlistEntryId, LocalDateTime openedAt) {
        this.id = requireNonBlank(id, "Session ID");
        this.customerId = requireNonBlank(customerId, "Customer ID");
        this.tableId = requireNonBlank(tableId, "Table ID");
        this.waiterId = requireNonBlank(waiterId, "Waiter ID");
        this.orderId = requireNonBlank(orderId, "Order ID");
        this.reservationId = normalizeOptional(reservationId, "Reservation ID");
        this.waitlistEntryId = normalizeOptional(waitlistEntryId, "Waitlist entry ID");
        if (this.reservationId != null && this.waitlistEntryId != null) {
            throw new IllegalArgumentException("A dining session may have only one origin.");
        }
        if (openedAt == null) {
            throw new IllegalArgumentException("Opened time is required.");
        }
        this.openedAt = openedAt;
        this.status = SessionStatus.OPEN;
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

    public String getWaiterId() {
        return waiterId;
    }

    public String getOrderId() {
        return orderId;
    }

    public Optional<String> getReservationId() {
        return Optional.ofNullable(reservationId);
    }

    public Optional<String> getWaitlistEntryId() {
        return Optional.ofNullable(waitlistEntryId);
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public Optional<LocalDateTime> getClosedAt() {
        return Optional.ofNullable(closedAt);
    }

    public SessionStatus getStatus() {
        return status;
    }

    public boolean isOpen() {
        return status == SessionStatus.OPEN;
    }

    /** Kino-close ng billing workflow ang open session pagkatapos mabayaran. */
    public void close(LocalDateTime now) {
        end(now, SessionStatus.CLOSED, "closed");
    }

    /** Vino-void ng cancellation workflow ang open session kapag cancelled ang order. */
    public void voidSession(LocalDateTime now) {
        end(now, SessionStatus.VOIDED, "voided");
    }

    private void end(LocalDateTime now, SessionStatus targetStatus, String completedAction) {
        if (!isOpen()) {
            throw new InvalidTransitionException("Only an open dining session may be " + completedAction + ".");
        }
        if (now == null) {
            throw new IllegalArgumentException("Closing time is required.");
        }
        if (now.isBefore(openedAt)) {
            throw new IllegalArgumentException("Closing time cannot be before opening time.");
        }
        closedAt = now;
        status = targetStatus;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
        return value.trim();
    }
}
