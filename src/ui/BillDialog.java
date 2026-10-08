package ui;

import bootstrap.ApplicationServices;
import domain.Bill;
import domain.DiningSession;
import domain.Order;
import domain.OrderItem;
import persistence.AppState;
import service.OrderReferences;
import util.Money;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * "Create bill" / "Change bill" pop-up. Shows what was ordered and a live total while the
 * discount and service charge are chosen. Mistakes are shown in red and can be fixed right away;
 * an unpaid bill can be opened again later with "Change bill" if something was entered wrong.
 */
final class BillDialog {
    /** Discount choices (percent). Add more here if needed. */
    static final int[] DISCOUNTS = {0, 5, 10, 15, 20, 25, 50};

    private final ApplicationServices services;
    private final String actorId;
    private final Bill existing;
    private final String visitId;
    private final BigDecimal subtotal;
    private final JDialog dialog;
    private final ChoiceBox discount = new ChoiceBox();
    private final JTextField serviceCharge = new JTextField(10);
    private final JLabel discountLine;
    private final JLabel serviceLine;
    private final JLabel totalLine;
    private final JLabel discountName;
    private final JLabel problem = Dialogs.message();
    private final JButton save;
    private boolean saved;

    /** visitId: create a bill for this served visit. billId: change this unpaid bill. Pass one of them. */
    private BillDialog(Component parent, ApplicationServices services, String actorId, AppState state,
                       String visitId, String billId) {
        this.services = services;
        this.actorId = actorId;
        this.existing = billId == null ? null : state.getBillOrThrow(billId);
        this.visitId = existing == null ? visitId : existing.getSessionId();
        DiningSession visit = state.getSessionOrThrow(this.visitId);
        Order order = state.getOrderOrThrow(visit.getOrderId());
        this.subtotal = order.getSubtotal();
        String title = existing == null ? "Create bill" : "Change bill";
        this.dialog = Dialogs.create(parent, title);
        this.save = new JButton(existing == null ? "Create bill" : "Save changes");

        JPanel body = Dialogs.body(title, "Order " + OrderReferences.display(state, order.getId())
                + "  ·  " + UiSupport.tableName(state, visit.getTableId())
                + "  ·  " + state.getCustomerOrThrow(visit.getCustomerId()).getFullName());

        // What was ordered, so the bill can be checked against it
        JPanel items = new JPanel(new GridLayout(0, 1, 0, 2));
        items.setOpaque(false);
        for (OrderItem item : order.getItems()) {
            JPanel line = new JPanel(new BorderLayout(10, 0));
            line.setOpaque(false);
            line.add(new JLabel(item.getQuantity() + " × " + item.getItemName()), BorderLayout.WEST);
            line.add(new JLabel(UiSupport.peso(item.getLineTotal())), BorderLayout.EAST);
            items.add(line);
        }
        JScrollPane itemScroll = new JScrollPane(items);
        itemScroll.setBorder(BorderFactory.createEmptyBorder());
        itemScroll.getViewport().setBackground(Theme.PAPER);
        int height = Math.min(order.getItems().size(), 5) * 24 + 4;
        itemScroll.setPreferredSize(new Dimension(380, height));
        Dialogs.add(body, Dialogs.label("Ordered"), 4);
        Dialogs.add(body, itemScroll, 12);

        List<Choice> choices = new ArrayList<>();
        for (int percent : DISCOUNTS) {
            choices.add(new Choice(String.valueOf(percent), percent == 0 ? "No discount"
                    : percent == 20 ? "20%  (Senior citizen / PWD)" : percent + "%"));
        }
        if (existing != null) {
            String current = existing.getDiscountRate().movePointRight(2).stripTrailingZeros().toPlainString();
            if (choices.stream().noneMatch(c -> c.id().equals(current))) {
                choices.add(new Choice(current, current + "%"));
            }
            discount.setChoices(choices);
            discount.selectId(current);
            serviceCharge.setText(existing.getServiceCharge().toPlainString());
        } else {
            discount.setChoices(choices);
            discount.selectId("0");
            serviceCharge.setText("0.00");
        }
        JPanel inputs = new JPanel(new GridLayout(0, 2, 10, 4));
        inputs.setOpaque(false);
        inputs.add(Dialogs.label("Discount"));
        inputs.add(Dialogs.label("Service charge (₱)"));
        inputs.add(discount);
        inputs.add(serviceCharge);
        Dialogs.add(body, inputs, 12);

        RoundedPanel summary = Dialogs.summary();
        Dialogs.row(summary, "Subtotal", UiSupport.peso(subtotal), false);
        JLabel[] d = Dialogs.row(summary, "Discount", "", false);
        discountName = d[0];
        discountLine = d[1];
        serviceLine = Dialogs.row(summary, "Service charge", "", false)[1];
        totalLine = Dialogs.row(summary, "Total to pay", "", true)[1];
        Dialogs.add(body, summary, 10);
        Dialogs.add(body, problem, 0);

        discount.addActionListener(e -> preview());
        serviceCharge.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { preview(); }
            @Override public void removeUpdate(DocumentEvent e) { preview(); }
            @Override public void changedUpdate(DocumentEvent e) { preview(); }
        });
        preview();

        JButton cancel = Theme.outline(new JButton("Cancel"));
        cancel.addActionListener(e -> dialog.dispose());
        save.addActionListener(e -> save());
        Dialogs.show(dialog, body, 460, cancel, save);
    }

    /** Opens "Create bill". Returns true when a bill was created. */
    static boolean create(Component parent, ApplicationServices services, String actorId, AppState state, String visitId) {
        return new BillDialog(parent, services, actorId, state, visitId, null).saved;
    }

    /** Opens "Change bill" for an unpaid bill. Returns true when it was changed. */
    static boolean change(Component parent, ApplicationServices services, String actorId, AppState state, String billId) {
        return new BillDialog(parent, services, actorId, state, null, billId).saved;
    }

    private BigDecimal rate() {
        String id = discount.getSelectedChoiceId();
        return id == null ? BigDecimal.ZERO : new BigDecimal(id).movePointLeft(2);
    }

    /** Recomputes the total on every change, and explains a wrong service charge. */
    private void preview() {
        BigDecimal charge = PaymentDialog.parse(serviceCharge.getText().isBlank() ? "0" : serviceCharge.getText());
        BigDecimal discountAmount = Money.multiply(subtotal, rate());
        discountName.setText(rate().signum() == 0 ? "Discount" : "Discount (" + PaymentDialog.percent(rate()) + ")");
        discountLine.setText(discountAmount.signum() == 0 ? "—" : "− " + UiSupport.peso(discountAmount));
        if (charge == null) {
            serviceLine.setText("?");
            totalLine.setText("?");
            Dialogs.say(problem, "Service charge must be a number, for example 0, 50 or 75.50.", Dialogs.ERROR);
            save.setEnabled(false);
            return;
        }
        serviceLine.setText(charge.signum() == 0 ? "—" : UiSupport.peso(charge));
        totalLine.setText(UiSupport.peso(Money.add(Money.subtract(subtotal, discountAmount), charge)));
        Dialogs.say(problem, existing == null ? null
                : "Was " + UiSupport.peso(existing.getGrandTotal()) + ". Saving replaces the old total.", Theme.MUTED);
        save.setEnabled(true);
    }

    private void save() {
        try {
            BigDecimal charge = PaymentDialog.parse(serviceCharge.getText().isBlank() ? "0" : serviceCharge.getText());
            if (charge == null) {
                throw new IllegalArgumentException("Service charge must be a number.");
            }
            if (existing == null) {
                services.billing().issueBill(actorId, visitId, rate(), charge);
            } else {
                services.billing().changeBillCharges(actorId, existing.getId(), rate(), charge);
            }
            saved = true;
            dialog.dispose();
        } catch (RuntimeException error) {
            Dialogs.say(problem, (error.getMessage() == null ? "The bill could not be saved." : error.getMessage())
                    + " Fix it and try again.", Dialogs.ERROR);
        }
    }
}
