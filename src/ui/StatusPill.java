package ui;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Soft colored status badge with a dot, e.g. "● Confirmed" in green or "● Cancelled" in red.
 * Colors come from Theme.toneOf(...), so every screen uses the same indicator colors.
 */
public final class StatusPill extends JComponent {
    private static final Font FONT = Theme.font(Font.BOLD, 12f);

    private String label;
    private Theme.Tone tone;

    /** Pill whose color is picked from the status text. */
    public StatusPill(Object status) {
        setStatus(status);
    }

    /** Pill with custom text and a chosen color, e.g. new StatusPill("5 left", Theme.Tone.WAIT). */
    public StatusPill(String label, Theme.Tone tone) {
        this.label = label;
        this.tone = tone;
        setFont(FONT);
    }

    public void setStatus(Object status) {
        this.label = Theme.statusLabel(status);
        this.tone = Theme.toneOf(status instanceof Boolean flag ? (flag ? "ACTIVE" : "INACTIVE") : String.valueOf(status));
        setFont(FONT);
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(FONT);
        return new Dimension(fm.stringWidth(label) + 30, fm.getHeight() + 8);
    }

    @Override
    protected void paintComponent(Graphics g) {
        StatusPill.paint((Graphics2D) g, label, tone, 0, (getHeight() - getPreferredSize().height) / 2);
    }

    /** Draws a pill at (x, y). Shared by the component and the table renderer. */
    static void paint(Graphics2D graphics, String label, Theme.Tone tone, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(FONT);
        FontMetrics fm = g.getFontMetrics();
        int width = fm.stringWidth(label) + 30;
        int height = fm.getHeight() + 8;
        Color[] colors = Theme.toneColors(tone);
        g.setColor(colors[0]);
        g.fill(new RoundRectangle2D.Float(x, y, width, height, height, height));
        g.setColor(colors[1]);
        g.fill(new Ellipse2D.Float(x + 10, y + height / 2f - 3.5f, 7, 7));
        g.setColor(colors[2]);
        g.drawString(label, x + 22, y + (height - fm.getHeight()) / 2 + fm.getAscent());
        g.dispose();
    }

    /** Table renderer used automatically for "Status" and "Active" columns (see UiSupport.tablePane). */
    static final class Renderer extends JComponent implements TableCellRenderer {
        private String label = "";
        private Theme.Tone tone = Theme.Tone.DONE;
        private Color background = Theme.WHITE;

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean selected, boolean focused, int row, int column) {
            if (value == null) {
                label = "";
            } else {
                label = Theme.statusLabel(value);
                tone = Theme.toneOf(value instanceof Boolean flag
                        ? (flag ? "ACTIVE" : "INACTIVE") : String.valueOf(value));
            }
            background = selected ? table.getSelectionBackground()
                    : (row % 2 == 0 ? Theme.WHITE : UiSupport.STRIPE);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(background);
            g.fillRect(0, 0, getWidth(), getHeight());
            if (!label.isEmpty()) {
                int height = getFontMetrics(FONT).getHeight() + 8;
                StatusPill.paint((Graphics2D) g, label, tone, 10, (getHeight() - height) / 2);
            }
        }
    }
}
