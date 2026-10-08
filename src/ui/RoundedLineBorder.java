package ui;

import javax.swing.border.AbstractBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/** Thin rounded outline used for scroll panes and combo boxes. */
public final class RoundedLineBorder extends AbstractBorder {
    private final Color color;
    private final int arc;
    private final int padding;

    public RoundedLineBorder(Color color, int arc, int padding) {
        this.color = color;
        this.arc = arc;
        this.padding = padding;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, width - 1f, height - 1f, arc, arc));
        g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(padding, padding, padding, padding);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(padding, padding, padding, padding);
        return insets;
    }
}
