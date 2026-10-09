package ui;

import bootstrap.ApplicationServices;
import domain.enums.OrderStatus;
import service.AuthenticatedUser;
import service.KitchenTicket;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.function.Supplier;

/** Step 3 — Kitchen: start cooking sent orders, then mark them ready to serve. */
public final class KitchenPanel extends JPanel implements Refreshable, OrderFocus {
    private final ApplicationServices services;
    private final Supplier<AuthenticatedUser> currentUser;
    private final WorkflowBar workflow;
    private final DefaultTableModel model = UiSupport.readOnlyModel("Order", "Table", "Progress", "Items");
    private final JTable table = new JTable(model);
    private final JButton preparing = new JButton("Start preparing");
    private final JButton ready = Theme.secondary(new JButton("Mark ready to serve"));
    private final JLabel selection = new JLabel("No order selected");
    private final JLabel hint = UiSupport.hint(FormCard.WIDTH - 130);

    public KitchenPanel(ApplicationServices services, Supplier<AuthenticatedUser> currentUser, Navigator navigator) {
        this.services = services;
        this.currentUser = currentUser;
        this.workflow = new WorkflowBar(navigator, KouzinaFrame.KITCHEN, () -> UiSupport.selectedOrNull(table));
        setOpaque(false);
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(UiSupport.heading("Kitchen",
                "Select an order to start cooking. Mark it ready when the waiter can collect it.",
                UiSupport.refreshButton(this::refreshData)), BorderLayout.NORTH);
        top.add(workflow, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(14, 0));
        body.setOpaque(false);
        body.add(UiSupport.card("Kitchen queue  ·  oldest first", UiSupport.tablePane(table)), BorderLayout.CENTER);
        UiSupport.columnWidths(table, 100, 90, 150, 380);

        FormCard actions = new FormCard("Selected order");
        selection.setFont(Theme.font(java.awt.Font.BOLD, 14f));
        actions.field("Order", selection);
        actions.buttons(preparing);
        actions.buttons(ready);
        actions.hint(hint);
        actions.note("Sent → Preparing → Ready. Ready orders show up in Orders so the waiter can serve them.");
        body.add(actions.inColumn(), BorderLayout.EAST);
        add(body, BorderLayout.CENTER);

        preparing.addActionListener(e -> UiSupport.perform(this,
                () -> services.kitchen().markPreparing(actor(), selected()), this::refreshData));
        ready.addActionListener(e -> UiSupport.perform(this,
                () -> services.kitchen().markReady(actor(), selected()), this::refreshData));
        table.getSelectionModel().addListSelectionListener(e -> updateActions());
        updateActions();
        services.orderEvents().addListener((id, status) -> SwingUtilities.invokeLater(() -> {
            if (isShowing()) {
                refreshData();
            }
        }));
    }

    @Override
    public void refreshData() {
        String selected = UiSupport.selectedOrNull(table);
        workflow.refreshAccess();
        model.setRowCount(0);
        var state = services.repository().snapshot();
        for (KitchenTicket ticket : services.kitchen().getQueue(actor())) {
            model.addRow(new Object[]{UiSupport.orderChoice(state, ticket.orderId()), "Table " + ticket.tableNumber(),
                    UiSupport.status(ticket.status()), String.join(", ", ticket.itemSummaries())});
        }
        UiSupport.restoreSelection(table, selected);
        if (table.getSelectedRow() < 0 && table.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
        }
        updateActions();
    }

    @Override
    public void focusOrder(String orderId) {
        UiSupport.restoreSelection(table, orderId);
    }

    private void updateActions() {
        String id = UiSupport.selectedOrNull(table);
        var state = services.repository().snapshot();
        OrderStatus status = id == null ? null : state.getOrderOrThrow(id).getStatus();
        preparing.setEnabled(status == OrderStatus.CONFIRMED);
        ready.setEnabled(status == OrderStatus.PREPARING);
        selection.setText(id == null ? "No order selected"
                : UiSupport.orderChoice(state, id).label() + "  ·  " + table.getValueAt(table.getSelectedRow(), 1));
        UiSupport.setHint(hint, id == null
                ? (table.getRowCount() == 0 ? "Nothing to cook. Orders appear here after they are sent from Orders."
                        : "Select an order in the queue.")
                : status == OrderStatus.CONFIRMED ? "New order. Click Start preparing when you begin cooking."
                : status == OrderStatus.PREPARING ? "Cooking. Click Mark ready to serve when the food is done."
                : "Ready. The waiter will serve it from Orders.");
    }

    private String actor() {
        return currentUser.get().sessionToken();
    }

    private String selected() {
        return UiSupport.selectedId(table, 0, "kitchen order");
    }
}
