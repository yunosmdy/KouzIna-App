package ui;

import domain.enums.OrderStatus;
import persistence.AppState;
import service.OrderReferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.UnaryOperator;

/** Small helpers shared by all screens: headings, styled tables, formatting, and messages. */
final class UiSupport {
    /** How dates and times appear in tables, e.g. "Oct 9, 6:30 PM". */
    static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("MMM d, h:mm a");
    /** Plain format used in reports. */
    static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** Table cells: light stripes. */
    static final Color STRIPE = new Color(0xFAF8F4);

    private UiSupport() {
    }

    // ---------- Headings ----------

    static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(Font.BOLD, 26f));
        label.setForeground(Theme.TEXT);
        return label;
    }

    static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Theme.MUTED);
        label.setFont(Theme.font(Font.PLAIN, 13f));
        return label;
    }

    /** Big title with one line of help under it, and optional buttons on the right. */
    static JPanel heading(String title, String help, JComponent... right) {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setOpaque(false);
        JPanel words = new JPanel(new BorderLayout(0, 2));
        words.setOpaque(false);
        words.add(title(title), BorderLayout.NORTH);
        words.add(muted(help), BorderLayout.CENTER);
        panel.add(words, BorderLayout.CENTER);
        if (right.length > 0) {
            JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 4));
            buttons.setOpaque(false);
            for (JComponent component : right) {
                buttons.add(component);
            }
            panel.add(buttons, BorderLayout.EAST);
        }
        return panel;
    }

    /** Blue "Refresh" button with an icon. */
    static JButton refreshButton(Runnable action) {
        JButton button = Theme.subtle(new JButton("Refresh"));
        button.setIcon(Icons.icon("refresh", 15, Theme.TEXT));
        button.setIconTextGap(6);
        button.addActionListener(event -> action.run());
        return button;
    }

    /** White rounded card with a small bold heading, used around tables. */
    static JComponent card(String heading, JComponent content) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 8), Theme.WHITE, 18).padding(12, 12, 10, 12);
        if (heading != null) {
            JLabel label = new JLabel(heading);
            label.setFont(Theme.font(Font.BOLD, 14.5f));
            label.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
            card.add(label, BorderLayout.NORTH);
        }
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    /** A grey hint line that can wrap. */
    static JLabel hint() {
        JLabel label = new JLabel(" ");
        label.setForeground(Theme.MUTED);
        label.setFont(Theme.font(Font.PLAIN, 12.5f));
        return label;
    }

    /** Hint that wraps onto more lines when it is wider than the given number of pixels. */
    static JLabel hint(int wrapWidth) {
        JLabel label = hint();
        label.putClientProperty("Kouzina.wrap", wrapWidth);
        return label;
    }

    static void setHint(JLabel label, String text) {
        String safe = text.replace("&", "&amp;").replace("<", "&lt;");
        Object wrap = label.getClientProperty("Kouzina.wrap");
        label.setText(wrap instanceof Integer width
                ? "<html><div style='width:" + width + "px'>" + safe + "</div></html>"
                : "<html>" + safe + "</html>");
    }

    // ---------- Tables ----------

    /** Table model that cannot be typed into and sorts numbers and dates properly. */
    static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                for (int row = 0; row < getRowCount(); row++) {
                    Object value = getValueAt(row, column);
                    if (value != null) {
                        return value instanceof Comparable<?> ? value.getClass() : Object.class;
                    }
                }
                return Object.class;
            }
        };
    }

    /**
     * Styles a table the Kóuz 'Inà way: white rounded card, blue header, striped rows,
     * colored status pills, ₱ amounts, and friendly dates. Only one row can be selected.
     */
    static JComponent tablePane(JTable table) {
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(34);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        StripedRenderer striped = new StripedRenderer();
        table.setDefaultRenderer(Object.class, striped);
        table.setDefaultRenderer(Number.class, striped);
        table.getTableHeader().setDefaultRenderer(new HeaderRenderer());
        table.getTableHeader().setReorderingAllowed(false);
        StatusPill.Renderer statusRenderer = new StatusPill.Renderer();
        for (int column = 0; column < table.getColumnCount(); column++) {
            String name = table.getColumnName(column).toLowerCase(Locale.ROOT);
            if (name.contains("status") || name.equals("progress") || name.equals("availability")
                    || name.equals("active") || name.equals("available")) {
                table.getColumnModel().getColumn(column).setCellRenderer(statusRenderer);
            }
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setBackground(Theme.WHITE);
        // On small screens the table scrolls sideways instead of squeezing every column
        scroll.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent event) {
                fitColumns(table, scroll.getViewport().getWidth());
            }
        });
        RoundedPanel card = new RoundedPanel(new BorderLayout(), Theme.WHITE, 18).padding(8, 8, 8, 8);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ---------- Scrolling ----------

    /**
     * Wraps a whole screen so it scrolls when the window is smaller than the screen needs
     * (small laptops, zoomed-in Windows displays). On big windows it simply fills the space.
     * minHeight = -1 uses the screen's own preferred height (for screens without inner lists).
     */
    static JScrollPane scrollPage(JComponent content, int minWidth, int minHeight) {
        JScrollPane scroll = new JScrollPane(new ScrollPage(content, minWidth, minHeight));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(28);
        scroll.getHorizontalScrollBar().setUnitIncrement(28);
        return scroll;
    }

    /** The view inside scrollPage: fills the window, but never gets smaller than its minimum. */
    private static final class ScrollPage extends JPanel implements javax.swing.Scrollable {
        private final JComponent content;
        private final int minWidth;
        private final int minHeight;

        ScrollPage(JComponent content, int minWidth, int minHeight) {
            super(new BorderLayout());
            this.content = content;
            this.minWidth = minWidth;
            this.minHeight = minHeight;
            setOpaque(false);
            add(content, BorderLayout.CENTER);
        }

        private int neededHeight() {
            return minHeight >= 0 ? minHeight : content.getPreferredSize().height;
        }

        @Override
        public Dimension getPreferredSize() {
            java.awt.Container viewport = getParent();
            int width = viewport == null ? minWidth : Math.max(minWidth, viewport.getWidth());
            int height = viewport == null ? neededHeight() : Math.max(neededHeight(), viewport.getHeight());
            return new Dimension(width, height);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(java.awt.Rectangle r, int o, int d) { return 28; }
        @Override public int getScrollableBlockIncrement(java.awt.Rectangle r, int o, int d) {
            return o == javax.swing.SwingConstants.VERTICAL ? r.height - 40 : r.width - 40;
        }
        @Override public boolean getScrollableTracksViewportWidth() {
            return getParent() != null && getParent().getWidth() >= minWidth;
        }
        @Override public boolean getScrollableTracksViewportHeight() {
            return getParent() != null && getParent().getHeight() >= neededHeight();
        }
    }

    /**
     * Mouse wheel fix for lists inside a scrolling page: a list (table, menu cards, form) scrolls
     * itself first, and when it is already at the top/bottom (or has nothing to scroll) the wheel
     * moves the page behind it. Without this, the page gets "stuck" while the mouse is over a table.
     */
    static void installWheelForwarding(Component root) {
        if (root instanceof JScrollPane pane && pane.getClientProperty("Kouzina.wheel") == null) {
            pane.putClientProperty("Kouzina.wheel", Boolean.TRUE);
            java.awt.event.MouseWheelListener[] originals = pane.getMouseWheelListeners();
            for (java.awt.event.MouseWheelListener listener : originals) {
                pane.removeMouseWheelListener(listener);
            }
            pane.addMouseWheelListener(event -> {
                javax.swing.JScrollBar bar = event.isShiftDown()
                        ? pane.getHorizontalScrollBar() : pane.getVerticalScrollBar();
                boolean down = event.getWheelRotation() > 0 || event.getPreciseWheelRotation() > 0;
                boolean canMove = bar != null && bar.isVisible() && (down
                        ? bar.getValue() + bar.getVisibleAmount() < bar.getMaximum()
                        : bar.getValue() > bar.getMinimum());
                JScrollPane parent = (JScrollPane) javax.swing.SwingUtilities.getAncestorOfClass(JScrollPane.class, pane);
                if (canMove || parent == null) {
                    for (java.awt.event.MouseWheelListener listener : originals) {
                        listener.mouseWheelMoved(event);
                    }
                } else {
                    parent.dispatchEvent(javax.swing.SwingUtilities.convertMouseEvent(pane, event, parent));
                }
            });
        }
        if (root instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                installWheelForwarding(child);
            }
        }
    }

    /** Columns fill the width when there is room; otherwise they keep their size and scroll. */
    private static void fitColumns(JTable table, int available) {
        int wanted = 0;
        for (int i = 0; i < table.getColumnCount(); i++) {
            wanted += table.getColumnModel().getColumn(i).getPreferredWidth();
        }
        boolean narrow = available > 0 && available < wanted;
        int mode = narrow ? JTable.AUTO_RESIZE_OFF : JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS;
        if (table.getAutoResizeMode() != mode) {
            table.setAutoResizeMode(mode);
            if (narrow) {
                for (int i = 0; i < table.getColumnCount(); i++) {
                    var column = table.getColumnModel().getColumn(i);
                    column.setWidth(column.getPreferredWidth());
                }
            }
        }
    }

    /** Sets how wide each visible column starts (extra space is shared out). */
    static void columnWidths(JTable table, int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    /** ID behind the selected row (column holds a Choice), or a friendly error. */
    static String selectedId(JTable table, int modelColumn, String entityName) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            throw new IllegalArgumentException("Select a " + entityName + " first.");
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        Object value = table.getModel().getValueAt(modelRow, modelColumn);
        return value instanceof Choice choice ? choice.id() : String.valueOf(value);
    }

    static String selectedOrNull(JTable table) {
        return table.getSelectedRow() < 0 ? null : selectedId(table, 0, "row");
    }

    /** Selects the row whose first column has this ID. Returns false when it is not shown. */
    static boolean restoreSelection(JTable table, String id) {
        if (id == null) {
            return false;
        }
        for (int i = 0; i < table.getRowCount(); i++) {
            Object value = table.getValueAt(i, 0);
            String rowId = value instanceof Choice choice ? choice.id() : String.valueOf(value);
            if (rowId.equals(id)) {
                table.setRowSelectionInterval(i, i);
                table.scrollRectToVisible(table.getCellRect(i, 0, true));
                return true;
            }
        }
        return false;
    }

    // ---------- Restaurant wording ----------

    /** Order cell: shows the short reference (O-0001) but keeps the real order ID. */
    static Choice orderChoice(AppState state, String orderId) {
        return new Choice(orderId, OrderReferences.display(state, orderId));
    }

    static String tableName(AppState state, String id) {
        return id == null ? "Awaiting table" : "Table " + state.getTableOrThrow(id).getTableNumber();
    }

    /** Friendly status words: DRAFT -> "Taking order", CHECKED_IN -> "Checked in". */
    static String status(Object value) {
        if (value == OrderStatus.DRAFT) {
            return "Taking order";
        }
        if (value == OrderStatus.CONFIRMED) {
            return "Sent to kitchen";
        }
        String text = String.valueOf(value).toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /** 1234.5 -> "₱1,234.50" */
    static String peso(BigDecimal amount) {
        return "₱" + String.format("%,.2f", amount);
    }

    // ---------- Typing helpers ----------

    /**
     * "juan carlos" -> "Juan Carlos", "DELA CRUZ" -> "Dela Cruz", "o'neil" -> "O'Neil".
     * Every word (also after - or ') starts with a capital letter; extra spaces are removed.
     */
    static String formatName(String text) {
        String cleaned = text.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(cleaned.length());
        boolean capitalizeNext = true;
        for (char ch : cleaned.toCharArray()) {
            result.append(capitalizeNext ? Character.toUpperCase(ch) : ch);
            capitalizeNext = ch == ' ' || ch == '-' || ch == '\'';
        }
        return result.toString();
    }

    /**
     * Adds dashes to a phone number so it is easy to read.
     * "09213214114" -> "0921-321-4114", "639213214114" -> "+63 921-321-4114",
     * "8123456" -> "812-3456", "81234567" -> "8123-4567".
     */
    static String formatPhone(String text) {
        String digits = text.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return text.trim();
        }
        if (digits.length() == 7) {
            return digits.substring(0, 3) + "-" + digits.substring(3);
        }
        if (digits.length() == 8) {
            return digits.substring(0, 4) + "-" + digits.substring(4);
        }
        if (digits.startsWith("63") && digits.length() == 12) {
            return "+63 " + groupDigits(digits.substring(2), 3);
        }
        return groupDigits(digits, 4);
    }

    private static String groupDigits(String digits, int firstGroup) {
        StringBuilder result = new StringBuilder();
        int index = 0;
        for (int size : new int[]{firstGroup, 3}) {
            if (index >= digits.length()) {
                break;
            }
            int end = Math.min(index + size, digits.length());
            result.append(result.length() == 0 ? "" : "-").append(digits, index, end);
            index = end;
        }
        while (index < digits.length()) {
            int end = Math.min(index + 4, digits.length());
            result.append('-').append(digits, index, end);
            index = end;
        }
        return result.toString();
    }

    /** Re-formats the field's text whenever the user leaves the field. */
    static void formatOnLeave(JTextField field, UnaryOperator<String> formatter) {
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent event) {
                if (!field.getText().isBlank()) {
                    field.setText(formatter.apply(field.getText()));
                }
            }
        });
    }

    static void placeholder(JTextField field, String text) {
        field.putClientProperty(Theme.PLACEHOLDER, text);
    }

    // ---------- Messages ----------

    /** Runs the action; shows the error message in a friendly box when something is wrong. */
    static void perform(Component parent, Runnable action, Runnable afterSuccess) {
        try {
            action.run();
            afterSuccess.run();
        } catch (RuntimeException error) {
            String message = error.getMessage();
            JOptionPane.showMessageDialog(
                    parent,
                    message == null || message.isBlank() ? "The action could not be completed." : message,
                    Theme.BRAND,
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    static void showSuccess(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, Theme.BRAND, JOptionPane.INFORMATION_MESSAGE);
    }

    static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Please confirm",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION;
    }

    static int parseInteger(String value, String fieldName) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(fieldName + " must be a whole number.");
        }
    }

    static BigDecimal parseDecimal(String value, String fieldName) {
        try {
            return new BigDecimal(value.trim().replace(",", "").replace("₱", ""));
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(fieldName + " must be a number, for example 150 or 150.50.");
        }
    }

    static GridBagConstraints fieldConstraints(int column, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.insets = new Insets(5, 6, 5, 6);
        constraints.fill = column == 1 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
        constraints.weightx = column == 1 ? 1.0 : 0.0;
        constraints.anchor = GridBagConstraints.WEST;
        return constraints;
    }

    // ---------- Renderers ----------

    /** Padding, light stripes, blue selection, ₱ for amounts, friendly dates. */
    private static final class StripedRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean selected, boolean focused, int row, int column) {
            Object shown = value;
            if (value instanceof BigDecimal amount) {
                shown = peso(amount);
            } else if (value instanceof LocalDateTime time) {
                shown = time.format(DISPLAY_DATE_TIME);
            }
            super.getTableCellRendererComponent(table, shown, selected, false, row, column);
            setHorizontalAlignment(value instanceof Number ? RIGHT : LEFT);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            setToolTipText(shown == null ? null : String.valueOf(shown));
            if (!selected) {
                setBackground(row % 2 == 0 ? Theme.WHITE : STRIPE);
                setForeground(Theme.TEXT);
            }
            return this;
        }
    }

    /** Table header: flat blue with bold text. */
    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean selected, boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            setFont(Theme.font(Font.BOLD, 13f));
            setBackground(Theme.BLUE);
            setForeground(Theme.TEXT);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BLUE_TINT),
                    BorderFactory.createEmptyBorder(9, 12, 9, 12)));
            return this;
        }
    }
}
