package ui;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

/**
 * Shared layout for the welcome and login screens:
 * orange curved panel on the left (with the logo space), content on the right.
 */
abstract class BrandSplitPanel extends JPanel {

    BrandSplitPanel() {
        setLayout(new GridLayout(1, 2));
        setBackground(Theme.WHITE);
        add(brandSide());
        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        GridBagConstraints center = new GridBagConstraints();
        center.anchor = GridBagConstraints.WEST;
        center.weightx = 1;
        center.insets = new Insets(0, 70, 0, 40);
        right.add(content(), center);
        add(right);
    }

    /** The right-hand side of the screen. */
    protected abstract JComponent content();

    private static JComponent brandSide() {
        JPanel left = new JPanel(new GridBagLayout());
        left.setOpaque(false);
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        LogoSpot logo = new LogoSpot(150, Theme.TEXT);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        stack.add(logo);
        stack.add(Box.createVerticalStrut(18));
        JLabel name = new JLabel(Theme.BRAND);
        name.setFont(Theme.font(Font.BOLD, 34f));
        name.setForeground(Theme.TEXT);
        name.setAlignmentX(Component.CENTER_ALIGNMENT);
        stack.add(name);
        JLabel tagline = new JLabel("Restaurant Management System");
        tagline.setFont(Theme.font(Font.PLAIN, 14f));
        tagline.setForeground(Theme.TEXT);
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);
        stack.add(tagline);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 60);
        left.add(stack, c);
        return left;
    }

    static JComponent leftAligned(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        Path2D shape = new Path2D.Float();
        shape.moveTo(0, 0);
        shape.lineTo(w * 0.48, 0);
        shape.curveTo(w * 0.535, h * 0.30, w * 0.535, h * 0.72, w * 0.50, h * 0.90);
        shape.quadTo(w * 0.485, h, w * 0.45, h);
        shape.lineTo(0, h);
        shape.closePath();
        g2.setColor(Theme.ORANGE);
        g2.fill(shape);
        g2.dispose();
    }
}
