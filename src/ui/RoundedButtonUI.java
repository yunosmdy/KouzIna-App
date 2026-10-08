package ui;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.BorderUIResource;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/** Flat rounded buttons. Background color = button.setBackground(...); hover/pressed are darker. */
public class RoundedButtonUI extends BasicButtonUI {

    public static ComponentUI createUI(JComponent c) {
        return new RoundedButtonUI();
    }

    @Override
    protected void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setFocusPainted(false);
        if (b.getBorder() == null || b.getBorder() instanceof UIResource) {
            b.setBorder(new BorderUIResource.EmptyBorderUIResource(9, 18, 9, 18));
        }
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        ButtonModel model = b.getModel();
        if (b.isContentAreaFilled()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color bg = b.getBackground();
            if (!b.isEnabled()) {
                bg = mix(bg, Theme.PAPER, 0.55f);
            } else if (model.isPressed()) {
                bg = mix(bg, Color.BLACK, 0.16f);
            } else if (model.isRollover()) {
                bg = mix(bg, Color.BLACK, 0.07f);
            }
            float arc = arc(b);
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, b.getWidth(), b.getHeight(), arc, arc));
            Object outline = b.getClientProperty(Theme.OUTLINE);
            if (outline instanceof Color color) {
                g2.setColor(model.isRollover() ? Theme.ORANGE : color);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.6f, 0.6f, b.getWidth() - 1.2f, b.getHeight() - 1.2f, arc, arc));
            }
            g2.dispose();
        }
        Graphics2D text = (Graphics2D) g;
        text.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        super.paint(g, c);
    }

    @Override
    protected void paintButtonPressed(Graphics g, AbstractButton b) {
        // pressed color is handled in paint()
    }

    @Override
    protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect, Rectangle textRect, Rectangle iconRect) {
        // no dotted focus box
    }

    private static float arc(AbstractButton b) {
        Object value = b.getClientProperty(Theme.ARC);
        int arc = value instanceof Integer number ? number : 14;
        return Math.min(arc, b.getHeight());
    }

    static Color mix(Color a, Color b, float amount) {
        float keep = 1f - amount;
        return new Color(
                Math.round(a.getRed() * keep + b.getRed() * amount),
                Math.round(a.getGreen() * keep + b.getGreen() * amount),
                Math.round(a.getBlue() * keep + b.getBlue() * amount));
    }
}
