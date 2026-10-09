package ui;

import bootstrap.*;
import domain.*;
import domain.enums.*;
import persistence.*;
import service.*;
import security.PasswordHasher;
import javax.swing.*;
import java.awt.Component;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Executable regression checks. Uses only temporary/in-memory data. */
public final class WorkflowChecks {
    static final String MANAGER = "employee-manager", WAITER = "employee-waiter";
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-05T04:00:00Z"), ZoneId.of("Asia/Manila"));
    static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    static int checks;
    static ApplicationServices app(AppState state) {
        return new ApplicationServices(new InMemoryAppStateRepository(state), new PasswordHasher(), new UuidIdGenerator(), CLOCK);
    }
    static final java.util.Map<String,String> TOKENS = new java.util.HashMap<>();
    static String token(ApplicationServices app, String id) {
        String key = app.repository().snapshot().getSecurityId() + id;
        return TOKENS.computeIfAbsent(key, ignored -> app.authentication().login(
                id.equals(MANAGER) ? "manager" : "waiter", (id.equals(MANAGER) ? "manager123" : "waiter123").toCharArray()).sessionToken());
    }
    static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
    }
    static void rejects(Runnable action, String message) {
        try { action.run(); } catch (RuntimeException expected) { checks++; return; }
        throw new AssertionError(message);
    }
    static void serve(ApplicationServices app, String id) {
        app.orders().addItem(token(app, WAITER), id, "menu-1", 2);
        app.orders().confirmOrder(token(app, WAITER), id);
        app.kitchen().markPreparing(token(app, MANAGER), id);
        app.kitchen().markReady(token(app, MANAGER), id);
        app.kitchen().markServed(token(app, WAITER), id);
    }
        static java.util.List<java.awt.Component> descendants(java.awt.Container root) {
        var result = new java.util.ArrayList<java.awt.Component>();
        for (var child : root.getComponents()) { result.add(child); if (child instanceof java.awt.Container container) result.addAll(descendants(container)); }
        return result;
    }
    static void click(java.awt.Container root, String text) {
        descendants(root).stream().filter(c -> c instanceof AbstractButton b && b.getText().equals(text))
                .map(c -> (AbstractButton) c).findFirst().orElseThrow().doClick();
    }

    /** Fixing mistakes: change an unpaid bill, take an order back from the kitchen, payment amounts. */
    static void retryChecks() {
        ApplicationServices app = app(TestData.create());
        SeatingResult visit = app.seating().registerWalkIn(token(app, WAITER), "customer-1", 2, WAITER);
        app.orders().addItem(token(app, WAITER), visit.orderId(), "menu-1", 2);
        int stockBefore = app.repository().snapshot().getMenuItemOrThrow("menu-1").getStockQuantity();
        app.orders().confirmOrder(token(app, WAITER), visit.orderId());
        check(app.repository().snapshot().getMenuItemOrThrow("menu-1").getStockQuantity() == stockBefore - 2, "Sending takes stock");
        rejects(() -> app.orders().recallOrder(token(app, WAITER), "missing"), "Unknown order cannot be taken back");
        app.orders().recallOrder(token(app, WAITER), visit.orderId());
        var recalled = app.repository().snapshot();
        check(recalled.getOrderOrThrow(visit.orderId()).getStatus() == OrderStatus.DRAFT, "Taken-back order is editable again");
        check(recalled.getMenuItemOrThrow("menu-1").getStockQuantity() == stockBefore, "Taking an order back returns the stock");
        rejects(() -> app.orders().recallOrder(token(app, WAITER), visit.orderId()), "An order still being taken cannot be taken back");
        app.orders().updateItemQuantity(token(app, WAITER), visit.orderId(), "menu-1", 1);
        app.orders().confirmOrder(token(app, WAITER), visit.orderId());
        app.kitchen().markPreparing(token(app, MANAGER), visit.orderId());
        rejects(() -> app.orders().recallOrder(token(app, WAITER), visit.orderId()), "Order already cooking cannot be taken back");
        app.kitchen().markReady(token(app, MANAGER), visit.orderId());
        app.kitchen().markServed(token(app, WAITER), visit.orderId());
        check(app.audit().getAuditLog(token(app, MANAGER)).stream().anyMatch(log -> log.getAction().equals("ORDER_RECALLED")), "Taking back is in the audit log");

        String bill = app.billing().issueBill(token(app, MANAGER), visit.sessionId(), new BigDecimal("0.50"), new BigDecimal("100.00"));
        check(app.repository().snapshot().getBillOrThrow(bill).getGrandTotal().compareTo(new BigDecimal("192.50")) == 0, "Wrong bill as typed");
        app.billing().changeBillCharges(token(app, MANAGER), bill, new BigDecimal("0.20"), BigDecimal.ZERO);
        check(app.repository().snapshot().getBillOrThrow(bill).getGrandTotal().compareTo(new BigDecimal("148.00")) == 0, "Unpaid bill can be corrected");
        rejects(() -> app.billing().changeBillCharges(token(app, MANAGER), bill, new BigDecimal("1.50"), BigDecimal.ZERO), "Discount over 100% rejected");
        rejects(() -> app.billing().changeBillCharges(token(app, MANAGER), bill, BigDecimal.ZERO, new BigDecimal("-1")), "Negative service charge rejected");
        check(app.repository().snapshot().getBillOrThrow(bill).getGrandTotal().compareTo(new BigDecimal("148.00")) == 0, "Rejected change leaves the bill as it was");
        rejects(() -> app.billing().changeBillCharges(token(app, WAITER), bill, BigDecimal.ZERO, BigDecimal.ZERO), "Waiter cannot change bills");
        rejects(() -> app.billing().acceptCash(token(app, MANAGER), bill, new BigDecimal("100")), "Short cash rejected and can be retried");
        check(!app.repository().snapshot().getBillOrThrow(bill).isPaid(), "Bill stays unpaid after a wrong amount");
        PaymentReceipt receipt = app.billing().acceptCash(token(app, MANAGER), bill, new BigDecimal("200"));
        check(receipt.change().compareTo(new BigDecimal("52.00")) == 0, "Retry with the right amount works");
        rejects(() -> app.billing().changeBillCharges(token(app, MANAGER), bill, BigDecimal.ZERO, BigDecimal.ZERO), "Paid bill cannot be changed");

        check(PaymentDialog.parse("1,000").compareTo(new BigDecimal("1000")) == 0, "Amount with comma accepted");
        check(PaymentDialog.parse("₱650.50").compareTo(new BigDecimal("650.50")) == 0, "Amount with peso sign accepted");
        check(PaymentDialog.parse("abc") == null && PaymentDialog.parse("-5") == null && PaymentDialog.parse("12.345") == null
                && PaymentDialog.parse(" ") == null, "Wrong amounts are caught before saving");
        check(PaymentDialog.quickAmounts(new BigDecimal("634.00")).stream().map(Choice::label).toList()
                .equals(java.util.List.of("Exact", "₱650", "₱700", "₱1,000", "₱2,000")), "Quick cash amounts cover the bill");
    }

    public static void main(String[] args) throws Exception {
        ApplicationServices app = app(TestData.create());
        SeatingResult first = app.seating().registerWalkIn(token(app, WAITER), "customer-1", 2, WAITER);
        check(!first.sessionId().equals(first.orderId()), "Visit and food order have independent IDs");
        check(first.orderId().equals("ORD-0001"), "Food orders use a short independent sequence");
        check(OrderReferences.display(app.repository().snapshot(), first.orderId()).equals("O-0001"), "Staff still see one short visit reference");
        check(app.repository().snapshot().getOrderOrThrow(first.orderId()).getSessionId().equals(first.sessionId()), "Order explicitly links to its visit");
        check(app.repository().snapshot().getSessionOrThrow(first.sessionId()).getOrderId().equals(first.orderId()), "Visit explicitly links to its order");
        check(first.tableId().equals("table-1"), "Deterministic table identity");
        rejects(() -> app.seating().seatNextCompatibleParty(token(app, WAITER), "1", WAITER), "Bare table number cannot bypass identity validation");
        rejects(() -> app.seating().seatNextCompatibleParty(token(app, WAITER), "missing", WAITER), "Unknown table rejected");
        rejects(() -> app.seating().seatNextCompatibleParty(token(app, WAITER), first.tableId(), WAITER), "Occupied table rejected");
        int waiting = app.repository().snapshot().getWaitlistEntries().size();
        rejects(() -> app.seating().registerWalkIn(token(app, WAITER), "customer-1", 100, WAITER), "Oversize party rejected");
        check(app.repository().snapshot().getWaitlistEntries().size() == waiting, "Invalid walk-in leaves no queue record");
        app.orders().addItem(token(app, WAITER), first.orderId(), "menu-1", 1);
        app.orders().updateItemQuantity(token(app, WAITER), first.orderId(), "menu-1", 2);
        check(app.repository().snapshot().getOrderOrThrow(first.orderId()).getItems().get(0).getQuantity() == 2, "Quantity update");
        app.orders().removeItem(token(app, WAITER), first.orderId(), "menu-1");
        rejects(() -> app.orders().confirmOrder(token(app, WAITER), first.orderId()), "Empty order rejected");
        serve(app, first.orderId());
        rejects(() -> app.orders().addItem(token(app, WAITER), first.orderId(), "menu-2", 1), "Served order cannot be edited");
        String bill = app.billing().issueBill(token(app, MANAGER), first.sessionId(), new BigDecimal("0.20"), new BigDecimal("10.00"));
        check(app.repository().snapshot().getBillOrThrow(bill).getGrandTotal().equals(new BigDecimal("306.00")), "20 percent discount and service charge");
        rejects(() -> app.billing().issueBill(token(app, MANAGER), first.sessionId(), BigDecimal.ZERO, BigDecimal.ZERO), "Duplicate bill rejected");
        rejects(() -> app.billing().acceptCash(token(app, MANAGER), bill, new BigDecimal("100")), "Insufficient cash rejected");
        check(!app.repository().snapshot().getTableOrThrow(first.tableId()).isAvailable(), "Failed payment does not release table");
        PaymentReceipt receipt = app.billing().acceptCash(token(app, MANAGER), bill, new BigDecimal("500"));
        check(receipt.change().equals(new BigDecimal("194.00")) && receipt.releasedTableId().equals(first.tableId()), "Correct change and table release");
        check(app.repository().snapshot().getTableOrThrow(first.tableId()).isAvailable(), "Paid table available");
        rejects(() -> app.billing().acceptCash(token(app, MANAGER), bill, new BigDecimal("500")), "Double payment rejected");
        SeatingResult second = app.seating().registerWalkIn(token(app, WAITER), "customer-2", 2, WAITER);
        check(second.orderId().equals("ORD-0002"), "Paid orders keep their IDs and numbering is not reused");
        check(OrderReferences.display(app.repository().snapshot(), second.orderId()).equals("O-0002") && second.tableId().equals(first.tableId()), "Reused table has new order reference");
        app.orders().addItem(token(app, WAITER), second.orderId(), "menu-1", 1);
        app.orders().confirmOrder(token(app, WAITER), second.orderId());
        rejects(() -> app.orders().cancelOrder(token(app, WAITER), second.orderId()), "Waiter cannot cancel sent order");
        app.orders().cancelOrder(token(app, MANAGER), second.orderId());
        check(app.repository().snapshot().getTableOrThrow(second.tableId()).isAvailable(), "Allowed cancellation frees correct table");
        check(app.repository().snapshot().getMenuItemOrThrow("menu-1").getStockQuantity() == 28, "Cancelled stock restored once");
        rejects(() -> app.orders().cancelOrder(token(app, MANAGER), second.orderId()), "Double cancellation rejected");

        ApplicationServices reservations = app(TestData.create());
        rejects(() -> reservations.reservations().bookTable(token(reservations, MANAGER), "customer-1", NOW.minusHours(1), NOW, 2), "Past date rejected");
        rejects(() -> reservations.reservations().bookTable(token(reservations, MANAGER), "customer-1", NOW.plusHours(1), NOW.plusHours(1), 2), "Invalid interval rejected");
        String booking = reservations.reservations().bookTable(token(reservations, MANAGER), "customer-1", NOW.plusHours(1), NOW.plusHours(3), 10);
        check(reservations.repository().snapshot().getReservationOrThrow(booking).getStatus() == ReservationStatus.CONFIRMED, "One-step booking confirmed");
        int count = reservations.repository().snapshot().getReservations().size();
        rejects(() -> reservations.reservations().bookTable(token(reservations, MANAGER), "customer-2", NOW.plusHours(2), NOW.plusHours(4), 10), "Conflicting booking rejected");
        check(reservations.repository().snapshot().getReservations().size() == count, "No partial booking saved");
        rejects(() -> reservations.seating().seatNextCompatibleParty(token(reservations, WAITER), "table-10", WAITER), "Reserved table excluded from waitlist seating");
        SeatingResult checkedIn = reservations.seating().checkInReservation(token(reservations, WAITER), booking, WAITER);
        check(checkedIn.orderId().equals("ORD-0001"), "Reservation check-in uses the short order sequence");
        check(!checkedIn.orderId().equals(checkedIn.sessionId()) && OrderReferences.display(reservations.repository().snapshot(), checkedIn.orderId()).equals("O-0001"), "Reservation has independent IDs and one staff reference");
        rejects(() -> reservations.seating().checkInReservation(token(reservations, WAITER), booking, WAITER), "Duplicate check-in rejected");
        serve(reservations, checkedIn.orderId());
        String reservedBill = reservations.billing().issueBill(token(reservations, MANAGER), checkedIn.sessionId(), BigDecimal.ZERO, BigDecimal.ZERO);
        reservations.billing().acceptElectronic(token(reservations, MANAGER), reservedBill, "TEST-123");
        check(reservations.repository().snapshot().getReservationOrThrow(booking).getStatus() == ReservationStatus.COMPLETED, "Paid booking completed");

        ApplicationServices queue = app(TestData.create());
        SeatingResult[] seated = new SeatingResult[10];
        for (int i = 0; i < 10; i++) seated[i] = queue.seating().registerWalkIn(token(queue, WAITER), "customer-1", 2, WAITER);
        SeatingResult waitingLarge = queue.seating().registerWalkIn(token(queue, WAITER), "customer-2", 4, WAITER);
        SeatingResult waitingSmall = queue.seating().registerWalkIn(token(queue, WAITER), "customer-3", 2, WAITER);
        check(waitingLarge.queued() && waitingSmall.queued(), "Full restaurant queues guests");
        queue.orders().cancelOrder(token(queue, WAITER), seated[0].orderId());
        SeatingResult next = queue.seating().seatNextCompatibleParty(token(queue, WAITER), seated[0].tableId(), WAITER).orElseThrow();
        check(next.orderId().equals("ORD-0011"), "Waitlist seating advances beyond cancelled orders");
        check(next.waitlistEntryId().equals(waitingSmall.waitlistEntryId()), "First fitting party seated, larger party keeps place");
        check(queue.repository().snapshot().getWaitlistEntryOrThrow(waitingLarge.waitlistEntryId()).getStatus() == WaitlistStatus.WAITING, "Nonfitting party retained");

        Path data = Files.createTempDirectory("kouzina-regression-").resolve("state.dat");
        FileAppStateRepository file = new FileAppStateRepository(data, () -> app.repository().snapshot());
        ApplicationServices reopened = new ApplicationServices(new FileAppStateRepository(data, SampleDataFactory::create), new PasswordHasher(), new UuidIdGenerator(), CLOCK);
        SeatingResult third = reopened.seating().registerWalkIn(token(reopened, WAITER), "customer-3", 2, WAITER);
        check(third.orderId().equals("ORD-0003"), "Internal order numbering continues after restart");
        check(OrderReferences.display(reopened.repository().snapshot(), third.orderId()).equals("O-0003"), "Short reference continues after restart");
        AppState independent = new AppState();
        independent.addSessionAndOrder(new DiningSession("V-0001", "customer-1", "table-1", WAITER, "ORD-9999", null, null, NOW), new Order("ORD-9999", "V-0001"));
        check(OrderReferences.nextVisitId(independent).equals("V-0002"), "Order numbering does not advance the visit sequence");
        check(OrderReferences.nextOrderId(independent).equals("ORD-10000"), "Short order numbering expands beyond four digits without wrapping");
                AppState oldShared = TestData.create();
        oldShared.addSessionAndOrder(new DiningSession("O-0042", "customer-1", "table-1", WAITER, "O-0042", null, null, NOW), new Order("O-0042", "O-0042"));
        oldShared.getTableOrThrow("table-1").occupy();
        ApplicationServices mixed = app(oldShared);
        check(OrderReferences.display(mixed.repository().snapshot(), "O-0042").equals("O-0042"), "Existing shared-ID references remain unchanged");
        serve(mixed, "O-0042");
        String oldBill = mixed.billing().issueBill(token(mixed, MANAGER), "O-0042", BigDecimal.ZERO, BigDecimal.ZERO);
        mixed.billing().acceptCash(token(mixed, MANAGER), oldBill, new BigDecimal("500"));
        SeatingResult afterOld = mixed.seating().registerWalkIn(token(mixed, WAITER), "customer-2", 2, WAITER);
        check(OrderReferences.display(mixed.repository().snapshot(), afterOld.orderId()).equals("O-0043"), "Mixed old and new IDs advance the same staff numbering");
        check(!afterOld.orderId().equals(afterOld.sessionId()), "New visit remains independent after legacy payment");
        check(OrderReferences.displayVisit(mixed.repository().snapshot(), afterOld.sessionId()).equals("O-0043"), "Reference is owned by the visit");
        rejects(() -> OrderReferences.display(mixed.repository().snapshot(), afterOld.sessionId()), "Visit IDs cannot be passed as order IDs");
        Path mixedFile = Files.createTempDirectory("kouzina-mixed-ids-").resolve("state.dat");
        new FileAppStateRepository(mixedFile, () -> mixed.repository().snapshot());
        var mixedReload = new FileAppStateRepository(mixedFile, SampleDataFactory::create).snapshot();
        check(OrderReferences.display(mixedReload, "O-0042").equals("O-0042") && OrderReferences.display(mixedReload, afterOld.orderId()).equals("O-0043"), "Old and new display references survive serialization together");
        if (args.length > 0) {
            ApplicationServices legacy = ApplicationServices.forDataFile(Path.of(args[0]));
            var snapshot = legacy.repository().snapshot();
            var refs = new java.util.HashSet<String>();
            for (Order order : snapshot.getOrders()) check(refs.add(OrderReferences.display(snapshot, order.getId())), "Legacy references are unique");
            check(!snapshot.getCustomers().isEmpty(), "Existing serialized save readable");
        }
        AccountChecks.run();
        retryChecks();
        SwingUtilities.invokeAndWait(() -> {
            Theme.apply();
            var model = UiSupport.readOnlyModel("Table");
            model.addRow(new Object[]{new Choice("table-1", "Table 1")});
            model.addRow(new Object[]{new Choice("table-2", "Table 2")});
            JTable table = new JTable(model); UiSupport.tablePane(table);
            table.getRowSorter().toggleSortOrder(0); table.getRowSorter().toggleSortOrder(0);
            table.setRowSelectionInterval(0, 0);
            check(UiSupport.selectedId(table, 0, "table").equals("table-2"), "Sorting keeps underlying table identity");
            DateTimePicker picker = new DateTimePicker(LocalDateTime.of(2028, 2, 29, 18, 10));
            check(picker.getDateTime().equals(LocalDateTime.of(2028, 2, 29, 18, 0)), "Leap day kept and time rounded to the half hour");
            picker.setDateTime(LocalDateTime.of(2028, 3, 1, 23, 59));
            check(picker.getDateTime().equals(LocalDateTime.of(2028, 3, 1, 23, 30)), "Late time stays on the same day");
            picker.setDateTime(LocalDateTime.of(2028, 3, 1, 0, 10));
            check(picker.getDateTime().equals(LocalDateTime.of(2028, 3, 1, 0, 0)), "Midnight selection applies");
            DatePicker date = new DatePicker(LocalDate.of(2026, 10, 9));
            date.setDate(LocalDate.of(2027, 1, 31));
            check(date.getDate().equals(LocalDate.of(2027, 1, 31)), "Report date picker keeps the chosen day");
            check(UiSupport.formatName("  juan  DELA cruz ").equals("Juan Dela Cruz"), "Names are capitalized");
            check(UiSupport.formatPhone("09175550101").equals("0917-555-0101"), "Phone numbers get dashes");

            AuthenticatedUser user = app.authentication().login("manager", "manager123".toCharArray());
            Navigator nav = new Navigator() {
                public void open(String screen) { }
                public void open(String screen, String orderId) { }
                public boolean canOpen(String screen) { return true; }
            };
            for (Refreshable screen : new Refreshable[]{new ReservationsPanel(app, () -> user, nav),
                    new OrderingPanel(app, () -> user, nav), new KitchenPanel(app, () -> user, nav),
                    new BillingPanel(app, () -> user, nav), new ManagerPanel(app, () -> user)}) screen.refreshData();
            DashboardPanel dashboard = new DashboardPanel(app, nav);
            dashboard.setUser(user);
            checks++;

            LoginPanel login = new LoginPanel(app, chosen -> { });
            check(descendants(login).stream().anyMatch(c -> c instanceof JButton b && b.getText().equals("Create account")), "Individual login offers registration");
            check(descendants(login).stream().noneMatch(c -> c instanceof JButton b && b.getText().equals("Switch profile")), "Shared profile navigation removed");

            JPanel big = new JPanel();
            JScrollPane page = UiSupport.scrollPage(big, 820, 600);
            page.setSize(600, 400); page.doLayout(); page.getViewport().doLayout();
            Component view = page.getViewport().getView();
            check(view.getWidth() >= 820 && view.getHeight() >= 600, "Small window: the screen keeps its size and scrolls");
            page.setSize(1200, 900); page.doLayout(); page.getViewport().doLayout();
            check(view.getWidth() == page.getViewport().getWidth() && view.getHeight() == page.getViewport().getHeight(), "Big window: the screen fills it");
        });
        System.out.println("PASS: " + checks + " workflow, persistence, validation, and UI checks");
    }
}
