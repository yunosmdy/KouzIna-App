package ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * White card that stacks a form top-to-bottom: heading, label-above-field rows, and buttons.
 * Example:
 *   FormCard card = new FormCard("New reservation");
 *   card.field("Customer", customerBox);
 *   card.buttons(createButton);
 */
public final class FormCard extends RoundedPanel {
    public static final int WIDTH = 340;

    public FormCard(String heading) {
        super(null, Theme.WHITE, 20);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        padding(18, 18, 18, 18);
        JLabel title = new JLabel(heading);
        title.setFont(Theme.font(Font.BOLD, 17f));
        add(left(title));
        add(Box.createVerticalStrut(12));
    }

    /** Small grey section heading inside the card, e.g. "For the selected reservation". */
    public FormCard section(String text) {
        add(Box.createVerticalStrut(14));
        JComponent line = new JComponent() {
            @Override protected void paintComponent(java.awt.Graphics g) {
                g.setColor(Theme.LINE);
                g.fillRect(0, getHeight() / 2, getWidth(), 1);
            }
        };
        line.setPreferredSize(new Dimension(10, 9));
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 9));
        add(left(line));
        add(Box.createVerticalStrut(6));
        JLabel label = new JLabel(text.toUpperCase());
        label.setFont(Theme.font(Font.BOLD, 11.5f));
        label.setForeground(Theme.MUTED);
        add(left(label));
        add(Box.createVerticalStrut(8));
        return this;
    }

    public FormCard note(String text) {
        JLabel label = new JLabel("<html><div style='width:" + (WIDTH - 130) + "px'>" + text + "</div></html>");
        label.setFont(Theme.font(Font.PLAIN, 12.5f));
        label.setForeground(Theme.MUTED);
        add(left(stretch(label)));
        add(Box.createVerticalStrut(8));
        return this;
    }

    /** A grey hint whose text changes (create it with UiSupport.hint(FormCard.WIDTH - 130)). */
    public FormCard hint(JLabel hint) {
        add(left(stretch(hint)));
        add(Box.createVerticalStrut(8));
        return this;
    }

    public FormCard field(String label, JComponent input) {
        JLabel text = new JLabel(label);
        text.setFont(Theme.font(Font.BOLD, 12.5f));
        add(left(text));
        add(Box.createVerticalStrut(5));
        add(left(stretch(input)));
        add(Box.createVerticalStrut(9));
        return this;
    }

    /** Buttons in rows of two (one button takes the full width). */
    public FormCard buttons(JButton... buttons) {
        JPanel row = new JPanel(new GridLayout(0, buttons.length == 1 ? 1 : 2, 8, 8));
        row.setOpaque(false);
        for (JButton button : buttons) {
            row.add(button);
        }
        add(left(stretch(row)));
        add(Box.createVerticalStrut(4));
        return this;
    }

    /** Puts the card at the top of a fixed-width column so it does not stretch. */
    public JComponent inColumn() {
        JPanel column = new JPanel(new java.awt.BorderLayout());
        column.setOpaque(false);
        column.add(this, java.awt.BorderLayout.NORTH);
        column.setPreferredSize(new Dimension(WIDTH, 10));
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(column);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.setPreferredSize(new Dimension(WIDTH + 12, 10));
        return scroll;
    }

    private static JComponent left(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }

    /**
     * Lets the component use the full card width. The height is not fixed, so text that
     * changes later (hints, totals, payment buttons) is never cut off. The card always gets
     * its preferred height (see inColumn), so nothing is stretched taller than needed.
     */
    private static JComponent stretch(JComponent component) {
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return component;
    }
}
