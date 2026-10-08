package ui;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.text.JTextComponent;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.geom.RoundRectangle2D;

/**
 * Rounded white text fields with an orange outline when focused.
 * Placeholder text: field.putClientProperty("Kouzina.placeholder", "Enter your username");
 */
public class RoundedTextFieldUI extends BasicTextFieldUI {
    private final FocusListener repaintOnFocus = new FocusAdapter() {
        @Override public void focusGained(FocusEvent e) { getComponent().repaint(); }
        @Override public void focusLost(FocusEvent e) { getComponent().repaint(); }
    };

    public static ComponentUI createUI(JComponent c) {
        return new RoundedTextFieldUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        getComponent().setOpaque(false);
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        getComponent().addFocusListener(repaintOnFocus);
    }

    @Override
    protected void uninstallListeners() {
        getComponent().removeFocusListener(repaintOnFocus);
        super.uninstallListeners();
    }

    @Override
    protected void paintSafely(Graphics g) {
        paintField(g, getComponent());
        super.paintSafely(g);
        paintPlaceholder(g, getComponent());
    }

    static void paintField(Graphics g, JTextComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float arc = 12f;
        g2.setColor(c.isEditable() && c.isEnabled() ? c.getBackground() : Theme.PAPER);
        g2.fill(new RoundRectangle2D.Float(0, 0, c.getWidth(), c.getHeight(), arc, arc));
        g2.setColor(c.hasFocus() ? Theme.ORANGE : Theme.LINE);
        g2.setStroke(new BasicStroke(c.hasFocus() ? 1.6f : 1.1f));
        g2.draw(new RoundRectangle2D.Float(0.8f, 0.8f, c.getWidth() - 1.6f, c.getHeight() - 1.6f, arc, arc));
        g2.dispose();
    }

    static void paintPlaceholder(Graphics g, JTextComponent c) {
        Object placeholder = c.getClientProperty(Theme.PLACEHOLDER);
        if (!(placeholder instanceof String text) || c.getDocument().getLength() > 0) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Theme.MUTED);
        g2.setFont(c.getFont());
        Insets insets = c.getInsets();
        int baseline = (c.getHeight() - g2.getFontMetrics().getHeight()) / 2 + g2.getFontMetrics().getAscent();
        g2.drawString(text, insets.left, baseline);
        g2.dispose();
    }
}
