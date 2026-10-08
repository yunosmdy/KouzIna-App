package ui;

import bootstrap.ApplicationServices;
import domain.Bill;
import domain.DiningSession;
import persistence.AppState;
import service.OrderReferences;
import service.PaymentReceipt;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * "Take payment" pop-up. Shows the amount due, and while the user types the cash received it shows
 * the change (or how much is missing). Nothing is saved until "Record payment" is clicked, and if
 * something is wrong the pop-up stays open so the amount can be fixed and tried again.
 */
final class PaymentDialog {
    /** E-payment options in the dropdown. Add or remove names here. */
    static final String[] E_PAYMENT_OPTIONS = {
            "GCash", "Maya", "MariBank", "GoTyme Bank", "ShopeePay", "GrabPay", "Coins.ph",
            "Bank transfer (InstaPay / PESONet)", "Credit / Debit card", "E-wallet (other)"};
    private static final String CASH = "cash";
    private static final String E_PAYMENT = "epay";

    private final ApplicationServices services;
    private final String actorId;
    private final Bill bill;
    private final JDialog dialog;
    private final JTextField cash = new JTextField(12);
    private final ChoiceBox provider = new ChoiceBox();
    private final JTextField reference = new JTextField(14);
    private final JLabel cashStatus = Dialogs.message();
    private final JLabel error = Dialogs.message();
    private final JButton record = Theme.secondary(new JButton("Record payment"));
    private final JButton cashChip = new JButton("Cash");
    private final JButton ePayChip = new JButton("E-payment");
    private final JPanel methodCards = new JPanel(new CardLayout());
    private String method = CASH;
    private PaymentReceipt receipt;

    private PaymentDialog(Component parent, ApplicationServices services, String actorId, AppState state, String billId) {
        this.services = services;
        this.actorId = actorId;
        this.bill = state.getBillOrThrow(billId);
        this.dialog = Dialogs.create(parent, "Take payment");
        DiningSession visit = state.getSessionOrThrow(bill.getSessionId());

        JPanel body = Dialogs.body("Take payment", "Order " + OrderReferences.display(state, visit.getOrderId())
                + "  ·  " + UiSupport.tableName(state, visit.getTableId())
                + "  ·  " + state.getCustomerOrThrow(visit.getCustomerId()).getFullName());

        RoundedPanel summary = Dialogs.summary();
        Dialogs.row(summary, "Subtotal", UiSupport.peso(bill.getSubtotal()), false);
        if (bill.getDiscountAmount().signum() > 0) {
            Dialogs.row(summary, "Discount (" + percent(bill.getDiscountRate()) + ")",
                    "− " + UiSupport.peso(bill.getDiscountAmount()), false);
        }
        if (bill.getServiceCharge().signum() > 0) {
            Dialogs.row(summary, "Service charge", UiSupport.peso(bill.getServiceCharge()), false);
        }
        Dialogs.row(summary, "Amount due", UiSupport.peso(bill.getGrandTotal()), true);
        Dialogs.add(body, summary, 14);

        Dialogs.add(body, Dialogs.label("Paid with"), 6);
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chips.setOpaque(false);
        chips.add(cashChip);
        chips.add(ePayChip);
        cashChip.addActionListener(e -> setMethod(CASH));
        ePayChip.addActionListener(e -> setMethod(E_PAYMENT));
        Dialogs.add(body, chips, 12);

        methodCards.setOpaque(false);
        methodCards.add(cashCard(), CASH);
        methodCards.add(ePaymentCard(), E_PAYMENT);
        Dialogs.add(body, methodCards, 8);
        Dialogs.add(body, error, 0);

        JButton cancel = Theme.outline(new JButton("Cancel"));
        cancel.addActionListener(e -> dialog.dispose());
        record.addActionListener(e -> save());
        setMethod(CASH);
        Dialogs.show(dialog, body, 470, cancel, record);
    }

    /** Opens the pop-up. Returns the receipt, or null when the user cancelled. */
    static PaymentReceipt show(Component parent, ApplicationServices services, String actorId,
                               AppState state, String billId) {
        return new PaymentDialog(parent, services, actorId, state, billId).receipt;
    }

    private JPanel cashCard() {
        JPanel card = new JPanel();
        card.setOpaque(false);
        card.setLayout(new javax.swing.BoxLayout(card, javax.swing.BoxLayout.Y_AXIS));
        Dialogs.add(card, Dialogs.label("Cash received (₱)"), 6);
        cash.setFont(Theme.font(Font.BOLD, 18f));
        UiSupport.placeholder(cash, "Type the amount the customer gave");
        Dialogs.add(card, cash, 8);
        JPanel quick = new JPanel(new GridLayout(0, 4, 6, 6));
        quick.setOpaque(false);
        for (Choice amount : quickAmounts(bill.getGrandTotal())) {
            JButton chip = Theme.chip(new JButton(amount.label()), false);
            chip.addActionListener(e -> {
                cash.setText(amount.id());
                cash.requestFocusInWindow();
            });
            quick.add(chip);
        }
        Dialogs.add(card, quick, 10);
        Dialogs.add(card, cashStatus, 0);
        cash.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { validate(); }
            @Override public void removeUpdate(DocumentEvent e) { validate(); }
            @Override public void changedUpdate(DocumentEvent e) { validate(); }
        });
        return card;
    }

    private JPanel ePaymentCard() {
        List<Choice> providers = new ArrayList<>();
        for (String name : E_PAYMENT_OPTIONS) {
            providers.add(new Choice(name, name));
        }
        provider.setChoices(providers);
        UiSupport.placeholder(reference, "Reference number from the receipt (optional)");
        JPanel card = new JPanel();
        card.setOpaque(false);
        card.setLayout(new javax.swing.BoxLayout(card, javax.swing.BoxLayout.Y_AXIS));
        Dialogs.add(card, Dialogs.label("App or card"), 6);
        Dialogs.add(card, provider, 10);
        Dialogs.add(card, Dialogs.label("Reference no."), 6);
        Dialogs.add(card, reference, 10);
        JLabel note = UiSupport.muted("<html><div style='width:360px'>Check the customer's screen shows a successful payment of "
                + UiSupport.peso(bill.getGrandTotal()) + " before recording it.</div></html>");
        Dialogs.add(card, note, 0);
        return card;
    }

    private void setMethod(String chosen) {
        method = chosen;
        Theme.chip(cashChip, CASH.equals(chosen));
        Theme.chip(ePayChip, E_PAYMENT.equals(chosen));
        ((CardLayout) methodCards.getLayout()).show(methodCards, chosen);
        Dialogs.say(error, null, Dialogs.ERROR);
        validate();
        (CASH.equals(chosen) ? cash : reference).requestFocusInWindow();
    }

    /** Checks the typed amount on every key press and explains what is wrong. */
    private void validate() {
        if (!CASH.equals(method)) {
            record.setEnabled(true);
            record.setText("Record " + UiSupport.peso(bill.getGrandTotal()) + " e-payment");
            return;
        }
        BigDecimal received = parse(cash.getText());
        BigDecimal due = bill.getGrandTotal();
        record.setText("Record payment");
        if (cash.getText().isBlank()) {
            Dialogs.say(cashStatus, "Type the amount received, or click one of the amounts above.", Theme.MUTED);
            record.setEnabled(false);
        } else if (received == null) {
            Dialogs.say(cashStatus, "That is not a valid amount. Use numbers only, for example 1000 or 650.50.", Dialogs.ERROR);
            record.setEnabled(false);
        } else if (received.compareTo(due) < 0) {
            Dialogs.say(cashStatus, "Short by " + UiSupport.peso(due.subtract(received))
                    + ". Check the amount, or ask the customer for more.", Dialogs.ERROR);
            record.setEnabled(false);
        } else {
            BigDecimal change = received.subtract(due);
            boolean suspicious = change.compareTo(new BigDecimal("1000")) >= 0
                    && received.compareTo(due.multiply(new BigDecimal("3"))) > 0;
            Dialogs.say(cashStatus, suspicious
                    ? "Change: " + UiSupport.peso(change) + " — that is a lot. Please double-check the amount received."
                    : "Change: " + UiSupport.peso(change), suspicious ? Dialogs.WARNING : Dialogs.OK);
            record.setEnabled(true);
            record.setText("Record payment · change " + UiSupport.peso(change));
        }
    }

    private void save() {
        try {
            if (CASH.equals(method)) {
                BigDecimal received = parse(cash.getText());
                if (received == null) {
                    throw new IllegalArgumentException("Enter the cash received.");
                }
                receipt = services.billing().acceptCash(actorId, bill.getId(), received);
            } else {
                String paidWith = provider.requireId("e-payment option")
                        + (reference.getText().isBlank() ? "" : " #" + reference.getText().trim());
                receipt = services.billing().acceptElectronic(actorId, bill.getId(), paidWith);
            }
            dialog.dispose();
        } catch (RuntimeException problem) {
            // Keep the pop-up open so the user can fix the amount and try again
            Dialogs.say(error, (problem.getMessage() == null ? "The payment could not be recorded." : problem.getMessage())
                    + " Fix it and try again.", Dialogs.ERROR);
        }
    }

    /** "1,000" or "₱650.50" -> number; null when it is not a usable amount. */
    static BigDecimal parse(String text) {
        String cleaned = text.trim().replace(",", "").replace("₱", "").replace(" ", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            BigDecimal amount = new BigDecimal(cleaned);
            return amount.signum() < 0 || amount.scale() > 2 ? null : amount;
        } catch (NumberFormatException error) {
            return null;
        }
    }

    /** Exact, then round amounts that cover the bill (e.g. due 634 -> 650, 700, 1,000, 2,000). */
    static List<Choice> quickAmounts(BigDecimal due) {
        List<Choice> amounts = new ArrayList<>();
        BigDecimal exact = due.setScale(2, RoundingMode.HALF_UP);
        amounts.add(new Choice(exact.toPlainString(), "Exact"));
        int whole = due.setScale(0, RoundingMode.CEILING).intValue();
        TreeSet<Integer> rounded = new TreeSet<>();
        for (int step : new int[]{50, 100, 500, 1000}) {
            int value = ((whole + step - 1) / step) * step;
            if (value > whole || BigDecimal.valueOf(value).compareTo(due) > 0) {
                rounded.add(value);
            }
        }
        rounded.add(((whole + 999) / 1000) * 1000 + 1000);
        for (int value : rounded.stream().limit(7).toList()) {
            amounts.add(new Choice(String.valueOf(value), "₱" + String.format("%,d", value)));
        }
        return amounts;
    }

    static String percent(BigDecimal rate) {
        return rate.movePointRight(2).stripTrailingZeros().toPlainString() + "%";
    }
}
