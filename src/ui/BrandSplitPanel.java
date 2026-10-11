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

/**
 * Layout for the login screen: a solid orange half on the left (logo, name, tagline),
 * a green stripe down the middle, and white on the right for the form.
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
        center.insets = new Insets(0, STRIPE + 50, 0, 40);
        right.add(content(), center);
        add(right);
    }

    /** Width of the green stripe between the orange and white halves. */
    static final int STRIPE = 55;

    /** The right-hand side of the screen. */
    protected abstract JComponent content();

    private static JComponent brandSide() {
        JPanel left = new JPanel(new GridBagLayout());
        left.setOpaque(false);
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        LogoSpot logo = new LogoSpot(170, Theme.TEXT, Theme.WHITE);
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
        left.add(stack, new GridBagConstraints());
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
        int half = w / 2;
        g2.setColor(Theme.ORANGE);
        g2.fillRect(0, 0, half, h);
        g2.setColor(Theme.GREEN);
        g2.fillRect(half, 0, STRIPE, h);
        g2.dispose();
    }
}
