package ui;

import bootstrap.ApplicationServices;
import domain.AuditLog;
import domain.Employee;
import domain.MenuItem;
import domain.SharedLogin;
import persistence.AppState;
import service.AuthenticatedUser;
import service.DailySalesReport;
import service.ReservationReportRow;
import service.StaffService;
import service.StockReportRow;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Manager Tools: menu and stock, staff and profiles, reports, and the audit log. */
public final class ManagerPanel extends JPanel implements Refreshable {
    private final ApplicationServices services;
    private final Supplier<AuthenticatedUser> currentUser;

    private final DefaultTableModel menuModel = UiSupport.readOnlyModel(
            "Menu item", "Category", "Price", "Stock", "Available");
    private final DefaultTableModel staffModel = UiSupport.readOnlyModel("Name", "Role", "Signs in with", "Status");
    private final DefaultTableModel auditModel = UiSupport.readOnlyModel("Time", "Person", "Action", "Detail");
    private final JTable menuTable = new JTable(menuModel);
    private final JTable staffTable = new JTable(staffModel);

    private final ChoiceBox category = new ChoiceBox();
    private final JTextField nameField = new JTextField(16);
    private final JTextField priceField = new JTextField(8);
    private final JTextField startingStock = new JTextField("20", 6);
    private final JTextField portionField = new JTextField("Single serving", 14);
    private final ChoiceBox volume = new ChoiceBox();
    private final JTextField stockChange = new JTextField("5", 6);

    private final JTextField personName = new JTextField(16);
    private final ChoiceBox personKind = new ChoiceBox();

    private final DatePicker reportDate = new DatePicker(LocalDate.now());
    private final JTextArea report = new JTextArea();

    public ManagerPanel(ApplicationServices services, Supplier<AuthenticatedUser> currentUser) {
        this.services = services;
        this.currentUser = currentUser;
        setOpaque(false);
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        add(UiSupport.heading("Manager Tools", "Menu and stock, staff and profiles, reports, and the audit log.",
                UiSupport.refreshButton(this::refreshData)), BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Menu & Stock", menuTab());
        tabs.addTab("Staff & Profiles", staffTab());
        tabs.addTab("Reports", reportTab());
        tabs.addTab("Audit log", auditTab());
        add(tabs, BorderLayout.CENTER);
    }

    @Override
    public void refreshData() {
        refreshMenu();
        refreshStaff();
        refreshAudit();
    }

    // ---------- Menu & Stock ----------

    private JComponent menuTab() {
        category.setChoices(List.of(new Choice("Food", "Food"), new Choice("Beverage", "Beverage")));
        volume.setChoices(List.of(new Choice("250", "250 ml"), new Choice("330", "330 ml"),
                new Choice("350", "350 ml"), new Choice("450", "450 ml"), new Choice("500", "500 ml"),
                new Choice("750", "750 ml"), new Choice("1000", "1 liter")));
        volume.selectId("350");
        UiSupport.placeholder(nameField, "e.g. Chicken Adobo");
        UiSupport.placeholder(priceField, "e.g. 199.00");
        UiSupport.formatOnLeave(nameField, UiSupport::formatName);

        JPanel serving = new JPanel(new CardLayout());
        serving.setOpaque(false);
        serving.add(portionField, "Food");
        serving.add(volume, "Beverage");

        FormCard form = new FormCard("New menu item");
        form.field("Category", category);
        form.field("Name", nameField);
        form.field("Price (₱)", priceField);
        form.field("Starting stock", startingStock);
        form.field("Portion / volume", serving);
        JButton create = new JButton("Add to menu");
        form.buttons(create);
        form.section("Selected item");
        form.field("Stock change (use - to subtract)", stockChange);
        JButton adjust = Theme.subtle(new JButton("Adjust stock"));
        JButton activate = Theme.outline(new JButton("Show on menu"));
        JButton deactivate = Theme.outline(new JButton("Hide from menu"));
        form.buttons(adjust);
        form.buttons(activate, deactivate);
        category.addActionListener(event -> {
            String chosen = category.getSelectedChoiceId();
            if (chosen != null) {
                ((CardLayout) serving.getLayout()).show(serving, chosen);
            }
        });

        create.addActionListener(event -> UiSupport.perform(this, this::createMenuItem, () -> {
            refreshData();
            nameField.setText("");
            priceField.setText("");
        }));
        adjust.addActionListener(event -> selectedMenuAction(id -> services.menu().adjustStock(
                actorId(), id, UiSupport.parseInteger(stockChange.getText(), "Stock change"))));
        activate.addActionListener(event -> selectedMenuAction(id -> services.menu().activate(actorId(), id)));
        deactivate.addActionListener(event -> selectedMenuAction(id -> services.menu().deactivate(actorId(), id)));
        UiSupport.columnWidths(menuTable, 200, 100, 100, 70, 100);
        return withForm(form, UiSupport.card("Menu", UiSupport.tablePane(menuTable)));
    }

    private void createMenuItem() {
        BigDecimal amount = UiSupport.parseDecimal(priceField.getText(), "Price");
        int stock = UiSupport.parseInteger(startingStock.getText(), "Starting stock");
        String name = UiSupport.formatName(nameField.getText());
        if ("Food".equals(category.getSelectedChoiceId())) {
            services.menu().createFood(actorId(), name, amount, stock, portionField.getText());
        } else {
            services.menu().createBeverage(actorId(), name, amount, stock, volume.requireInt("drink volume"), true);
        }
    }

    private void selectedMenuAction(Consumer<String> action) {
        UiSupport.perform(this, () -> action.accept(UiSupport.selectedId(menuTable, 0, "menu item")), this::refreshData);
    }

    private void refreshMenu() {
        AppState state = services.repository().snapshot();
        String selected = UiSupport.selectedOrNull(menuTable);
        menuModel.setRowCount(0);
        state.getMenuItems().stream().sorted(Comparator.comparing(MenuItem::getName))
                .forEach(item -> menuModel.addRow(new Object[]{
                        new Choice(item.getId(), item.getName()), item.getCategoryName(), item.getUnitPrice(),
                        item.getStockQuantity(), item.isActive() ? "Yes" : "No"}));
        UiSupport.restoreSelection(menuTable, selected);
    }

    // ---------- Staff & Profiles ----------

    private JComponent staffTab() {
        personKind.setChoices(List.of(
                new Choice("Staff", "Employee profile (employee login)"),
                new Choice("Waiter", "Waiter (can be assigned to tables)"),
                new Choice("Manager", "Manager profile (manager login)")));
        UiSupport.placeholder(personName, "First and last name");
        UiSupport.formatOnLeave(personName, UiSupport::formatName);

        FormCard form = new FormCard("Add a person");
        form.field("Name", personName);
        form.field("Type", personKind);
        JButton add = new JButton("Add person");
        form.buttons(add);
        form.note("Profiles appear on the \"Would you like to log in as\" screen after the shared"
                + " employee or manager login. Waiters appear in the \"Assigned waiter\" list.");
        form.section("Selected person");
        JButton activate = Theme.outline(new JButton("Activate"));
        JButton deactivate = Theme.outline(new JButton("Deactivate"));
        form.buttons(activate, deactivate);
        form.note("Profile photos: put a picture named after the person (e.g. \"Jael Castillo.png\") in "
                + "the photos folder, or use Manage profiles on the profile screen.");

        add.addActionListener(event -> UiSupport.perform(this, () -> services.staff().addEmployee(
                actorId(), UiSupport.formatName(personName.getText()), personKind.requireId("type")), () -> {
                    refreshData();
                    personName.setText("");
                }));
        activate.addActionListener(event -> selectedStaffAction(id -> services.staff().setActive(actorId(), id, true)));
        deactivate.addActionListener(event -> selectedStaffAction(id -> services.staff().setActive(actorId(), id, false)));
        UiSupport.columnWidths(staffTable, 160, 120, 230, 90);
        return withForm(form, UiSupport.card("Everyone", UiSupport.tablePane(staffTable)));
    }

    private void selectedStaffAction(Consumer<String> action) {
        UiSupport.perform(this, () -> action.accept(UiSupport.selectedId(staffTable, 0, "person")), this::refreshData);
    }

    private void refreshStaff() {
        AppState state = services.repository().snapshot();
        String selected = UiSupport.selectedOrNull(staffTable);
        staffModel.setRowCount(0);
        state.getEmployees().stream()
                .sorted(Comparator.comparing((Employee e) -> e instanceof SharedLogin ? 0 : 1)
                        .thenComparing(Employee::getRoleName).thenComparing(Employee::getName))
                .forEach(e -> staffModel.addRow(new Object[]{new Choice(e.getId(), e.getName()), e.getRoleName(),
                        StaffService.signInDescription(e), e.isActive() ? "Active" : "Inactive"}));
        UiSupport.restoreSelection(staffTable, selected);
    }

    // ---------- Reports ----------

    private JComponent reportTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);
        JLabel dateLabel = new JLabel("Date");
        dateLabel.setFont(Theme.font(Font.BOLD, 13f));
        controls.add(dateLabel);
        reportDate.setPreferredSize(new java.awt.Dimension(220, reportDate.getPreferredSize().height));
        controls.add(reportDate);
        JButton sales = new JButton("Daily sales");
        JButton reservations = Theme.subtle(new JButton("Reservations for the day"));
        JButton stock = Theme.subtle(new JButton("Remaining stock"));
        controls.add(sales);
        controls.add(reservations);
        controls.add(stock);
        report.setEditable(false);
        report.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        report.setText("Pick a date, then choose a report.");
        sales.addActionListener(event -> UiSupport.perform(this, this::showSales, () -> { }));
        reservations.addActionListener(event -> UiSupport.perform(this, this::showReservations, () -> { }));
        stock.addActionListener(event -> UiSupport.perform(this, this::showStock, () -> { }));
        panel.add(controls, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(report);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(UiSupport.card("Report", scroll), BorderLayout.CENTER);
        return panel;
    }

    private void showSales() {
        DailySalesReport sales = services.reports().dailySales(actorId(), reportDate.getDate());
        report.setText("Daily sales for " + sales.getDate()
                + "\n\nPayments:        " + sales.getPaymentCount()
                + "\nAmount received: " + UiSupport.peso(sales.getTotalAmount()));
    }

    private void showReservations() {
        LocalDate date = reportDate.getDate();
        AppState state = services.repository().snapshot();
        StringBuilder text = new StringBuilder("Reservations for ").append(date).append("\n\n");
        List<ReservationReportRow> rows = services.reports().reservationsByDate(actorId(), date);
        if (rows.isEmpty()) {
            text.append("No reservations on this day.");
        }
        for (ReservationReportRow row : rows) {
            text.append(row.getStartTime().format(UiSupport.DATE_TIME_FORMAT))
                    .append("  |  ").append(state.getCustomerOrThrow(
                            state.getReservationOrThrow(row.getReservationId()).getCustomerId()).getFullName())
                    .append("  |  party of ").append(row.getPartySize())
                    .append("  |  ").append(UiSupport.status(row.getStatus())).append('\n');
        }
        report.setText(text.toString());
    }

    private void showStock() {
        StringBuilder text = new StringBuilder("Remaining stock\n\n");
        for (StockReportRow row : services.reports().remainingStock(actorId())) {
            text.append(String.format("%-22s %-10s %5d   %s%n", row.getName(), row.getCategory(),
                    row.getStockQuantity(), row.isActive() ? "on menu" : "hidden"));
        }
        report.setText(text.toString());
    }

    // ---------- Audit ----------

    private JComponent auditTab() {
        JTable table = new JTable(auditModel);
        JComponent pane = UiSupport.tablePane(table);
        UiSupport.columnWidths(table, 170, 150, 170, 420);
        return UiSupport.card("Who did what  ·  newest at the bottom", pane);
    }

    private void refreshAudit() {
        AppState state = services.repository().snapshot();
        auditModel.setRowCount(0);
        for (AuditLog log : services.audit().getAuditLog(actorId())) {
            auditModel.addRow(new Object[]{log.getTimestamp(), state.getEmployeeOrThrow(log.getEmployeeId()).getName(),
                    UiSupport.status(log.getAction()), log.getDetail()});
        }
    }

    // ---------- Helpers ----------

    private static JComponent withForm(FormCard form, JComponent right) {
        JPanel panel = new JPanel(new BorderLayout(14, 0));
        panel.setOpaque(false);
        panel.add(form.inColumn(), BorderLayout.WEST);
        panel.add(right, BorderLayout.CENTER);
        return panel;
    }

    private String actorId() {
        return currentUser.get().employeeId();
    }
}
