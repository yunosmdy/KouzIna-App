package service;

import domain.AuditLog;
import domain.DiningSession;
import domain.Order;
import domain.Reservation;
import domain.RestaurantTable;
import domain.WaitlistEntry;
import domain.enums.Permission;
import domain.enums.ReservationStatus;
import domain.enums.WaitlistStatus;
import exception.ConflictException;
import exception.UnavailableException;
import exception.ValidationException;
import persistence.AppState;
import persistence.AppStateRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/** Dito yung check-in, walk-in, at pagpaupo ng nasa waitlist. */
public final class SeatingService {
    private final AppStateRepository repository;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public SeatingService(
            AppStateRepository repository, IdGenerator idGenerator, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "Repository is required.");
        this.idGenerator = Objects.requireNonNull(idGenerator, "ID generator is required.");
        this.clock = Objects.requireNonNull(clock, "Clock is required.");
    }

    /** Sabay dapat yung check-in, session, at draft order sa isang transaction. */
    public SeatingResult checkInReservation(
            String sessionToken, String reservationId, String assignedWaiterId) {
        return repository.transact(state -> {
            AuthorizationService.require(state, sessionToken, Permission.CHECK_IN_GUESTS);
            AuthorizationService.requireActiveWaiter(state, assignedWaiterId);

            Reservation reservation = state.getReservationOrThrow(reservationId);
            state.getCustomerOrThrow(reservation.getCustomerId());
            if (state.findSessionByReservation(reservation.getId()).isPresent()) {
                throw new ConflictException(
                        "Reservation " + reservation.getId() + " already has a dining session.");
            }
            String tableId = reservation.getTableId();
            if (tableId == null || tableId.isBlank()) {
                throw new ValidationException(
                        "A reservation must have an assigned table before check-in.");
            }
            RestaurantTable table = state.getTableOrThrow(tableId);
            requireAvailableFittingTable(state, table, reservation.getPartySize());

            LocalDateTime now = LocalDateTime.now(clock);
            String sessionId = OrderReferences.nextVisitId(state);
            String orderId = OrderReferences.nextOrderId(state);
            DiningSession session = new DiningSession(
                    sessionId,
                    reservation.getCustomerId(),
                    table.getId(),
                    assignedWaiterId,
                    orderId,
                    reservation.getId(),
                    null,
                    now);
            Order order = new Order(orderId, sessionId);

            reservation.checkIn();
            table.occupy();
            state.addSessionAndOrder(session, order);
            addAudit(
                    state,
                    sessionToken,
                    now,
                    "RESERVATION_CHECKED_IN",
                    "Checked in reservation " + reservation.getId()
                            + " at table " + table.getTableNumber() + ".");

            return seatedResult(session, null);
        });
    }

    /** Sa waitlist muna lahat ng walk-in, upo agad pag may pwede nang table. */
    public SeatingResult registerWalkIn(
            String sessionToken, String customerId, int partySize, String assignedWaiterId) {
        return repository.transact(state -> {
            AuthorizationService.require(state, sessionToken, Permission.MANAGE_WAITLIST);
            AuthorizationService.requireActiveWaiter(state, assignedWaiterId);
            state.getCustomerOrThrow(customerId);
            if (partySize <= 0 || state.getTables().stream().noneMatch(table -> table.canSeat(partySize))) {
                throw new ValidationException("Choose a party size that fits one of our tables.");
            }

            LocalDateTime now = LocalDateTime.now(clock);
            WaitlistEntry entry = new WaitlistEntry(
                    idGenerator.nextId(), customerId, partySize, now);
            state.addWaitlistEntry(entry);

            Optional<RestaurantTable> availableTable = state.getTables().stream()
                    .filter(table -> isAvailable(state, table))
                    .filter(table -> !hasUnfinishedBooking(state, table, now))
                    .filter(table -> table.canSeat(partySize))
                    .filter(table -> earliestCompatibleEntry(state, table) == entry)
                    .min((first, second) ->
                            Integer.compare(first.getTableNumber(), second.getTableNumber()));

            if (availableTable.isEmpty()) {
                addAudit(
                        state,
                        sessionToken,
                        now,
                        "WALK_IN_QUEUED",
                        "Queued walk-in " + entry.getId() + " for customer " + customerId + ".");
                return new SeatingResult(true, null, null, null, entry.getId());
            }

            RestaurantTable table = availableTable.orElseThrow();
            DiningSession session = seatWaitlistEntry(
                    state, entry, table, assignedWaiterId, now);
            addAudit(
                    state,
                    sessionToken,
                    now,
                    "WALK_IN_SEATED",
                    "Seated walk-in " + entry.getId()
                            + " at table " + table.getTableNumber() + ".");
            return seatedResult(session, entry.getId());
        });
    }

    /**
     * Unang group sa pila na kasya yung papaupuin sa available table.
     * Pag di kasya, same spot pa rin sila sa pila, di ililipat sa dulo.
     */
    public Optional<SeatingResult> seatNextCompatibleParty(
            String sessionToken, String tableId, String assignedWaiterId) {
        return repository.transact(state -> {
            AuthorizationService.require(state, sessionToken, Permission.CHECK_IN_GUESTS);
            AuthorizationService.requireActiveWaiter(state, assignedWaiterId);

            RestaurantTable table = state.getTableOrThrow(tableId);
            requireAvailableTable(state, table);
            LocalDateTime now = LocalDateTime.now(clock);
            if (hasUnfinishedBooking(state, table, now)) {
                throw new UnavailableException(
                        "Table " + table.getTableNumber()
                                + " has an active or upcoming reservation. Choose another table.");
            }

            WaitlistEntry selected = earliestCompatibleEntry(state, table);
            if (selected == null) {
                return Optional.empty();
            }
            state.getCustomerOrThrow(selected.getCustomerId());
            if (hasSessionForWaitlistEntry(state, selected.getId())) {
                throw new ConflictException(
                        "Waitlist entry " + selected.getId() + " already has a dining session.");
            }

            DiningSession session = seatWaitlistEntry(
                    state, selected, table, assignedWaiterId, now);
            addAudit(
                    state,
                    sessionToken,
                    now,
                    "WAITLIST_PARTY_SEATED",
                    "Seated waitlist entry " + selected.getId()
                            + " at table " + table.getTableNumber() + ".");
            return Optional.of(seatedResult(session, selected.getId()));
        });
    }

    private DiningSession seatWaitlistEntry(
            AppState state,
            WaitlistEntry entry,
            RestaurantTable table,
            String assignedWaiterId,
            LocalDateTime now) {
        requireAvailableFittingTable(state, table, entry.getPartySize());

        String sessionId = OrderReferences.nextVisitId(state);
        String orderId = OrderReferences.nextOrderId(state);
        DiningSession session = new DiningSession(
                sessionId,
                entry.getCustomerId(),
                table.getId(),
                assignedWaiterId,
                orderId,
                null,
                entry.getId(),
                now);
        Order order = new Order(orderId, sessionId);

        entry.seat();
        table.occupy();
        state.addSessionAndOrder(session, order);
        return session;
    }

    private WaitlistEntry earliestCompatibleEntry(AppState state, RestaurantTable table) {
        WaitlistEntry earliest = null;
        for (WaitlistEntry entry : state.getWaitlistEntries()) {
            if (entry.getStatus() != WaitlistStatus.WAITING
                    || !table.canSeat(entry.getPartySize())) {
                continue;
            }
            if (earliest == null || entry.getJoinedAt().isBefore(earliest.getJoinedAt())) {
                earliest = entry;
            }
        }
        return earliest;
    }

    private boolean hasSessionForWaitlistEntry(AppState state, String waitlistEntryId) {
        return state.getSessions().stream()
                .anyMatch(session -> session.getWaitlistEntryId()
                        .map(waitlistEntryId::equals)
                        .orElse(false));
    }

    private void requireAvailableFittingTable(
            AppState state, RestaurantTable table, int partySize) {
        if (!table.canSeat(partySize)) {
            throw new UnavailableException(
                    "Table " + table.getTableNumber() + " cannot seat a party of " + partySize + ".");
        }
        requireAvailableTable(state, table);
    }

    private void requireAvailableTable(AppState state, RestaurantTable table) {
        if (!isAvailable(state, table)) {
            throw new UnavailableException(
                    "Table " + table.getTableNumber() + " is not available.");
        }
    }

    private boolean isAvailable(AppState state, RestaurantTable table) {
        return table.isAvailable() && state.findOpenSessionByTable(table.getId()).isEmpty();
    }

    private boolean hasUnfinishedBooking(
            AppState state, RestaurantTable table, LocalDateTime now) {
        // walang estimated end yung walk-in, kaya check din yung upcoming reservations
        return state.getReservations().stream()
                .filter(reservation -> table.getId().equals(reservation.getTableId()))
                .filter(reservation -> reservation.getStatus() == ReservationStatus.CONFIRMED
                        || reservation.getStatus() == ReservationStatus.CHECKED_IN)
                .anyMatch(reservation -> reservation.getEndTime().isAfter(now));
    }

    private SeatingResult seatedResult(DiningSession session, String waitlistEntryId) {
        return new SeatingResult(
                false,
                session.getId(),
                session.getOrderId(),
                session.getTableId(),
                waitlistEntryId);
    }

    private void addAudit(
            AppState state,
            String sessionToken,
            LocalDateTime timestamp,
            String action,
            String detail) {
        state.addAuditLog(new AuditLog(
                idGenerator.nextId(), timestamp, AuthorizationService.employeeId(state, sessionToken), action, detail));
    }
}
