package service;

import domain.AuditLog;
import domain.DiningSession;
import domain.Reservation;
import domain.RestaurantTable;
import domain.enums.Permission;
import domain.enums.ReservationStatus;
import exception.UnavailableException;
import exception.InvalidTransitionException;
import persistence.AppState;
import persistence.AppStateRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/** Dito yung reservation status at table assignment, lowest suitable table number muna. */
public final class ReservationService {
    private final AppStateRepository repository;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public ReservationService(
            AppStateRepository repository, IdGenerator idGenerator, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "Repository is required.");
        this.idGenerator = Objects.requireNonNull(idGenerator, "ID generator is required.");
        this.clock = Objects.requireNonNull(clock, "Clock is required.");
    }

    public String createReservation(
            String sessionToken, String customerId, LocalDateTime start,
            LocalDateTime end, int partySize) {
        return createReservation(sessionToken, customerId, start, end, partySize, false);
    }

    /** A booking either gets a table and saves, or leaves no incomplete record. */
    public String bookTable(String sessionToken, String customerId, LocalDateTime start,
            LocalDateTime end, int partySize) {
        return createReservation(sessionToken, customerId, start, end, partySize, true);
    }

    private String createReservation(String sessionToken, String customerId, LocalDateTime start,
            LocalDateTime end, int partySize, boolean confirmNow) {
        return repository.transact(state -> {
            AuthorizationService.require(state, sessionToken, Permission.MANAGE_RESERVATIONS);
            state.getCustomerOrThrow(customerId);
            if (start == null || start.isBefore(LocalDateTime.now(clock))) {
                throw new exception.ValidationException("Choose a reservation date and time in the future.");
            }
            if (partySize <= 0 || state.getTables().stream().noneMatch(table -> table.canSeat(partySize))) {
                throw new exception.ValidationException("Choose a party size that fits one of our tables.");
            }

            Reservation reservation = new Reservation(
                    idGenerator.nextId(), customerId, start, end, partySize);
            if (confirmNow) {
                RestaurantTable table = findRecommendedTable(state, reservation).orElseThrow(() ->
                        new UnavailableException("No table fits this booking. Choose another time or a smaller party."));
                reservation.confirm(table.getId());
            }
            state.addReservation(reservation);
            addAudit(state, sessionToken, "RESERVATION_CREATED",
                    "Created reservation " + reservation.getId()
                            + " for customer " + reservation.getCustomerId() + ".");
            return reservation.getId();
        });
    }

    public Optional<String> recommendTable(String sessionToken, String reservationId) {
        AppState state = repository.snapshot();
        AuthorizationService.require(state, sessionToken, Permission.MANAGE_RESERVATIONS);
        Reservation reservation = state.getReservationOrThrow(reservationId);
        return findRecommendedTable(state, reservation).map(RestaurantTable::getId);
    }

    public String confirmReservation(String sessionToken, String reservationId) {
        return repository.transact(state -> {
            AuthorizationService.require(state, sessionToken, Permission.MANAGE_RESERVATIONS);
            Reservation reservation = state.getReservationOrThrow(reservationId);
            RestaurantTable table = findRecommendedTable(state, reservation)
                    .orElseThrow(() -> new UnavailableException(
                            "No table is available for reservation " + reservation.getId() + "."));

            reservation.confirm(table.getId());
            addAudit(state, sessionToken, "RESERVATION_CONFIRMED",
                    "Confirmed reservation " + reservation.getId()
                            + " for table " + table.getId() + ".");
            return table.getId();
        });
    }

    public void cancelReservation(String sessionToken, String reservationId) {
        repository.transact(state -> {
            AuthorizationService.require(state, sessionToken, Permission.MANAGE_RESERVATIONS);
            Reservation reservation = state.getReservationOrThrow(reservationId);
            if (reservation.getStatus() == ReservationStatus.CHECKED_IN) {
                throw new InvalidTransitionException(
                        "This reservation is already checked in. Cancel its order in Ordering "
                                + "to close the visit; confirmed orders require a manager.");
            }
            reservation.cancel();
            addAudit(state, sessionToken, "RESERVATION_CANCELLED",
                    "Cancelled reservation " + reservation.getId() + ".");
            return null;
        });
    }

    public void markNoShow(String sessionToken, String reservationId) {
        repository.transact(state -> {
            AuthorizationService.require(state, sessionToken, Permission.MANAGE_RESERVATIONS);
            Reservation reservation = state.getReservationOrThrow(reservationId);
            reservation.markNoShow(LocalDateTime.now(clock));
            addAudit(state, sessionToken, "RESERVATION_NO_SHOW",
                    "Marked reservation " + reservation.getId() + " as no-show.");
            return null;
        });
    }

    private Optional<RestaurantTable> findRecommendedTable(
            AppState state, Reservation requestedReservation) {
        RestaurantTable recommendedTable = null;
        for (RestaurantTable table : state.getTables()) {
            if (!table.canSeat(requestedReservation.getPartySize())
                    || hasReservationConflict(state, table, requestedReservation)
                    || hasSessionConflict(state, table, requestedReservation)) {
                continue;
            }
            // lowest table number pa rin kahit iba yung ayos sa collection
            if (recommendedTable == null
                    || table.getTableNumber() < recommendedTable.getTableNumber()) {
                recommendedTable = table;
            }
        }
        return Optional.ofNullable(recommendedTable);
    }

    private boolean hasReservationConflict(
            AppState state, RestaurantTable table, Reservation requestedReservation) {
        return state.getReservations().stream()
                .filter(existing -> !existing.getId().equals(requestedReservation.getId()))
                .filter(existing -> existing.getStatus() == ReservationStatus.CONFIRMED
                        || existing.getStatus() == ReservationStatus.CHECKED_IN)
                .filter(existing -> table.getId().equals(existing.getTableId()))
                .anyMatch(existing -> existing.overlaps(requestedReservation));
    }

    private boolean hasSessionConflict(
            AppState state, RestaurantTable table, Reservation requestedReservation) {
        return state.getSessions().stream()
                .filter(DiningSession::isOpen)
                .filter(session -> table.getId().equals(session.getTableId()))
                .anyMatch(session -> session.getOpenedAt().isBefore(requestedReservation.getEndTime()));
    }

    private void addAudit(AppState state, String sessionToken, String action, String detail) {
        state.addAuditLog(new AuditLog(
                idGenerator.nextId(), LocalDateTime.now(clock), AuthorizationService.employeeId(state, sessionToken), action, detail));
    }
}
