package ui;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/** Slim scrollbar with a rounded green thumb and no arrow buttons. */
public class ThinScrollBarUI extends BasicScrollBarUI {

    public static ComponentUI createUI(JComponent c) {
        return new ThinScrollBarUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        scrollbar.setOpaque(false);
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return emptyButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return emptyButton();
    }

    private static JButton emptyButton() {
        JButton button = new JButton();
        Dimension zero = new Dimension(0, 0);
        button.setPreferredSize(zero);
        button.setMinimumSize(zero);
        button.setMaximumSize(zero);
        button.setVisible(false);
        return button;
    }

    /** Light track so it is easy to see that there is more to scroll to. */
    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        if (r.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new java.awt.Color(0xEFEAE1));
        int inset = 2;
        int size = Math.min(r.width, r.height) - inset * 2;
        g2.fillRoundRect(r.x + inset, r.y + inset, r.width - inset * 2, r.height - inset * 2, size, size);
        g2.dispose();
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
        if (r.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(isThumbRollover() || isDragging ? Theme.GREEN : Theme.SAGE);
        int inset = 2;
        int size = Math.min(r.width, r.height) - inset * 2;
        g2.fillRoundRect(r.x + inset, r.y + inset, r.width - inset * 2, r.height - inset * 2, size, size);
        g2.dispose();
    }

    @Override
    protected Dimension getMinimumThumbSize() {
        return new Dimension(12, 40);
    }
}
