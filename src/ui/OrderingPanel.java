package ui;

import bootstrap.ApplicationServices;
import domain.DiningSession;
import domain.MenuItem;
import domain.Order;
import domain.OrderItem;
import domain.enums.OrderStatus;
import persistence.AppState;
import service.AuthenticatedUser;
import service.OrderReferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.Scrollable;
import javax.swing.table.DefaultTableModel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Supplier;

/**
 * Step 2 — Orders.
 * Top: the active orders. Bottom left: the menu as picture cards (click to select, double-click to add).
 * Bottom right: what the selected order contains, plus its buttons.
 */
public final class OrderingPanel extends JPanel implements Refreshable, OrderFocus {
    private static final String ALL = "All";
    private static final int CARD_WIDTH = 150;
    private static final int CARD_GAP = 14;

    private final ApplicationServices services;
    private final Supplier<AuthenticatedUser> currentUser;
    private final WorkflowBar workflow;
    private final DefaultTableModel orderModel = UiSupport.readOnlyModel(
            "Order", "Table", "Guest", "Progress", "Subtotal", "Items");
    private final DefaultTableModel itemModel = UiSupport.readOnlyModel("Item", "Qty", "Total");
    private final JTable orders = new JTable(orderModel);
    private final JTable items = new JTable(itemModel);
    private final ChoiceBox quantity = new ChoiceBox().numbers(1, 30);

    private final JButton add = new JButton("Add to order");
    private final JButton update = Theme.subtle(new JButton("Set quantity"));
    private final JButton remove = Theme.outline(new JButton("Remove item"));
    private final JButton send = new JButton("Send to kitchen");
    private final JButton serve = Theme.secondary(new JButton("Mark served"));
    private final JButton cancel = Theme.outline(new JButton("Cancel order"));
    private final JButton recall = Theme.outline(new JButton("Take back order"));
    private final JCheckBox history = new JCheckBox("Show finished orders");
    private final JLabel orderTitle = new JLabel("No order selected");
    private final JLabel hint = UiSupport.hint(280);

    private final JPanel chipRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JPanel cardGrid = new JPanel(new GridLayout(0, 4, CARD_GAP, CARD_GAP));
    private final List<MenuItem> menuItems = new ArrayList<>();
    private String selectedCategory = ALL;
    private String selectedMenuId;
    private AppState state;

    public OrderingPanel(ApplicationServices services, Supplier<AuthenticatedUser> currentUser, Navigator navigator) {
        this.services = services;
        this.currentUser = currentUser;
        this.workflow = new WorkflowBar(navigator, KouzinaFrame.ORDERING, () -> UiSupport.selectedOrNull(orders));
        setOpaque(false);
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(UiSupport.heading("Orders",
                "1. Select an order   2. Add food from the menu   3. Send to kitchen   4. Mark served when ready",
                UiSupport.refreshButton(this::refreshData)), BorderLayout.NORTH);
        top.add(workflow, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JPanel bottom = new JPanel(new BorderLayout(14, 0));
        bottom.setOpaque(false);
        bottom.add(menuSection(), BorderLayout.CENTER);
        bottom.add(orderSection(), BorderLayout.EAST);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, ordersBlock(), bottom);
        split.setResizeWeight(0.3);
        split.setOpaque(false);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setContinuousLayout(true);
        add(split, BorderLayout.CENTER);

        orders.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refreshItems();
            }
        });
        items.getSelectionModel().addListSelectionListener(e -> {
            if (items.getSelectedRow() >= 0) {
                quantity.selectId(String.valueOf(items.getValueAt(items.getSelectedRow(), 1)));
            }
            updateActions();
        });
        history.setOpaque(false);
        history.addActionListener(e -> refreshData());
        add.addActionListener(e -> addSelectedItem());
        update.addActionListener(e -> action(() -> services.orders().updateItemQuantity(actor(), orderId(),
                UiSupport.selectedId(items, 0, "ordered item"), quantity.requireInt("quantity"))));
        remove.addActionListener(e -> removeSelectedItem());
        send.addActionListener(e -> reviewAndSend());
        recall.addActionListener(e -> recallFromKitchen());
        recall.setToolTipText("Bring a sent order back from the kitchen to fix it (only before cooking starts)");
        serve.addActionListener(e -> markServed());
        cancel.addActionListener(e -> {
            if (UiSupport.confirm(this, "Cancel this order and free its table?")) {
                action(() -> services.orders().cancelOrder(actor(), orderId()));
            }
        });
    }

    private JComponent ordersBlock() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel heading = new JLabel("Active orders");
        heading.setFont(Theme.font(Font.BOLD, 14.5f));
        heading.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        header.add(heading, BorderLayout.WEST);
        header.add(history, BorderLayout.EAST);
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 6), Theme.WHITE, 18).padding(10, 12, 8, 12);
        card.add(header, BorderLayout.NORTH);
        card.add(UiSupport.tablePane(orders), BorderLayout.CENTER);
        UiSupport.columnWidths(orders, 95, 90, 150, 140, 110, 300);
        card.setMinimumSize(new Dimension(200, 130));
        return card;
    }

    // ---------- Menu cards (bottom left) ----------

    private JComponent menuSection() {
        JPanel section = new JPanel(new BorderLayout(0, 10));
        section.setOpaque(false);
        section.setMinimumSize(new Dimension(CARD_WIDTH + 40, 200));
        chipRow.setOpaque(false);
        section.add(chipRow, BorderLayout.NORTH);

        cardGrid.setOpaque(false);
        JPanel gridHolder = new WidthTrackingPanel();
        gridHolder.setOpaque(false);
        gridHolder.setBorder(BorderFactory.createEmptyBorder(2, 2, 8, 8));
        gridHolder.add(cardGrid, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(gridHolder);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        section.add(scroll, BorderLayout.CENTER);
        return section;
    }

    /** Holds the card grid; fits its width to the scroll pane and picks how many columns fit. */
    private final class WidthTrackingPanel extends JPanel implements Scrollable {
        WidthTrackingPanel() {
            super(new BorderLayout());
        }

        @Override
        public void doLayout() {
            int columns = Math.max(1, (getWidth() + CARD_GAP - 10) / (CARD_WIDTH + CARD_GAP));
            if (((GridLayout) cardGrid.getLayout()).getColumns() != columns) {
                cardGrid.setLayout(new GridLayout(0, columns, CARD_GAP, CARD_GAP));
            }
            super.doLayout();
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 18; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private void rebuildChips() {
        chipRow.removeAll();
        TreeSet<String> categories = new TreeSet<>();
        menuItems.forEach(item -> categories.add(item.getCategoryName()));
        List<String> chips = new ArrayList<>();
        chips.add(ALL);
        chips.addAll(categories);
        if (!chips.contains(selectedCategory)) {
            selectedCategory = ALL;
        }
        for (String category : chips) {
            JButton chip = Theme.chip(new JButton(category), category.equals(selectedCategory));
            chip.addActionListener(event -> {
                selectedCategory = category;
                rebuildChips();
                rebuildCards();
            });
            chipRow.add(chip);
        }
        JLabel tip = UiSupport.muted("   Click a dish to select it, double-click to add it.");
        chipRow.add(tip);
        chipRow.revalidate();
        chipRow.repaint();
    }

    private void rebuildCards() {
        cardGrid.removeAll();
        for (MenuItem item : menuItems) {
            if (selectedCategory.equals(ALL) || item.getCategoryName().equals(selectedCategory)) {
                cardGrid.add(new MenuCard(item));
            }
        }
        cardGrid.revalidate();
        cardGrid.repaint();
    }

    /** One menu item: picture (photos/menu/<name>.png) or initials, name, stock, price. */
    private final class MenuCard extends RoundedPanel {
        private final String itemId;

        MenuCard(MenuItem item) {
            super(new BorderLayout(0, 8), Theme.WHITE, 20);
            padding(12, 12, 12, 12);
            setPreferredSize(new Dimension(CARD_WIDTH, 214));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            itemId = item.getId();
            if (item.getId().equals(selectedMenuId)) {
                outline(Theme.ORANGE);
            }
            setToolTipText(item.getName() + " — " + UiSupport.peso(item.getUnitPrice()));

            JPanel pictureRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            pictureRow.setOpaque(false);
            pictureRow.add(new DishPicture(item.getName()));
            add(pictureRow, BorderLayout.NORTH);

            JPanel text = new JPanel(new GridLayout(0, 1, 0, 0));
            text.setOpaque(false);
            JPanel textHolder = new JPanel(new BorderLayout());
            textHolder.setOpaque(false);
            textHolder.add(text, BorderLayout.NORTH);
            JLabel name = new JLabel(item.getName());
            name.setFont(Theme.font(Font.BOLD, 14f));
            JLabel category = new JLabel(item.getCategoryName());
            category.setForeground(Theme.MUTED);
            category.setFont(Theme.font(Font.PLAIN, 12f));
            text.add(name);
            text.add(category);
            add(textHolder, BorderLayout.CENTER);

            JPanel bottom = new JPanel(new BorderLayout(6, 0));
            bottom.setOpaque(false);
            JLabel price = new JLabel(UiSupport.peso(item.getUnitPrice()));
            price.setFont(Theme.font(Font.BOLD, 15f));
            bottom.add(price, BorderLayout.WEST);
            bottom.add(stockPill(item), BorderLayout.EAST);
            add(bottom, BorderLayout.SOUTH);

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    if (!item.getId().equals(selectedMenuId)) {
                        outline(Theme.BLUE_DARK);
                    }
                }
                @Override public void mouseExited(MouseEvent e) {
                    if (!item.getId().equals(selectedMenuId) && !contains(e.getPoint())) {
                        outline(null);
                    }
                }
                @Override public void mouseClicked(MouseEvent e) {
                    selectedMenuId = item.getId();
                    updateCardOutlines();
                    updateActions();
                    if (e.getClickCount() == 2) {
                        addSelectedItem();
                    }
                }
            });
        }
    }

    private void updateCardOutlines() {
        for (Component component : cardGrid.getComponents()) {
            if (component instanceof MenuCard card) {
                card.outline(card.itemId.equals(selectedMenuId) ? Theme.ORANGE : null);
            }
        }
    }

    /** Round dish picture: photos/menu/<dish name>.png when present, otherwise the dish initials. */
    private static final class DishPicture extends JComponent {
        private static final int SIZE = 84;
        private final String name;

        DishPicture(String name) {
            this.name = name;
            setPreferredSize(new Dimension(SIZE, SIZE));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            BufferedImage photo = Photos.menuItem(name);
            if (photo != null) {
                g2.clip(new Ellipse2D.Float(0, 0, SIZE, SIZE));
                double scale = Math.max(SIZE / (double) photo.getWidth(), SIZE / (double) photo.getHeight());
                int w = (int) Math.ceil(photo.getWidth() * scale);
                int h = (int) Math.ceil(photo.getHeight() * scale);
                g2.drawImage(photo, (SIZE - w) / 2, (SIZE - h) / 2, w, h, null);
            } else {
                g2.setColor(Theme.ORANGE_TINT);
                g2.fillOval(0, 0, SIZE, SIZE);
                g2.setColor(Theme.ORANGE);
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawOval(5, 5, SIZE - 10, SIZE - 10);
                g2.setFont(Theme.font(Font.BOLD, 24f));
                g2.setColor(Theme.ORANGE_DARK);
                String initials = Avatar.initials(name);
                int width = g2.getFontMetrics().stringWidth(initials);
                g2.drawString(initials, (SIZE - width) / 2,
                        (SIZE - g2.getFontMetrics().getHeight()) / 2 + g2.getFontMetrics().getAscent());
            }
            g2.dispose();
        }
    }

    /** Green when in stock, amber when 5 or fewer are left, red when sold out. */
    private static StatusPill stockPill(MenuItem item) {
        int left = item.getStockQuantity();
        if (left <= 0) {
            return new StatusPill("Sold out", Theme.Tone.BAD);
        }
        return new StatusPill(left + " left", left <= 5 ? Theme.Tone.WAIT : Theme.Tone.GOOD);
    }

    // ---------- Selected order (bottom right) ----------

    private JComponent orderSection() {
        RoundedPanel panel = new RoundedPanel(new BorderLayout(0, 10), Theme.WHITE, 18).padding(14, 14, 14, 14);
        panel.setPreferredSize(new Dimension(380, 10));
        orderTitle.setFont(Theme.font(Font.BOLD, 15f));
        panel.add(orderTitle, BorderLayout.NORTH);

        UiSupport.columnWidths(items, 190, 50, 90);
        panel.add(UiSupport.tablePane(items), BorderLayout.CENTER);

        JPanel controls = new JPanel(new BorderLayout(0, 8));
        controls.setOpaque(false);
        JPanel quantityRow = new JPanel(new BorderLayout(10, 0));
        quantityRow.setOpaque(false);
        JLabel quantityLabel = new JLabel("Quantity");
        quantityLabel.setFont(Theme.font(Font.BOLD, 13f));
        quantityRow.add(quantityLabel, BorderLayout.WEST);
        quantityRow.add(quantity, BorderLayout.CENTER);
        quantityRow.add(add, BorderLayout.EAST);
        controls.add(quantityRow, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new GridLayout(0, 2, 8, 8));
        buttons.setOpaque(false);
        buttons.add(update);
        buttons.add(remove);
        buttons.add(send);
        buttons.add(recall);
        buttons.add(serve);
        buttons.add(cancel);
        controls.add(buttons, BorderLayout.CENTER);
        controls.add(hint, BorderLayout.SOUTH);
        panel.add(controls, BorderLayout.SOUTH);
        return panel;
    }

    // ---------- Data ----------

    @Override
    public void refreshData() {
        String selected = UiSupport.selectedOrNull(orders);
        state = services.repository().snapshot();
        workflow.refreshAccess();
        orderModel.setRowCount(0);
        for (Order order : state.getOrders()) {
            DiningSession visit = state.getSessionOrThrow(order.getSessionId());
            if (!history.isSelected() && !visit.isOpen()) {
                continue;
            }
            String progress = visit.isOpen() ? UiSupport.status(order.getStatus())
                    : order.getStatus() == OrderStatus.CANCELLED ? "Cancelled" : "Paid";
            orderModel.addRow(new Object[]{UiSupport.orderChoice(state, order.getId()),
                    UiSupport.tableName(state, visit.getTableId()),
                    state.getCustomerOrThrow(visit.getCustomerId()).getFullName(),
                    progress, order.getSubtotal(), itemSummary(order)});
        }
        UiSupport.restoreSelection(orders, selected);
        if (orders.getSelectedRow() < 0 && orders.getRowCount() == 1) {
            orders.setRowSelectionInterval(0, 0);
        }

        menuItems.clear();
        state.getMenuItems().stream().filter(MenuItem::isActive)
                .sorted(Comparator.comparing(MenuItem::getName)).forEach(menuItems::add);
        if (selectedMenuId != null && menuItems.stream().noneMatch(i -> i.getId().equals(selectedMenuId))) {
            selectedMenuId = null;
        }
        rebuildChips();
        rebuildCards();
        refreshItems();
    }

    @Override
    public void focusOrder(String orderId) {
        if (!UiSupport.restoreSelection(orders, orderId) && !history.isSelected()) {
            history.setSelected(true);
            refreshData();
            UiSupport.restoreSelection(orders, orderId);
        }
    }

    private static String itemSummary(Order order) {
        return order.getItems().stream()
                .map(item -> item.getQuantity() + " × " + item.getItemName())
                .reduce((left, right) -> left + ", " + right)
                .orElse("—");
    }

    private void refreshItems() {
        if (state == null) {
            return;
        }
        String selected = UiSupport.selectedOrNull(items);
        itemModel.setRowCount(0);
        String id = UiSupport.selectedOrNull(orders);
        if (id != null) {
            Order order = state.getOrderOrThrow(id);
            DiningSession visit = state.getSessionOrThrow(order.getSessionId());
            orderTitle.setText("Order " + OrderReferences.display(state, id) + "  ·  "
                    + UiSupport.tableName(state, visit.getTableId()) + "  ·  "
                    + state.getCustomerOrThrow(visit.getCustomerId()).getFirstName());
            for (OrderItem item : order.getItems()) {
                itemModel.addRow(new Object[]{new Choice(item.getMenuItemId(), item.getItemName()),
                        item.getQuantity(), item.getLineTotal()});
            }
        } else {
            orderTitle.setText("No order selected");
        }
        UiSupport.restoreSelection(items, selected);
        updateActions();
    }

    private void updateActions() {
        if (state == null) {
            return;
        }
        String id = UiSupport.selectedOrNull(orders);
        Order order = id == null ? null : state.getOrderOrThrow(id);
        boolean draft = order != null && order.getStatus() == OrderStatus.DRAFT;
        add.setEnabled(draft && selectedMenuId != null
                && state.getMenuItemOrThrow(selectedMenuId).getStockQuantity() > 0);
        update.setEnabled(draft && items.getSelectedRow() >= 0);
        remove.setEnabled(draft && items.getSelectedRow() >= 0);
        quantity.setEnabled(draft);
        send.setEnabled(draft && !order.getItems().isEmpty());
        serve.setEnabled(order != null && order.getStatus() == OrderStatus.READY);
        recall.setEnabled(order != null && order.getStatus() == OrderStatus.CONFIRMED);
        cancel.setEnabled(draft || order != null && order.getStatus() == OrderStatus.CONFIRMED
                && currentUser.get().roleName().equals("Manager"));
        String text;
        if (order == null) {
            text = orders.getRowCount() == 0 ? "No active orders. Seat guests in Guests & Tables first."
                    : "Select an order above to begin.";
        } else {
            text = switch (order.getStatus()) {
                case DRAFT -> selectedMenuId == null ? "Pick a dish on the left, choose the quantity, then Add to order."
                        : "Add the selected dish, or select an ordered item to change it.";
                case CONFIRMED -> "Sent to the kitchen. Made a mistake? Click Take back order before cooking starts.";
                case PREPARING -> "The kitchen is preparing this order.";
                case READY -> "Food is ready. Bring it to the table, then click Mark served.";
                case SERVED -> state.getSessionOrThrow(order.getSessionId()).isOpen()
                        ? "Served. Next: go to Billing to take payment." : "Paid. The table is free again.";
                case CANCELLED -> "Cancelled. The table was freed.";
            };
        }
        UiSupport.setHint(hint, text);
    }

    /**
     * Adds the selected dish. If it is already in the order, asks whether to add more or change the
     * amount; if there is not enough stock, offers what is left instead of just failing.
     */
    private void addSelectedItem() {
        if (selectedMenuId == null) {
            UiSupport.perform(this, () -> { throw new IllegalArgumentException("Click a dish on the menu first."); }, () -> { });
            return;
        }
        String orderId = UiSupport.selectedOrNull(orders);
        if (orderId == null) {
            UiSupport.perform(this, () -> { throw new IllegalArgumentException("Select an order above first."); }, () -> { });
            return;
        }
        MenuItem dish = state.getMenuItemOrThrow(selectedMenuId);
        int wanted = quantity.requireInt("quantity");
        OrderItem already = state.getOrderOrThrow(orderId).getItems().stream()
                .filter(item -> item.getMenuItemId().equals(dish.getId())).findFirst().orElse(null);
        int newTotal = wanted;
        if (already != null) {
            int have = already.getQuantity();
            int choice = Dialogs.choose(this, "Already in the order", dish.getName() + " is already in this order",
                    "This order already has " + have + " × " + dish.getName() + ". What would you like to do?",
                    "Cancel", "Change it to " + wanted, "Add " + wanted + " more (" + (have + wanted) + " total)");
            if (choice <= 0) {
                return;
            }
            newTotal = choice == 2 ? have + wanted : wanted;
            if (newTotal == have) {
                return;
            }
        }
        int stock = dish.getStockQuantity();
        if (newTotal > stock) {
            if (stock <= 0) {
                Dialogs.choose(this, "Sold out", dish.getName() + " is sold out",
                        "There is no " + dish.getName() + " left. Please pick another dish.", "OK");
                return;
            }
            int choice = Dialogs.choose(this, "Not enough stock", "Only " + stock + " left",
                    "You asked for " + newTotal + " × " + dish.getName() + ", but only " + stock + " are left.",
                    "Change quantity", "Use " + stock);
            if (choice != 1) {
                quantity.requestFocusInWindow();
                return;
            }
            newTotal = stock;
        }
        int finalTotal = newTotal;
        action(() -> {
            if (already == null) {
                services.orders().addItem(actor(), orderId, dish.getId(), finalTotal);
            } else {
                services.orders().updateItemQuantity(actor(), orderId, dish.getId(), finalTotal);
            }
        });
    }

    private void removeSelectedItem() {
        String itemId = UiSupport.selectedOrNull(items);
        if (itemId == null) {
            return;
        }
        String name = String.valueOf(items.getValueAt(items.getSelectedRow(), 0));
        int qty = ((Number) items.getValueAt(items.getSelectedRow(), 1)).intValue();
        if (Dialogs.choose(this, "Remove item", "Remove " + name + "?",
                "Take " + qty + " × " + name + " off this order?", "Keep it", "Remove") == 1) {
            action(() -> services.orders().removeItem(actor(), orderId(), itemId));
        }
    }

    /** Shows everything that will be sent, so mistakes can be fixed before the kitchen sees them. */
    private void reviewAndSend() {
        String id = UiSupport.selectedOrNull(orders);
        if (id == null) {
            return;
        }
        Order order = state.getOrderOrThrow(id);
        StringBuilder text = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            text.append(item.getQuantity()).append(" × ").append(item.getItemName())
                    .append("   ").append(UiSupport.peso(item.getLineTotal())).append('\n');
        }
        text.append("\nTotal: ").append(UiSupport.peso(order.getSubtotal()))
                .append("\n\nIs everything correct? You can still take it back until the kitchen starts cooking.");
        int choice = Dialogs.choose(this, "Send to kitchen", "Send " + OrderReferences.display(state, id) + " to the kitchen?",
                text.toString(), "Go back and edit", "Send to kitchen");
        if (choice == 1) {
            action(() -> services.orders().confirmOrder(actor(), id));
        }
    }

    private void recallFromKitchen() {
        String id = UiSupport.selectedOrNull(orders);
        if (id == null) {
            return;
        }
        int choice = Dialogs.choose(this, "Take back from kitchen", "Take " + OrderReferences.display(state, id) + " back?",
                "The order goes back to \"Taking order\" so you can fix the items, then send it again."
                        + " The kitchen has not started cooking it yet.", "Leave it in the kitchen", "Take it back");
        if (choice == 1) {
            action(() -> services.orders().recallOrder(actor(), id));
        }
    }

    private void markServed() {
        String id = UiSupport.selectedOrNull(orders);
        if (id == null) {
            return;
        }
        String table = String.valueOf(orders.getValueAt(orders.getSelectedRow(), 1));
        int choice = Dialogs.choose(this, "Mark served", "Did the food reach " + table + "?",
                "Mark " + OrderReferences.display(state, id) + " as served. It then moves to Billing.",
                "Not yet", "Yes, mark served");
        if (choice == 1) {
            action(() -> services.kitchen().markServed(actor(), id));
        }
    }

    private void action(Runnable work) {
        UiSupport.perform(this, work, this::refreshData);
    }

    private String actor() {
        return currentUser.get().employeeId();
    }

    private String orderId() {
        return UiSupport.selectedId(orders, 0, "order");
    }
}
