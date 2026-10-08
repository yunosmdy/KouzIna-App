package ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/** First screen: "Welcome to Kóuz 'Inà!  Log in as  Employee / Manager". */
public final class RolePickerPanel extends BrandSplitPanel {
    private Consumer<Portal> onPick;

    public RolePickerPanel(Consumer<Portal> onPick) {
        super();
        this.onPick = onPick;
    }

    @Override
    protected JComponent content() {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Welcome to " + Theme.BRAND + "!");
        title.setFont(Theme.font(Font.BOLD, 34f));
        form.add(leftAligned(title));
        form.add(Box.createVerticalStrut(6));
        JLabel subtitle = new JLabel("Log in as");
        subtitle.setForeground(Theme.MUTED);
        subtitle.setFont(Theme.font(Font.BOLD, 16f));
        form.add(leftAligned(subtitle));
        form.add(Box.createVerticalStrut(26));
        form.add(leftAligned(roleCard(Portal.EMPLOYEE, "person")));
        form.add(Box.createVerticalStrut(14));
        form.add(leftAligned(roleCard(Portal.MANAGER, "shield")));
        return form;
    }

    private JComponent roleCard(Portal portal, String icon) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(16, 0), Theme.PAPER, 20)
                .padding(16, 16, 16, 18);
        card.outline(Theme.LINE);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Dimension size = new Dimension(400, 98);
        card.setPreferredSize(size);
        card.setMaximumSize(size);

        JComponent badge = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.ORANGE_TINT);
                g2.fillOval(0, (getHeight() - 56) / 2, 56, 56);
                g2.dispose();
                Icons.paint((Graphics2D) g, icon, 15, (getHeight() - 26) / 2, 26, Theme.ORANGE_DARK);
            }
        };
        badge.setPreferredSize(new Dimension(56, 56));
        card.add(badge, BorderLayout.WEST);

        JPanel text = new JPanel(new BorderLayout(0, 3));
        text.setOpaque(false);
        JLabel name = new JLabel(portal.label());
        name.setFont(Theme.font(Font.BOLD, 18f));
        JLabel description = new JLabel("<html>" + portal.description() + "</html>");
        description.setForeground(Theme.MUTED);
        description.setFont(Theme.font(Font.PLAIN, 13f));
        text.add(name, BorderLayout.NORTH);
        text.add(description, BorderLayout.CENTER);
        card.add(text, BorderLayout.CENTER);
        JLabel arrow = new JLabel(Icons.icon("arrow", 20, Theme.ORANGE_DARK));
        card.add(arrow, BorderLayout.EAST);
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 18));

        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                card.setBackground(Theme.ORANGE_TINT);
                card.outline(Theme.ORANGE);
            }
            @Override public void mouseExited(MouseEvent e) {
                if (!card.contains(e.getPoint())) {
                    card.setBackground(Theme.PAPER);
                    card.outline(Theme.LINE);
                }
            }
            @Override public void mouseClicked(MouseEvent e) {
                card.setBackground(Theme.PAPER);
                card.outline(Theme.LINE);
                onPick.accept(portal);
            }
        });
        return card;
    }
}
