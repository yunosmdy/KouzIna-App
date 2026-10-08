package ui;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/** Tabs drawn as pills: orange when selected, white with an outline otherwise. */
public class PillTabbedPaneUI extends BasicTabbedPaneUI {

    public static ComponentUI createUI(JComponent c) {
        return new PillTabbedPaneUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        tabInsets = new Insets(7, 18, 7, 18);
        selectedTabPadInsets = new Insets(0, 0, 0, 0);
        tabAreaInsets = new Insets(2, 0, 12, 0);
        contentBorderInsets = new Insets(0, 0, 0, 0);
    }

    @Override
    protected Insets getTabInsets(int tabPlacement, int tabIndex) {
        return tabInsets;
    }

    @Override
    protected int calculateTabWidth(int tabPlacement, int tabIndex, java.awt.FontMetrics metrics) {
        return super.calculateTabWidth(tabPlacement, tabIndex, metrics) + 8;
    }

    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                      int x, int y, int w, int h, boolean isSelected) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RoundRectangle2D shape = new RoundRectangle2D.Float(x + 4, y + 2, w - 8, h - 4, h - 4, h - 4);
        if (isSelected) {
            g2.setColor(Theme.ORANGE);
            g2.fill(shape);
        } else {
            g2.setColor(tabIndex == getRolloverTab() ? Theme.BLUE_TINT : Theme.WHITE);
            g2.fill(shape);
            g2.setColor(Theme.LINE);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(shape);
        }
        g2.dispose();
    }

    @Override
    protected void paintText(Graphics g, int tabPlacement, java.awt.Font font, java.awt.FontMetrics metrics,
                             int tabIndex, String title, java.awt.Rectangle textRect, boolean isSelected) {
        ((Graphics2D) g).setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        super.paintText(g, tabPlacement, font, metrics, tabIndex, title, textRect, isSelected);
    }

    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                  int x, int y, int w, int h, boolean isSelected) {
    }

    @Override
    protected void paintFocusIndicator(Graphics g, int tabPlacement, java.awt.Rectangle[] rects, int tabIndex,
                                       java.awt.Rectangle iconRect, java.awt.Rectangle textRect, boolean isSelected) {
    }

    @Override
    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
    }

    @Override
    protected int getTabLabelShiftY(int tabPlacement, int tabIndex, boolean isSelected) {
        return 0;
    }

    @Override
    protected int getTabLabelShiftX(int tabPlacement, int tabIndex, boolean isSelected) {
        return 0;
    }
}
