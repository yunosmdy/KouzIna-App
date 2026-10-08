package ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Left navigation bar: brand on top, section buttons, Log out at the bottom. */
public final class Sidebar extends JPanel {
    public static final int WIDTH = 230;

    private final Map<String, NavItem> items = new LinkedHashMap<>();
    private final JPanel itemColumn = new JPanel();

    private final Avatar avatar = new Avatar(40, true);
    private final JLabel profileName = new JLabel();
    private final JLabel profileRole = new JLabel();
    private final NavItem switchItem;

    public Sidebar(Runnable switchProfile, Runnable logout) {
        setLayout(new BorderLayout());
        setBackground(Theme.CREAM);
        setPreferredSize(new Dimension(WIDTH, 0));
        setBorder(BorderFactory.createEmptyBorder(18, 10, 14, 10));

        add(brand(), BorderLayout.NORTH);

        itemColumn.setLayout(new BoxLayout(itemColumn, BoxLayout.Y_AXIS));
        itemColumn.setOpaque(false);
        itemColumn.setBorder(BorderFactory.createEmptyBorder(22, 0, 0, 0));
        add(itemColumn, BorderLayout.CENTER);

        // Bottom: who is signed in, Switch profile, Log out
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        RoundedPanel profile = new RoundedPanel(new BorderLayout(10, 0), Theme.PAPER, 16).padding(8, 8, 8, 8);
        profile.add(avatar, BorderLayout.WEST);
        JPanel words = new JPanel(new java.awt.GridLayout(0, 1));
        words.setOpaque(false);
        profileName.setFont(Theme.font(Font.BOLD, 13.5f));
        profileRole.setFont(Theme.font(Font.PLAIN, 12f));
        profileRole.setForeground(Theme.MUTED);
        words.add(profileName);
        words.add(profileRole);
        profile.add(words, BorderLayout.CENTER);
        profile.setAlignmentX(LEFT_ALIGNMENT);
        profile.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        bottom.add(profile);
        bottom.add(Box.createVerticalStrut(8));
        switchItem = new NavItem("swap", "Switch profile", null, key -> switchProfile.run());
        bottom.add(switchItem);
        NavItem logoutItem = new NavItem("logout", "Log out", null, key -> logout.run());
        logoutItem.accent = Theme.GREEN_DARK;
        bottom.add(logoutItem);
        add(bottom, BorderLayout.SOUTH);
    }

    /** Shows the signed-in profile at the bottom of the sidebar. */
    public void setProfile(String name, String colorKey, String role, boolean canSwitch) {
        avatar.setPerson(name, colorKey);
        profileName.setText(name);
        profileRole.setText(role);
        switchItem.setVisible(canSwitch);
    }

    public void addItem(String key, String label, String icon, String shortcut, Consumer<String> onClick) {
        NavItem item = new NavItem(icon, label, shortcut, ignored -> onClick.accept(key));
        items.put(key, item);
        itemColumn.add(item);
        itemColumn.add(Box.createVerticalStrut(4));
    }

    public void setSelected(String key) {
        items.forEach((name, item) -> item.setSelected(name.equals(key)));
    }

    public void setItemVisible(String key, boolean visible) {
        NavItem item = items.get(key);
        if (item != null) {
            item.setVisible(visible);
        }
    }

    public boolean isItemVisible(String key) {
        NavItem item = items.get(key);
        return item != null && item.isVisible();
    }

    private final JLabel portalLabel = new JLabel("Restaurant Management");

    /** Shows "Manager Portal" or "Employee Portal" under the name. */
    public void setPortal(String text) {
        portalLabel.setText(text);
    }

    private JComponent brand() {
        JPanel brand = new JPanel(new BorderLayout(10, 0));
        brand.setOpaque(false);
        brand.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));

        // Space reserved for the logo (add logo.png to the Code folder to show it)
        brand.add(new LogoSpot(42, Theme.ORANGE_DARK), BorderLayout.WEST);

        JPanel words = new JPanel(new BorderLayout());
        words.setOpaque(false);
        JLabel name = new JLabel(Theme.BRAND);
        name.setFont(Theme.font(Font.BOLD, 18f));
        portalLabel.setFont(Theme.font(Font.BOLD, 11.5f));
        portalLabel.setForeground(Theme.ORANGE_DARK);
        words.add(name, BorderLayout.CENTER);
        words.add(portalLabel, BorderLayout.SOUTH);
        brand.add(words, BorderLayout.CENTER);
        return brand;
    }

    /** One clickable row in the sidebar. */
    private static final class NavItem extends JComponent {
        private final String icon;
        private final String label;
        private final String shortcut;
        private boolean selected;
        private boolean hover;
        private Color accent = Theme.TEXT;

        NavItem(String icon, String label, String shortcut, Consumer<String> onClick) {
            this.icon = icon;
            this.label = label;
            this.shortcut = shortcut;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(Theme.font(Font.PLAIN, 14f));
            setAlignmentX(LEFT_ALIGNMENT);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) { onClick.accept(label); }
            });
        }

        void setSelected(boolean selected) {
            this.selected = selected;
            repaint();
        }

        @Override public Dimension getPreferredSize() { return new Dimension(WIDTH - 20, 44); }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, 44); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (selected || hover) {
                g2.setColor(selected ? Theme.ORANGE : Theme.BLUE_TINT);
                g2.fill(new RoundRectangle2D.Float(0, 2, getWidth(), getHeight() - 4, 14, 14));
            }
            Color color = selected ? Theme.TEXT : accent;
            int iconSize = 20;
            Icons.paint(g2, icon, 14, (getHeight() - iconSize) / 2, iconSize, color);

            g2.setFont(selected ? getFont().deriveFont(Font.BOLD) : getFont());
            FontMetrics fm = g2.getFontMetrics();
            int baseline = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(color);
            g2.drawString(label, 46, baseline);

            if (shortcut != null) {
                g2.setFont(Theme.font(Font.PLAIN, 11.5f));
                FontMetrics small = g2.getFontMetrics();
                g2.setColor(selected ? Theme.TEXT : Theme.MUTED);
                int width = small.stringWidth(shortcut);
                g2.drawString(shortcut, getWidth() - width - 14,
                        (getHeight() - small.getHeight()) / 2 + small.getAscent());
            }
            g2.dispose();
        }
    }
}
