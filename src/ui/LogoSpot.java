package ui;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Reserved space for the restaurant logo.
 * To use a real logo: save it as "logo.png" in the photos folder.
 * Until then, a dashed placeholder box is drawn.
 */
public final class LogoSpot extends JComponent {
    private final int size;
    private final Color lineColor;
    private final Color backdrop;

    public LogoSpot(int size, Color lineColor) {
        this(size, lineColor, null);
    }

    /** backdrop: a circle drawn behind the logo so it stands out on a same-coloured background (null = none). */
    public LogoSpot(int size, Color lineColor, Color backdrop) {
        this.size = size;
        this.lineColor = lineColor;
        this.backdrop = backdrop;
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
        setToolTipText(Photos.logo() == null ? "Logo space — add logo.png to the photos folder" : null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;
        Image logo = Photos.logo();
        if (logo != null) {
            if (backdrop != null) {
                g2.setColor(backdrop);
                g2.fillOval(x, y, size, size);
                int inset = Math.round(size * 0.14f);
                g2.drawImage(logo, x + inset, y + inset, size - 2 * inset, size - 2 * inset, null);
            } else {
                g2.drawImage(logo, x, y, size, size, null);
            }
        } else {
            float arc = size * 0.28f;
            float stroke = Math.max(1.4f, size / 40f);
            g2.setColor(lineColor);
            g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    10f, new float[]{size / 14f, size / 18f}, 0f));
            g2.draw(new RoundRectangle2D.Float(x + stroke, y + stroke, size - 2 * stroke, size - 2 * stroke, arc, arc));
            g2.setFont(Theme.font(Font.BOLD, Math.max(9f, size / 6.5f)));
            FontMetrics fm = g2.getFontMetrics();
            String text = "LOGO";
            g2.drawString(text, x + (size - fm.stringWidth(text)) / 2,
                    y + (size - fm.getHeight()) / 2 + fm.getAscent());
        }
        g2.dispose();
    }
}
