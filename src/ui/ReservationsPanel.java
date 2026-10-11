package ui;

import bootstrap.ApplicationServices;
import domain.Customer;
import domain.Reservation;
import domain.RestaurantTable;
import domain.Waiter;
import domain.WaitlistEntry;
import domain.enums.ReservationStatus;
import domain.enums.WaitlistStatus;
import persistence.AppState;
import service.AuthenticatedUser;
import service.OrderReferences;
import service.SeatingResult;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Step 1 — Guests & Tables: seat walk-ins, book tables, and keep the guest list.
 * After guests are seated, "Go to Orders" opens their order.
 */
public final class ReservationsPanel extends JPanel implements Refreshable {
    private static final int[] STAY_MINUTES = {30, 60, 90, 120, 150, 180, 240};
    private static final java.time.format.DateTimeFormatter UNTIL = java.time.format.DateTimeFormatter.ofPattern("h:mm a");

    private final ApplicationServices services;
    private final Supplier<AuthenticatedUser> currentUser;
    private final Navigator navigator;
    private final WorkflowBar workflow;

    private final DefaultTableModel customerModel = UiSupport.readOnlyModel("Guest", "Phone");
    private final DefaultTableModel reservationModel = UiSupport.readOnlyModel(
            "Booking", "Guest", "Table", "Arrival", "Until", "Party", "Status");
    private final DefaultTableModel waitlistModel = UiSupport.readOnlyModel("Guest", "Party", "Waiting since");
    private final DefaultTableModel tableModel = UiSupport.readOnlyModel("Table", "Seats", "Availability");
    private final JTable reservationTable = new JTable(reservationModel);
    private final JTable tables = new JTable(tableModel);

    private final ChoiceBox walkInGuest = new ChoiceBox();
    private final ChoiceBox walkInParty = new ChoiceBox();
    private final ChoiceBox walkInWaiter = new ChoiceBox();
    private final ChoiceBox bookingGuest = new ChoiceBox();
    private final ChoiceBox bookingParty = new ChoiceBox();
    private final ChoiceBox bookingWaiter = new ChoiceBox();
    private final ChoiceBox stay = new ChoiceBox();
    private final DateTimePicker arrival = new DateTimePicker(LocalDate.now().plusDays(1).atTime(18, 0));

    private final JTextField firstName = new JTextField(14);
    private final JTextField lastName = new JTextField(14);
    private final JTextField phone = new JTextField(14);

    private final JButton seatWalkIn = new JButton("Seat walk-in / Join waiting list");
    private final JButton seatNext = new JButton("Seat next waiting party here");
    private final JButton book = new JButton("Book table");
    private final JButton checkIn = new JButton("Seat booked guests");
    private final JButton confirm = Theme.subtle(new JButton("Assign table"));
    private final JButton cancel = Theme.outline(new JButton("Cancel booking"));
    private final JButton noShow = Theme.outline(new JButton("Mark no-show"));
    private final JCheckBox history = new JCheckBox("Show past bookings");
    private final JLabel bookingHint = UiSupport.hint();
    private final JLabel tableHint = UiSupport.hint();
    private final JTabbedPane tabs = new JTabbedPane();
    private AppState state;
    /** Order of the party seated last, so "Go to Orders" opens it. */
    private String lastSeatedOrder;

    public ReservationsPanel(ApplicationServices services, Supplier<AuthenticatedUser> currentUser, Navigator navigator) {
        this.services = services;
        this.currentUser = currentUser;
        this.navigator = navigator;
        this.workflow = new WorkflowBar(navigator, KouzinaFrame.RESERVATIONS, () -> lastSeatedOrder);
        setOpaque(false);
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(UiSupport.heading("Guests & Tables",
                "Choose a guest, seat the party, then go to Orders to add their food.",
                UiSupport.refreshButton(this::refreshData)), BorderLayout.NORTH);
        top.add(workflow, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        tabs.addTab("Walk-ins & Tables", walkInTab());
        tabs.addTab("Reservations", reservationTab());
        tabs.addTab("Guest list", guestTab());
        add(tabs, BorderLayout.CENTER);

        List<Choice> stays = new ArrayList<>();
        for (int minutes : STAY_MINUTES) {
            stays.add(new Choice(String.valueOf(minutes), stayLabel(minutes)));
        }
        stay.setChoices(stays);
        stay.selectId("120");

        reservationTable.getSelectionModel().addListSelectionListener(e -> updateActions());
        tables.getSelectionModel().addListSelectionListener(e -> updateActions());
        history.setOpaque(false);
        history.addActionListener(e -> refreshData());
    }

    // ---------- Tabs ----------

    private JComponent walkInTab() {
        FormCard form = new FormCard("Seat a walk-in");
        form.field("Guest", guestRow(walkInGuest));
        form.field("Party size", walkInParty);
        form.field("Assigned waiter", walkInWaiter);
        form.buttons(seatWalkIn);
        form.note("Arrival time is recorded automatically. A free table that fits is assigned;"
                + " if none is free, the party joins the waiting list.");
        seatWalkIn.addActionListener(e -> UiSupport.perform(this, () -> {
            SeatingResult result = services.seating().registerWalkIn(actor(), walkInGuest.requireId("guest"),
                    walkInParty.requireInt("party size"), walkInWaiter.requireId("waiter"));
            refreshData();
            if (result.queued()) {
                UiSupport.showSuccess(this, "No free table fits right now, so the party joined the waiting list.\n"
                        + "When a table frees up, select it and click \"Seat next waiting party here\".");
            } else {
                showSeated(result);
            }
        }, () -> { }));

        JPanel tableSide = new JPanel(new BorderLayout(0, 8));
        tableSide.setOpaque(false);
        tableSide.add(UiSupport.card("Tables", UiSupport.tablePane(tables)), BorderLayout.CENTER);
        JPanel tableActions = new JPanel(new BorderLayout(0, 4));
        tableActions.setOpaque(false);
        JPanel seatRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        seatRow.setOpaque(false);
        seatRow.add(seatNext);
        tableActions.add(seatRow, BorderLayout.NORTH);
        tableActions.add(tableHint, BorderLayout.CENTER);
        tableSide.add(tableActions, BorderLayout.SOUTH);
        seatNext.addActionListener(e -> UiSupport.perform(this, () -> {
            var result = services.seating().seatNextCompatibleParty(actor(),
                    UiSupport.selectedId(tables, 0, "table"), walkInWaiter.requireId("waiter"));
            refreshData();
            if (result.isPresent()) {
                showSeated(result.get());
            } else {
                UiSupport.showSuccess(this, "No waiting party fits this table.");
            }
        }, () -> { }));

        JTable waitlist = new JTable(waitlistModel);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableSide,
                UiSupport.card("Waiting list  ·  first party that fits is seated first",
                        UiSupport.tablePane(waitlist)));
        UiSupport.columnWidths(tables, 110, 70, 130);
        UiSupport.columnWidths(waitlist, 150, 70, 140);
        split.setResizeWeight(0.55);
        split.setOpaque(false);
        split.setBorder(BorderFactory.createEmptyBorder());
        // Side by side on wide screens, stacked on small laptop screens
        split.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent event) {
                int orientation = split.getWidth() < 720 ? JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT;
                if (split.getOrientation() != orientation) {
                    split.setOrientation(orientation);
                    split.setResizeWeight(orientation == JSplitPane.VERTICAL_SPLIT ? 0.65 : 0.55);
                    split.resetToPreferredSizes();
                }
            }
        });
        return withForm(form, split);
    }

    private JComponent reservationTab() {
        FormCard form = new FormCard("Book a table");
        form.field("Guest", guestRow(bookingGuest));
        form.field("Arrival", arrival);
        form.field("Party size", bookingParty);
        form.field("Stay", stay);
        form.buttons(book);
        form.note("The booking is confirmed with a table right away. Times in the past are not allowed.");
        book.addActionListener(e -> UiSupport.perform(this, () -> {
            LocalDateTime start = arrival.getDateTime();
            String id = services.reservations().bookTable(actor(), bookingGuest.requireId("guest"),
                    start, start.plusMinutes(stay.requireInt("stay")), bookingParty.requireInt("party size"));
            refreshData();
            UiSupport.restoreSelection(reservationTable, id);
            UiSupport.showSuccess(this, "Booking confirmed for "
                    + UiSupport.tableName(state, state.getReservationOrThrow(id).getTableId())
                    + ".\nWhen the guests arrive, select the booking and click \"Seat booked guests\".");
        }, () -> { }));

        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.setOpaque(false);
        right.add(UiSupport.card("Bookings", UiSupport.tablePane(reservationTable)), BorderLayout.CENTER);
        UiSupport.columnWidths(reservationTable, 85, 140, 85, 135, 85, 65, 115);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row1.setOpaque(false);
        JLabel waiterLabel = new JLabel("Assigned waiter");
        waiterLabel.setFont(Theme.font(Font.BOLD, 12.5f));
        row1.add(waiterLabel);
        bookingWaiter.setPrototypeDisplayValue(new Choice("", "Mmmmmmmmmmmmmm"));
        row1.add(bookingWaiter);
        row1.add(checkIn);
        row1.add(confirm);
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row2.setOpaque(false);
        row2.add(cancel);
        row2.add(noShow);
        row2.add(history);
        actions.add(row1);
        actions.add(Box.createVerticalStrut(6));
        actions.add(row2);
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row3.setOpaque(false);
        row3.add(bookingHint);
        actions.add(Box.createVerticalStrut(4));
        actions.add(row3);
        right.add(actions, BorderLayout.SOUTH);

        checkIn.addActionListener(e -> UiSupport.perform(this, () -> {
            SeatingResult result = services.seating().checkInReservation(
                    actor(), bookingId(), bookingWaiter.requireId("waiter"));
            refreshData();
            showSeated(result);
        }, () -> { }));
        confirm.addActionListener(e -> UiSupport.perform(this,
                () -> services.reservations().confirmReservation(actor(), bookingId()), this::refreshData));
        cancel.addActionListener(e -> {
            if (UiSupport.confirm(this, "Cancel the selected booking?")) {
                UiSupport.perform(this, () -> services.reservations().cancelReservation(actor(), bookingId()),
                        this::refreshData);
            }
        });
        noShow.addActionListener(e -> {
            if (UiSupport.confirm(this, "Mark the selected guests as a no-show?")) {
                UiSupport.perform(this, () -> services.reservations().markNoShow(actor(), bookingId()),
                        this::refreshData);
            }
        });
        return withForm(form, right);
    }

    private JComponent guestTab() {
        FormCard form = new FormCard("Register a guest");
        UiSupport.placeholder(firstName, "e.g. Juan");
        UiSupport.placeholder(lastName, "e.g. Dela Cruz");
        UiSupport.placeholder(phone, "e.g. 0917 555 0101");
        UiSupport.formatOnLeave(firstName, UiSupport::formatName);
        UiSupport.formatOnLeave(lastName, UiSupport::formatName);
        UiSupport.formatOnLeave(phone, UiSupport::formatPhone);
        form.field("First name", firstName);
        form.field("Last name", lastName);
        form.field("Phone", phone);
        JButton register = new JButton("Register guest");
        form.buttons(register);
        form.note("Names get capital letters and phone numbers get dashes automatically."
                + " Saved guests can be picked by name when seating or booking.");
        register.addActionListener(e -> UiSupport.perform(this, () -> {
            String id = services.customers().createCustomer(actor(), UiSupport.formatName(firstName.getText()),
                    UiSupport.formatName(lastName.getText()), UiSupport.formatPhone(phone.getText()));
            refreshData();
            walkInGuest.selectId(id);
            bookingGuest.selectId(id);
        }, () -> {
            UiSupport.showSuccess(this, firstName.getText() + " " + lastName.getText()
                    + " was added. They are now selected on the walk-in and booking forms.");
            firstName.setText("");
            lastName.setText("");
            phone.setText("");
        }));
        return withForm(form, UiSupport.card("Saved guests", UiSupport.tablePane(new JTable(customerModel))));
    }

    /** Form card on the left, the rest on the right. */
    private static JComponent withForm(FormCard form, JComponent right) {
        JPanel panel = new JPanel(new BorderLayout(14, 0));
        panel.setOpaque(false);
        panel.add(form.inColumn(), BorderLayout.WEST);
        panel.add(right, BorderLayout.CENTER);
        return panel;
    }

    /** Guest dropdown with a "+ New" button next to it. */
    private JComponent guestRow(ChoiceBox combo) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        combo.setPrototypeDisplayValue(new Choice("", "Mmmmmmmmmmmm"));
        row.add(combo, BorderLayout.CENTER);
        JButton add = Theme.subtle(new JButton("New"));
        add.setIcon(Icons.icon("plus", 14, Theme.TEXT));
        add.setIconTextGap(4);
        add.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 12));
        add.setToolTipText("Register a new guest");
        add.addActionListener(e -> addGuestDialog());
        row.add(add, BorderLayout.EAST);
        return row;
    }

    private void addGuestDialog() {
        JTextField first = new JTextField(18);
        JTextField last = new JTextField(18);
        JTextField number = new JTextField(18);
        UiSupport.formatOnLeave(first, UiSupport::formatName);
        UiSupport.formatOnLeave(last, UiSupport::formatName);
        UiSupport.formatOnLeave(number, UiSupport::formatPhone);
        JPanel form = new JPanel(new GridBagLayout());
        String[] labels = {"First name", "Last name", "Phone"};
        JTextField[] fields = {first, last, number};
        for (int row = 0; row < labels.length; row++) {
            form.add(new JLabel(labels[row]), UiSupport.fieldConstraints(0, row));
            form.add(fields[row], UiSupport.fieldConstraints(1, row));
        }
        while (JOptionPane.showConfirmDialog(this, form, "New guest", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                String id = services.customers().createCustomer(actor(), UiSupport.formatName(first.getText()),
                        UiSupport.formatName(last.getText()), UiSupport.formatPhone(number.getText()));
                refreshData();
                walkInGuest.selectId(id);
                bookingGuest.selectId(id);
                return;
            } catch (RuntimeException error) {
                JOptionPane.showMessageDialog(this, error.getMessage(), "Check guest details",
                        JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void showSeated(SeatingResult result) {
        lastSeatedOrder = result.orderId();
        String message = UiSupport.tableName(state, result.tableId()) + " is seated. Order "
                + OrderReferences.display(state, result.orderId()) + " is ready for food items.";
        int choice = JOptionPane.showOptionDialog(this, message, "Guests seated", JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, new String[]{"Go to Orders", "Stay here"}, "Go to Orders");
        if (choice == 0) {
            navigator.open(KouzinaFrame.ORDERING, result.orderId());
        }
    }

    // ---------- Data ----------

    @Override
    public void refreshData() {
        String booking = UiSupport.selectedOrNull(reservationTable);
        String table = UiSupport.selectedOrNull(tables);
        state = services.repository().snapshot();
        workflow.refreshAccess();

        List<Choice> guests = new ArrayList<>();
        state.getCustomers().stream().sorted(Comparator.comparing(Customer::getFullName))
                .forEach(c -> guests.add(new Choice(c.getId(), c.getFullName() + "  |  " + c.getPhoneNumber())));
        walkInGuest.setChoices(guests);
        bookingGuest.setChoices(guests);

        List<Choice> waiters = new ArrayList<>();
        state.getEmployees().stream().filter(e -> e.isActive() && e instanceof Waiter)
                .sorted(Comparator.comparing(domain.Employee::getName))
                .forEach(e -> waiters.add(new Choice(e.getId(), e.getName())));
        walkInWaiter.setChoices(waiters);
        bookingWaiter.setChoices(waiters);

        int largest = state.getTables().stream().mapToInt(RestaurantTable::getCapacity).max().orElse(1);
        boolean firstFill = walkInParty.getItemCount() == 0;
        walkInParty.numbers(1, largest);
        bookingParty.numbers(1, largest);
        if (firstFill) {
            walkInParty.selectId("2");
            bookingParty.selectId("2");
        }

        customerModel.setRowCount(0);
        state.getCustomers().stream().sorted(Comparator.comparing(Customer::getFullName))
                .forEach(c -> customerModel.addRow(new Object[]{c.getFullName(), c.getPhoneNumber()}));

        reservationModel.setRowCount(0);
        int index = 0;
        for (Reservation r : state.getReservations()) {
            index++;
            boolean past = r.getStatus() == ReservationStatus.CANCELLED
                    || r.getStatus() == ReservationStatus.COMPLETED || r.getStatus() == ReservationStatus.NO_SHOW;
            if (past && !history.isSelected()) {
                continue;
            }
            reservationModel.addRow(new Object[]{new Choice(r.getId(), String.format("R-%04d", index)),
                    state.getCustomerOrThrow(r.getCustomerId()).getFullName(),
                    UiSupport.tableName(state, r.getTableId()),
                    r.getStartTime(), r.getEndTime().format(UNTIL), r.getPartySize(), UiSupport.status(r.getStatus())});
        }

        waitlistModel.setRowCount(0);
        state.getWaitlistEntries().stream().filter(e -> e.getStatus() == WaitlistStatus.WAITING)
                .sorted(Comparator.comparing(WaitlistEntry::getJoinedAt))
                .forEach(e -> waitlistModel.addRow(new Object[]{
                        state.getCustomerOrThrow(e.getCustomerId()).getFullName(), e.getPartySize(), e.getJoinedAt()}));

        tableModel.setRowCount(0);
        state.getTables().stream().sorted(Comparator.comparingInt(RestaurantTable::getTableNumber))
                .forEach(t -> tableModel.addRow(new Object[]{
                        new Choice(t.getId(), UiSupport.tableName(state, t.getId())), t.getCapacity(), availability(t)}));

        UiSupport.restoreSelection(reservationTable, booking);
        UiSupport.restoreSelection(tables, table);
        updateActions();
    }

    /** "Available", "Occupied", or "Reserved" (has a booking that has not ended yet). */
    private String availability(RestaurantTable table) {
        if (!table.isAvailable() || state.findOpenSessionByTable(table.getId()).isPresent()) {
            return "Occupied";
        }
        boolean booked = state.getReservations().stream().anyMatch(r -> table.getId().equals(r.getTableId())
                && (r.getStatus() == ReservationStatus.CONFIRMED || r.getStatus() == ReservationStatus.CHECKED_IN)
                && r.getEndTime().isAfter(LocalDateTime.now()));
        return booked ? "Reserved" : "Available";
    }

    private void updateActions() {
        if (state == null) {
            return;
        }
        String id = UiSupport.selectedOrNull(reservationTable);
        Reservation r = id == null ? null : state.getReservationOrThrow(id);
        confirm.setEnabled(r != null && r.getStatus() == ReservationStatus.PENDING);
        checkIn.setEnabled(r != null && r.getStatus() == ReservationStatus.CONFIRMED);
        cancel.setEnabled(r != null && (r.getStatus() == ReservationStatus.PENDING
                || r.getStatus() == ReservationStatus.CONFIRMED));
        noShow.setEnabled(r != null && r.getStatus() == ReservationStatus.CONFIRMED
                && LocalDateTime.now().isAfter(r.getStartTime()));
        UiSupport.setHint(bookingHint, r == null ? "Select a booking to see what you can do."
                : r.getStatus() == ReservationStatus.CHECKED_IN ? "Guests are seated. Continue in Orders."
                : "Only the actions that fit this booking are enabled.");

        String table = UiSupport.selectedOrNull(tables);
        boolean free = table != null && availability(state.getTableOrThrow(table)).equals("Available");
        boolean fitting = table != null && state.getWaitlistEntries().stream()
                .anyMatch(e -> e.getStatus() == WaitlistStatus.WAITING
                        && state.getTableOrThrow(table).canSeat(e.getPartySize()));
        seatNext.setEnabled(free && fitting);
        UiSupport.setHint(tableHint, table == null
                ? "Select a table. Tables free up automatically after payment."
                : !free ? "This table is in use or reserved. Walk-ins cannot use reserved tables."
                : !fitting ? "This table is free. No one on the waiting list fits it."
                : "Ready: the first waiting party that fits will be seated here.");
    }

    private static String stayLabel(int minutes) {
        int hours = minutes / 60;
        int rest = minutes % 60;
        if (hours == 0) {
            return rest + " minutes";
        }
        String text = hours + (hours == 1 ? " hour" : " hours");
        return rest == 0 ? text : text + " " + rest + " min";
    }

    private String actor() {
        return currentUser.get().sessionToken();
    }

    private String bookingId() {
        return UiSupport.selectedId(reservationTable, 0, "booking");
    }
}
