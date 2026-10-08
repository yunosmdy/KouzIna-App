package ui;

import bootstrap.ApplicationServices;
import domain.Bill;
import domain.DiningSession;
import domain.Order;
import domain.enums.OrderStatus;
import persistence.AppState;
import service.AuthenticatedUser;
import service.OrderReferences;
import service.PaymentReceipt;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.function.Supplier;

/**
 * Step 4 — Billing: create the bill for a served order, take payment, and free the table.
 * Creating a bill and taking payment both open a pop-up that shows the totals first and
 * lets the user fix a wrong amount before anything is saved.
 */
public final class BillingPanel extends JPanel implements Refreshable, OrderFocus {
    private final ApplicationServices services;
    private final Supplier<AuthenticatedUser> currentUser;
    private final Navigator navigator;
    private final WorkflowBar workflow;
    private final DefaultTableModel readyModel = UiSupport.readOnlyModel("Order", "Table", "Guest", "Subtotal");
    private final DefaultTableModel billModel = UiSupport.readOnlyModel(
            "Order", "Table", "Guest", "Discount", "Service", "Total", "Status");
    private final JTable readyTable = new JTable(readyModel);
    private final JTable bills = new JTable(billModel);

    private final JButton issue = new JButton("Create bill…");
    private final JButton change = Theme.subtle(new JButton("Change bill…"));
    private final JButton pay = Theme.secondary(new JButton("Take payment…"));
    private final JCheckBox paid = new JCheckBox("Show paid bills");
    private final JLabel selectedOrder = new JLabel("—");
    private final JLabel total = new JLabel("—");
    private final JLabel hint = UiSupport.hint(FormCard.WIDTH - 130);
    private AppState state;

    public BillingPanel(ApplicationServices services, Supplier<AuthenticatedUser> currentUser, Navigator navigator) {
        this.services = services;
        this.currentUser = currentUser;
        this.navigator = navigator;
        this.workflow = new WorkflowBar(navigator, KouzinaFrame.BILLING, this::selectedOrderId);
        setOpaque(false);
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(UiSupport.heading("Billing",
                "1. Select a served order and create its bill   2. Select the bill and take the payment",
                UiSupport.refreshButton(this::refreshData)), BorderLayout.NORTH);
        top.add(workflow, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JPanel billsCard = new JPanel(new BorderLayout(0, 6));
        billsCard.setOpaque(false);
        paid.setOpaque(false);
        JPanel billsHeader = new JPanel(new BorderLayout());
        billsHeader.setOpaque(false);
        billsHeader.add(paid, BorderLayout.EAST);
        billsCard.add(billsHeader, BorderLayout.NORTH);
        billsCard.add(UiSupport.tablePane(bills), BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                UiSupport.card("1. Served orders waiting for a bill", UiSupport.tablePane(readyTable)),
                UiSupport.card("2. Bills", billsCard));
        split.setResizeWeight(0.42);
        split.setOpaque(false);
        split.setBorder(BorderFactory.createEmptyBorder());
        UiSupport.columnWidths(bills, 80, 75, 130, 85, 75, 95, 95);
        UiSupport.columnWidths(readyTable, 95, 90, 200, 110);

        JPanel body = new JPanel(new BorderLayout(14, 0));
        body.setOpaque(false);
        body.add(split, BorderLayout.CENTER);
        body.add(form().inColumn(), BorderLayout.EAST);
        add(body, BorderLayout.CENTER);

        readyTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && readyTable.getSelectedRow() >= 0) {
                bills.clearSelection();
            }
            updateActions();
        });
        bills.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && bills.getSelectedRow() >= 0) {
                readyTable.clearSelection();
            }
            updateActions();
        });
        // Double-click: open the matching pop-up right away
        readyTable.addMouseListener(doubleClick(this::createBill));
        bills.addMouseListener(doubleClick(this::takePayment));
        paid.addActionListener(e -> refreshData());
    }

    private FormCard form() {
        FormCard form = new FormCard("Bill and payment");
        selectedOrder.setFont(Theme.font(Font.BOLD, 14f));
        form.field("Selected", selectedOrder);
        form.section("1. Bill");
        form.buttons(issue);
        form.buttons(change);
        form.note("Typed a wrong discount or service charge? Select the unpaid bill and click Change bill.");
        form.section("2. Payment");
        total.setFont(Theme.font(Font.BOLD, 24f));
        form.field("Amount due", total);
        form.buttons(pay);
        form.note("The pop-up shows the change before anything is saved, so a wrong amount can be fixed first.");
        form.hint(hint);

        issue.addActionListener(e -> createBill());
        change.addActionListener(e -> changeBill());
        pay.addActionListener(e -> takePayment());
        return form;
    }

    private void createBill() {
        String visitId = UiSupport.selectedOrNull(readyTable);
        if (visitId == null) {
            return;
        }
        if (BillDialog.create(this, services, actor(), state, visitId)) {
            refreshData();
            state.findBillBySession(visitId).ifPresent(bill -> UiSupport.restoreSelection(bills, bill.getId()));
        }
    }

    private void changeBill() {
        String billId = UiSupport.selectedOrNull(bills);
        if (billId == null || state.getBillOrThrow(billId).isPaid()) {
            return;
        }
        if (BillDialog.change(this, services, actor(), state, billId)) {
            refreshData();
            UiSupport.restoreSelection(bills, billId);
        }
    }

    private void takePayment() {
        String billId = UiSupport.selectedOrNull(bills);
        if (billId == null || state.getBillOrThrow(billId).isPaid()) {
            return;
        }
        PaymentReceipt receipt = PaymentDialog.show(this, services, actor(), state, billId);
        if (receipt == null) {
            return;
        }
        refreshData();
        String table = UiSupport.tableName(state, receipt.releasedTableId());
        boolean cash = receipt.change().signum() > 0;
        String message = "Paid " + UiSupport.peso(receipt.amountApplied()) + "."
                + (cash ? "\nGive the customer " + UiSupport.peso(receipt.change()) + " change." : "")
                + "\n" + table + " is free again."
                + (receipt.compatibleWaitlistEntryId() != null ? "\nA party on the waiting list fits this table." : "");
        int choice = Dialogs.choose(this, "Payment recorded", "Payment recorded", message,
                "Stay on Billing", "Seat next guests");
        if (choice == 1) {
            navigator.open(KouzinaFrame.RESERVATIONS);
        }
    }

    private static java.awt.event.MouseAdapter doubleClick(Runnable action) {
        return new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2) {
                    action.run();
                }
            }
        };
    }

    // ---------- Data ----------

    @Override
    public void refreshData() {
        String selectedReady = UiSupport.selectedOrNull(readyTable);
        String selectedBill = UiSupport.selectedOrNull(bills);
        state = services.repository().snapshot();
        workflow.refreshAccess();
        readyModel.setRowCount(0);
        for (DiningSession visit : state.getSessions()) {
            Order order = state.getOrderOrThrow(visit.getOrderId());
            if (visit.isOpen() && order.getStatus() == OrderStatus.SERVED && state.findBillBySession(visit.getId()).isEmpty()) {
                readyModel.addRow(new Object[]{new Choice(visit.getId(), OrderReferences.display(state, order.getId())),
                        UiSupport.tableName(state, visit.getTableId()),
                        state.getCustomerOrThrow(visit.getCustomerId()).getFullName(), order.getSubtotal()});
            }
        }
        billModel.setRowCount(0);
        for (Bill bill : state.getBills()) {
            if (bill.isPaid() && !paid.isSelected()) {
                continue;
            }
            DiningSession visit = state.getSessionOrThrow(bill.getSessionId());
            billModel.addRow(new Object[]{new Choice(bill.getId(), OrderReferences.display(state, visit.getOrderId())),
                    UiSupport.tableName(state, visit.getTableId()),
                    state.getCustomerOrThrow(visit.getCustomerId()).getFullName(),
                    bill.getDiscountAmount(), bill.getServiceCharge(), bill.getGrandTotal(), UiSupport.status(bill.getStatus())});
        }
        UiSupport.restoreSelection(readyTable, selectedReady);
        UiSupport.restoreSelection(bills, selectedBill);
        if (readyTable.getSelectedRow() < 0 && bills.getSelectedRow() < 0) {
            if (bills.getRowCount() > 0) {
                bills.setRowSelectionInterval(0, 0);
            } else if (readyTable.getRowCount() > 0) {
                readyTable.setRowSelectionInterval(0, 0);
            }
        }
        updateActions();
    }

    /** Opened from another step with an order: select its bill, or the order waiting for a bill. */
    @Override
    public void focusOrder(String orderId) {
        if (state == null || orderId == null) {
            return;
        }
        String visitId = state.getOrders().stream().filter(o -> o.getId().equals(orderId))
                .map(Order::getSessionId).findFirst().orElse(null);
        if (visitId == null) {
            return;
        }
        if (UiSupport.restoreSelection(readyTable, visitId)) {
            return;
        }
        state.findBillBySession(visitId).ifPresent(bill -> {
            if (!UiSupport.restoreSelection(bills, bill.getId()) && !paid.isSelected()) {
                paid.setSelected(true);
                refreshData();
                UiSupport.restoreSelection(bills, bill.getId());
            }
        });
    }

    /** Order behind the selected bill or served visit (passed on by the step bar). */
    private String selectedOrderId() {
        if (state == null) {
            return null;
        }
        String billId = UiSupport.selectedOrNull(bills);
        if (billId != null) {
            return state.getSessionOrThrow(state.getBillOrThrow(billId).getSessionId()).getOrderId();
        }
        String visitId = UiSupport.selectedOrNull(readyTable);
        return visitId == null ? null : state.getSessionOrThrow(visitId).getOrderId();
    }

    private void updateActions() {
        if (state == null) {
            return;
        }
        String visitId = UiSupport.selectedOrNull(readyTable);
        String billId = UiSupport.selectedOrNull(bills);
        Bill bill = billId == null ? null : state.getBillOrThrow(billId);
        boolean unpaid = bill != null && !bill.isPaid();
        issue.setEnabled(visitId != null);
        change.setEnabled(unpaid);
        pay.setEnabled(unpaid);
        String orderId = selectedOrderId();
        selectedOrder.setText(orderId == null ? "—" : OrderReferences.display(state, orderId) + "  ·  "
                + UiSupport.tableName(state, state.getSessionOrThrow(state.getOrderOrThrow(orderId).getSessionId()).getTableId()));
        total.setText(bill == null ? "—" : UiSupport.peso(bill.getGrandTotal()) + (bill.isPaid() ? "  (paid)" : ""));
        UiSupport.setHint(hint, readyTable.getRowCount() == 0 && billModel.getRowCount() == 0
                ? "Nothing to bill yet. Orders appear here after they are marked served."
                : visitId != null ? "Click Create bill (or double-click the order)."
                : unpaid ? "Click Take payment (or double-click the bill)."
                : bill != null ? "This bill is paid. The table is free again."
                : "Select a served order to bill it, or a bill to take payment.");
    }

    private String actor() {
        return currentUser.get().employeeId();
    }
}
