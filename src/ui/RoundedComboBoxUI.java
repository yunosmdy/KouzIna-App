package ui;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/** Rounded white dropdowns with a simple chevron, matching the text fields. */
public class RoundedComboBoxUI extends BasicComboBoxUI {

    public static ComponentUI createUI(JComponent c) {
        return new RoundedComboBoxUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        comboBox.setOpaque(false);
        comboBox.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 4));
        comboBox.setMaximumRowCount(10);
        comboBox.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    }

    @Override
    protected JButton createArrowButton() {
        JButton button = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.MUTED);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                Path2D chevron = new Path2D.Float();
                chevron.moveTo(cx - 5, cy - 2);
                chevron.lineTo(cx, cy + 3);
                chevron.lineTo(cx + 5, cy - 2);
                g2.draw(chevron);
                g2.dispose();
            }
        };
        button.setContentAreaFilled(false);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setFocusable(false);
        button.setPreferredSize(new Dimension(28, 28));
        return button;
    }

    @Override
    protected ListCellRenderer<Object> createRenderer() {
        return new PaddedRenderer();
    }

    @Override
    protected ComboPopup createPopup() {
        BasicComboPopup popup = new BasicComboPopup(comboBox);
        popup.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        return popup;
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float arc = 12f;
        g2.setColor(comboBox.isEnabled() ? Theme.WHITE : Theme.PAPER);
        g2.fill(new RoundRectangle2D.Float(0, 0, c.getWidth(), c.getHeight(), arc, arc));
        boolean active = comboBox.hasFocus() || isPopupVisible(comboBox);
        g2.setColor(active ? Theme.ORANGE : Theme.LINE);
        g2.setStroke(new BasicStroke(active ? 1.6f : 1.1f));
        g2.draw(new RoundRectangle2D.Float(0.8f, 0.8f, c.getWidth() - 1.6f, c.getHeight() - 1.6f, arc, arc));
        g2.dispose();
        Rectangle value = rectangleForCurrentValue();
        paintCurrentValue(g, value, false);
    }

    @Override
    public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        comboBox.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { comboBox.repaint(); }
            @Override public void focusLost(java.awt.event.FocusEvent e) { comboBox.repaint(); }
        });
    }

    /** List rows with comfortable padding; the closed box shows no blue highlight. */
    private static final class PaddedRenderer extends DefaultListCellRenderer implements UIResource {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean selected, boolean focused) {
            super.getListCellRendererComponent(list, value, index, selected, false);
            setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
            if (index < 0) {
                setOpaque(false);
                setForeground(Theme.TEXT);
            } else {
                setOpaque(true);
                setBackground(selected ? Theme.BLUE_TINT : Theme.WHITE);
                setForeground(Theme.TEXT);
            }
            if (value == null && index < 0) {
                setText("Select…");
                setForeground(Theme.MUTED);
            }
            return this;
        }
    }
}
