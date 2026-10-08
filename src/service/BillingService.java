package service;

import domain.AuditLog;
import domain.Bill;
import domain.DiningSession;
import domain.Order;
import domain.Payment;
import domain.Reservation;
import domain.RestaurantTable;
import domain.WaitlistEntry;
import domain.enums.OrderStatus;
import domain.enums.Permission;
import domain.enums.ReservationStatus;
import domain.enums.WaitlistStatus;
import exception.ConflictException;
import exception.InvalidTransitionException;
import exception.PaymentException;
import persistence.AppState;
import persistence.AppStateRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/** Dito yung bill, simulated payment, at pag-close ng dining visit. */
public final class BillingService {
    private final AppStateRepository repository;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final PaymentFactory paymentFactory;

    public BillingService(
            AppStateRepository repository, IdGenerator idGenerator, Clock clock) {
        this(repository, idGenerator, clock, new PaymentFactory());
    }

    public BillingService(
            AppStateRepository repository,
            IdGenerator idGenerator,
            Clock clock,
            PaymentFactory paymentFactory) {
        this.repository = Objects.requireNonNull(repository, "Repository is required.");
        this.idGenerator = Objects.requireNonNull(idGenerator, "ID generator is required.");
        this.clock = Objects.requireNonNull(clock, "Clock is required.");
        this.paymentFactory = Objects.requireNonNull(
                paymentFactory, "Payment factory is required.");
    }

    /** Save yung fixed charges pag served na yung order at open pa yung session. */
    public String issueBill(
            String actorId,
            String sessionId,
            BigDecimal discountRate,
            BigDecimal serviceCharge) {
        return repository.transact(state -> {
            AuthorizationService.require(state, actorId, Permission.ISSUE_BILLS);
            DiningSession session = state.getSessionOrThrow(sessionId);
            if (!session.isOpen()) {
                throw new InvalidTransitionException(
                        "A bill may be issued only for an open dining session.");
            }

            Order order = state.getOrderOrThrow(session.getOrderId());
            if (order.getStatus() != OrderStatus.SERVED) {
                throw new InvalidTransitionException(
                        "A bill may be issued only after the order is served.");
            }
            if (state.findBillBySession(session.getId()).isPresent()) {
                throw new ConflictException(
                        "Dining session " + session.getId() + " already has a bill.");
            }

            LocalDateTime now = LocalDateTime.now(clock);
            Bill bill = new Bill(
                    idGenerator.nextId(),
                    session.getId(),
                    order.getSubtotal(),
                    discountRate,
                    serviceCharge);
            state.addBill(bill);
            addAudit(
                    state,
                    actorId,
                    now,
                    "BILL_ISSUED",
                    "Issued bill " + bill.getId() + " for dining session "
                            + session.getId() + ".");
            return bill.getId();
        });
    }

    /** Itinatama ang discount / service charge ng bill na hindi pa bayad (retry kapag mali ang na-type). */
    public void changeBillCharges(
            String actorId,
            String billId,
            BigDecimal discountRate,
            BigDecimal serviceCharge) {
        repository.transact(state -> {
            AuthorizationService.require(state, actorId, Permission.ISSUE_BILLS);
            Bill bill = requirePayableBill(state, billId);
            BigDecimal oldTotal = bill.getGrandTotal();
            bill.changeCharges(discountRate, serviceCharge);
            addAudit(
                    state,
                    actorId,
                    LocalDateTime.now(clock),
                    "BILL_CHANGED",
                    "Changed bill " + bill.getId() + " total from " + oldTotal
                            + " to " + bill.getGrandTotal() + ".");
            return null;
        });
    }

    /** Bill total lang yung applied, yung sobra sa cash ibabalik as sukli. */
    public PaymentReceipt acceptCash(
            String actorId, String billId, BigDecimal tendered) {
        return repository.transact(state -> {
            AuthorizationService.require(state, actorId, Permission.ACCEPT_PAYMENTS);
            Bill bill = requirePayableBill(state, billId);
            LocalDateTime now = LocalDateTime.now(clock);
            Payment payment = paymentFactory.createCash(
                    idGenerator.nextId(),
                    bill.getId(),
                    bill.getGrandTotal(),
                    tendered,
                    now);
            return completePayment(state, actorId, bill, payment, now);
        });
    }

    /** Simulated e-payment to, dapat sakto sa bill total yung amount. */
    public PaymentReceipt acceptElectronic(
            String actorId, String billId, String reference) {
        return repository.transact(state -> {
            AuthorizationService.require(state, actorId, Permission.ACCEPT_PAYMENTS);
            Bill bill = requirePayableBill(state, billId);
            LocalDateTime now = LocalDateTime.now(clock);
            Payment payment = paymentFactory.createElectronic(
                    idGenerator.nextId(),
                    bill.getId(),
                    bill.getGrandTotal(),
                    reference,
                    now);
            return completePayment(state, actorId, bill, payment, now);
        });
    }

    private Bill requirePayableBill(AppState state, String billId) {
        Bill bill = state.getBillOrThrow(billId);
        if (bill.isPaid() || state.findPaymentByBill(bill.getId()).isPresent()) {
            throw new PaymentException("Bill " + bill.getId() + " has already been paid.");
        }

        DiningSession session = state.getSessionOrThrow(bill.getSessionId());
        if (!session.isOpen()) {
            throw new PaymentException(
                    "Payment requires an open dining session for the bill.");
        }
        return bill;
    }

    private PaymentReceipt completePayment(
            AppState state,
            String actorId,
            Bill bill,
            Payment payment,
            LocalDateTime now) {
        DiningSession session = state.getSessionOrThrow(bill.getSessionId());
        RestaurantTable table = state.getTableOrThrow(session.getTableId());

        state.addPayment(payment);
        bill.markPaid();
        completeCheckedInReservation(state, session);
        session.close(now);
        table.release();
        addAudit(
                state,
                actorId,
                now,
                "PAYMENT_ACCEPTED",
                "Accepted " + payment.getMethod() + " payment " + payment.getId()
                        + " for bill " + bill.getId() + ".");

        WaitlistEntry compatibleEntry = earliestCompatibleEntry(state, table);
        return new PaymentReceipt(
                payment.getId(),
                bill.getId(),
                payment.getAmountApplied(),
                payment.getChange(),
                table.getId(),
                compatibleEntry == null ? null : compatibleEntry.getId());
    }

    private void completeCheckedInReservation(AppState state, DiningSession session) {
        session.getReservationId().ifPresent(reservationId -> {
            Reservation reservation = state.getReservationOrThrow(reservationId);
            if (reservation.getStatus() == ReservationStatus.CHECKED_IN) {
                reservation.complete();
            }
        });
    }

    private WaitlistEntry earliestCompatibleEntry(
            AppState state, RestaurantTable releasedTable) {
        WaitlistEntry earliest = null;
        for (WaitlistEntry entry : state.getWaitlistEntries()) {
            if (entry.getStatus() != WaitlistStatus.WAITING
                    || !releasedTable.canSeat(entry.getPartySize())) {
                continue;
            }
            if (earliest == null || entry.getJoinedAt().isBefore(earliest.getJoinedAt())) {
                earliest = entry;
            }
        }
        return earliest;
    }

    private void addAudit(
            AppState state,
            String actorId,
            LocalDateTime timestamp,
            String action,
            String detail) {
        state.addAuditLog(new AuditLog(
                idGenerator.nextId(), timestamp, actorId, action, detail));
    }
}
