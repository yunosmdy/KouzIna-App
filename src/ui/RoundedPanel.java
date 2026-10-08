package ui;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/** A panel with rounded corners. Used for cards, the content area, and the login field boxes. */
public class RoundedPanel extends JPanel {
    private int arc;
    private Color outline;

    public RoundedPanel(LayoutManager layout, Color background, int arc) {
        super(layout);
        this.arc = arc;
        setBackground(background);
        setOpaque(false);
    }

    public RoundedPanel padding(int top, int left, int bottom, int right) {
        setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        return this;
    }

    public RoundedPanel outline(Color color) {
        this.outline = color;
        repaint();
        return this;
    }

    public void setArc(int arc) {
        this.arc = arc;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc, arc));
        if (outline != null) {
            g2.setColor(outline);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new RoundRectangle2D.Float(0.75f, 0.75f, getWidth() - 1.5f, getHeight() - 1.5f, arc, arc));
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
