package ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Step bar shown on Guests & Tables, Orders, Kitchen, and Billing:
 *
 *   [← Back to Guests & Tables]   1 Guests & Tables › 2 Orders › 3 Kitchen › 4 Billing   [Go to Kitchen →]
 *
 * Every step can be clicked, so nobody has to go back to the Dashboard to find a screen.
 * The selected order travels along, so the next screen opens on the same order.
 */
final class WorkflowBar extends RoundedPanel {
    /** The four steps of a guest's visit, in order. */
    static final String[][] STEPS = {
            {KouzinaFrame.RESERVATIONS, "Guests & Tables"},
            {KouzinaFrame.ORDERING, "Orders"},
            {KouzinaFrame.KITCHEN, "Kitchen"},
            {KouzinaFrame.BILLING, "Billing"}};

    private final Navigator navigator;
    private final int current;
    private final Supplier<String> selectedOrder;
    private final JButton back = Theme.outline(new JButton());
    private final JButton next = new JButton();
    private final List<StepChip> chips = new ArrayList<>();
    private final JPanel steps = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
    /** On small screens only the current step shows its name; the others show their number. */
    private boolean compact;

    /**
     * @param screen        which step this bar is on (e.g. KouzinaFrame.ORDERING)
     * @param selectedOrder the order selected on this screen, or null (passed to the next screen)
     */
    WorkflowBar(Navigator navigator, String screen, Supplier<String> selectedOrder) {
        super(new BorderLayout(12, 0), Theme.WHITE, 18);
        this.navigator = navigator;
        this.selectedOrder = selectedOrder;
        this.current = indexOf(screen);
        padding(8, 10, 8, 10);

        steps.setOpaque(false);
        for (int index = 0; index < STEPS.length; index++) {
            if (index > 0) {
                JLabel arrow = new JLabel("›");
                arrow.setFont(Theme.font(Font.BOLD, 18f));
                arrow.setForeground(Theme.MUTED);
                arrow.setBorder(BorderFactory.createEmptyBorder(0, 3, 2, 3));
                steps.add(arrow);
            }
            StepChip chip = new StepChip(index);
            chips.add(chip);
            steps.add(chip);
        }
        add(steps, BorderLayout.CENTER);

        back.setIcon(Icons.icon("back", 16, Theme.TEXT));
        back.setIconTextGap(8);
        back.addActionListener(event -> go(previousScreen()));
        add(back, BorderLayout.WEST);

        next.setIcon(Icons.icon("arrow", 16, Theme.TEXT));
        next.setHorizontalTextPosition(JButton.LEFT);
        next.setIconTextGap(8);
        next.addActionListener(event -> go(STEPS[nextIndex()][0]));
        add(next, BorderLayout.EAST);
        refreshAccess();
    }

    /** Updates which steps this person can open. Call when the screen is shown. */
    void refreshAccess() {
        String previous = previousScreen();
        back.setText(previous.equals(KouzinaFrame.DASHBOARD) ? "Dashboard" : "Back to " + label(previous));
        back.setVisible(navigator.canOpen(previous));
        int nextIndex = nextIndex();
        String target = STEPS[nextIndex][0];
        next.setText(nextIndex == 0 ? "Seat next guests" : "Go to " + STEPS[nextIndex][1]);
        next.setVisible(navigator.canOpen(target));
        chips.forEach(StepChip::repaint);
    }

    @Override
    public void doLayout() {
        boolean wasCompact = compact;
        compact = getWidth() > 0 && getWidth() < fullWidth();
        if (compact != wasCompact) {
            steps.invalidate();
        }
        super.doLayout();
    }

    /** Width needed to show every step name (measured directly, not from cached sizes). */
    private int fullWidth() {
        int width = back.isVisible() ? back.getPreferredSize().width : 0;
        width += next.isVisible() ? next.getPreferredSize().width : 0;
        for (Component child : steps.getComponents()) {
            width += (child instanceof StepChip chip ? chip.fullWidth() : child.getPreferredSize().width) + 2;
        }
        return width + 60;
    }

    private void go(String screen) {
        navigator.open(screen, selectedOrder == null ? null : selectedOrder.get());
    }

    private String previousScreen() {
        return current == 0 ? KouzinaFrame.DASHBOARD : STEPS[current - 1][0];
    }

    /** After Billing comes Guests & Tables again (the table is free for the next party). */
    private int nextIndex() {
        return (current + 1) % STEPS.length;
    }

    private static int indexOf(String screen) {
        for (int index = 0; index < STEPS.length; index++) {
            if (STEPS[index][0].equals(screen)) {
                return index;
            }
        }
        throw new IllegalArgumentException("Not a workflow step: " + screen);
    }

    private static String label(String screen) {
        return STEPS[indexOf(screen)][1];
    }

    /** One clickable step: a numbered circle and the step name. */
    private final class StepChip extends JComponent {
        private final int index;
        private boolean hover;

        StepChip(int index) {
            this.index = index;
            setFont(Theme.font(Font.BOLD, 13f));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Open " + STEPS[index][1]);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) {
                    if (index != current && allowed()) {
                        go(STEPS[index][0]);
                    }
                }
            });
        }

        private boolean allowed() {
            return navigator.canOpen(STEPS[index][0]);
        }

        @Override
        public Dimension getPreferredSize() {
            if (compact && index != current) {
                return new Dimension(38, 34);
            }
            return new Dimension(fullWidth(), 34);
        }

        int fullWidth() {
            return getFontMetrics(getFont()).stringWidth(STEPS[index][1]) + 50;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            boolean here = index == current;
            boolean done = index < current;
            boolean enabled = allowed();
            int h = getHeight();
            if (here) {
                g2.setColor(Theme.ORANGE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), h, h, h));
            } else if (hover && enabled) {
                g2.setColor(Theme.BLUE_TINT);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), h, h, h));
            }
            int circle = 22;
            int cy = (h - circle) / 2;
            g2.setColor(here ? Theme.WHITE : done ? Theme.SAGE : enabled ? Theme.ORANGE_TINT : Theme.LINE);
            g2.fillOval(8, cy, circle, circle);
            g2.setColor(here ? Theme.ORANGE_DARK : done ? Theme.WHITE : enabled ? Theme.ORANGE_DARK : Theme.MUTED);
            g2.setFont(Theme.font(Font.BOLD, 12f));
            String number = String.valueOf(index + 1);
            FontMetrics small = g2.getFontMetrics();
            g2.drawString(number, 8 + (circle - small.stringWidth(number)) / 2,
                    cy + (circle - small.getHeight()) / 2 + small.getAscent());
            if (compact && !here) {
                g2.dispose();
                return;
            }
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(enabled ? Theme.TEXT : Theme.MUTED);
            g2.drawString(STEPS[index][1], 38, (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }
}
