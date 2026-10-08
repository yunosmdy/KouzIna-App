package service;

import domain.MenuItem;
import domain.Payment;
import domain.Reservation;
import domain.enums.Permission;
import persistence.AppState;
import persistence.AppStateRepository;
import util.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Reports galing sa copy ng state, check muna kung may permission. */
public final class ReportService {
    private final AppStateRepository repository;

    public ReportService(AppStateRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Repository is required.");
    }

    /** Payments na natanggap sa selected local date lang kasama dito. */
    public DailySalesReport dailySales(String actorId, LocalDate date) {
        LocalDate requiredDate = Objects.requireNonNull(date, "Report date is required.");
        AppState state = authorizedSnapshot(actorId);

        int paymentCount = 0;
        BigDecimal total = Money.ZERO;
        for (Payment payment : state.getPayments()) {
            if (payment.getPaidAt().toLocalDate().equals(requiredDate)) {
                paymentCount++;
                total = Money.add(total, payment.getAmountApplied());
            }
        }
        return new DailySalesReport(requiredDate, paymentCount, total);
    }

    /** Reservations na start sa selected date, kahit ano pa yung status. */
    public List<ReservationReportRow> reservationsByDate(String actorId, LocalDate date) {
        LocalDate requiredDate = Objects.requireNonNull(date, "Report date is required.");
        AppState state = authorizedSnapshot(actorId);
        return state.getReservations().stream()
                .filter(reservation -> reservation.getStartTime().toLocalDate().equals(requiredDate))
                .sorted(Comparator.comparing(Reservation::getStartTime)
                        .thenComparing(Reservation::getId))
                .map(ReservationReportRow::new)
                .toList();
    }

    /** Lahat ng menu items kasama, kahit inactive or wala nang stock. */
    public List<StockReportRow> remainingStock(String actorId) {
        AppState state = authorizedSnapshot(actorId);
        return state.getMenuItems().stream()
                .sorted(Comparator.comparing(MenuItem::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(MenuItem::getName)
                        .thenComparing(MenuItem::getId))
                .map(StockReportRow::new)
                .toList();
    }

    private AppState authorizedSnapshot(String actorId) {
        AppState state = repository.snapshot();
        AuthorizationService.require(state, actorId, Permission.VIEW_REPORTS);
        return state;
    }
}
