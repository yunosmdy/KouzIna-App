package service;

import domain.Reservation;
import domain.enums.ReservationStatus;

import java.time.LocalDateTime;
import java.util.Objects;

/** Reservation details para sa report, read-only lang to. */
public final class ReservationReportRow {
    private final String reservationId;
    private final String customerId;
    private final String tableId;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final int partySize;
    private final ReservationStatus status;

    public ReservationReportRow(Reservation reservation) {
        this(
                Objects.requireNonNull(reservation, "Reservation is required.").getId(),
                reservation.getCustomerId(),
                reservation.getTableId(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getPartySize(),
                reservation.getStatus());
    }

    public ReservationReportRow(
            String reservationId, String customerId, String tableId,
            LocalDateTime startTime, LocalDateTime endTime,
            int partySize, ReservationStatus status) {
        this.reservationId = requireText(reservationId, "Reservation ID");
        this.customerId = requireText(customerId, "Customer ID");
        this.tableId = tableId == null ? null : requireText(tableId, "Table ID");
        this.startTime = Objects.requireNonNull(startTime, "Reservation start time is required.");
        this.endTime = Objects.requireNonNull(endTime, "Reservation end time is required.");
        if (partySize <= 0) {
            throw new IllegalArgumentException("Party size must be positive.");
        }
        this.partySize = partySize;
        this.status = Objects.requireNonNull(status, "Reservation status is required.");
    }

    public String getReservationId() {
        return reservationId;
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

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }
}
