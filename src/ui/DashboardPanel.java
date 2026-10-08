package ui;

import bootstrap.ApplicationServices;
import domain.enums.BillStatus;
import domain.enums.OrderStatus;
import domain.enums.ReservationStatus;
import domain.enums.TableStatus;
import domain.enums.WaitlistStatus;
import persistence.AppState;
import service.AuthenticatedUser;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.ToLongFunction;

/**
 * Home screen: greeting, quick numbers for today, and one card per section.
 * Employees only see the numbers and sections for their job; managers see everything.
 */
public final class DashboardPanel extends JPanel implements Refreshable {
    private static final Map<String, String> DESCRIPTIONS = Map.of(
            KouzinaFrame.RESERVATIONS, "Seat walk-ins, book tables, and keep the guest list.",
            KouzinaFrame.ORDERING, "Add food from the menu and send the order to the kitchen.",
            KouzinaFrame.KITCHEN, "Start cooking and mark orders ready to serve.",
            KouzinaFrame.BILLING, "Create the bill, take cash or e-payment, free the table.",
            KouzinaFrame.MANAGER, "Menu and stock, reports, staff and profiles, audit log.");

    /** Step numbers shown on the four workflow cards. */
    private static final Map<String, String> STEPS = Map.of(
            KouzinaFrame.RESERVATIONS, "STEP 1",
            KouzinaFrame.ORDERING, "STEP 2",
            KouzinaFrame.KITCHEN, "STEP 3",
            KouzinaFrame.BILLING, "STEP 4");

    /** One number tile: label, which roles see it, and how to count it. */
    private record Stat(String label, String hint, List<String> roles, Theme.Tone tone,
                        ToLongFunction<AppState> counter) {
    }

    private static final List<Stat> STATS = List.of(
            new Stat("Bookings", "for today", List.of("Staff", "Waiter", "Manager"), Theme.Tone.GOOD,
                    state -> state.getReservations().stream()
                            .filter(r -> r.getStartTime().toLocalDate().equals(LocalDate.now()))
                            .filter(r -> r.getStatus() != ReservationStatus.CANCELLED
                                    && r.getStatus() != ReservationStatus.NO_SHOW)
                            .count()),
            new Stat("Free tables", "available now", List.of("Staff", "Waiter", "Manager"), Theme.Tone.GOOD,
                    state -> state.getTables().stream()
                            .filter(t -> t.getStatus() == TableStatus.AVAILABLE).count()),
            new Stat("Waiting list", "parties waiting", List.of("Staff", "Waiter", "Manager"), Theme.Tone.WAIT,
                    state -> state.getWaitlistEntries().stream()
                            .filter(e -> e.getStatus() == WaitlistStatus.WAITING).count()),
            new Stat("In the kitchen", "sent or cooking", List.of("Staff", "Chef", "Manager"), Theme.Tone.WAIT,
                    state -> state.getOrders().stream()
                            .filter(o -> o.getStatus() == OrderStatus.CONFIRMED
                                    || o.getStatus() == OrderStatus.PREPARING).count()),
            new Stat("Ready to serve", "for pickup", List.of("Staff", "Waiter", "Chef", "Manager"), Theme.Tone.GOOD,
                    state -> state.getOrders().stream()
                            .filter(o -> o.getStatus() == OrderStatus.READY).count()),
            new Stat("Unpaid bills", "to collect", List.of("Staff", "Cashier", "Manager"), Theme.Tone.BAD,
                    state -> state.getBills().stream()
                            .filter(b -> b.getStatus() == BillStatus.UNPAID).count()));

    private final ApplicationServices services;
    private final Navigator navigator;
    private final JLabel greeting = new JLabel();
    private final Avatar avatar = new Avatar(58, true);
    private final JLabel subtitle = new JLabel();
    private final JPanel statRow = new JPanel(new GridLayout(1, 0, 14, 0));
    private final JPanel grid = new JPanel(new GridLayout(0, 3, 18, 18));
    private final Set<String> visibleSections = new LinkedHashSet<>();
    private final List<JLabel[]> statLabels = new ArrayList<>();
    private final List<Stat> shownStats = new ArrayList<>();
    private Portal portal = Portal.EMPLOYEE;

    public DashboardPanel(ApplicationServices services, Navigator navigator) {
        this.services = Objects.requireNonNull(services);
        this.navigator = Objects.requireNonNull(navigator);
        setLayout(new BorderLayout(0, 22));
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        JPanel words = new JPanel(new BorderLayout(0, 4));
        words.setOpaque(false);
        greeting.setFont(Theme.font(Font.BOLD, 28f));
        subtitle.setForeground(Theme.MUTED);
        subtitle.setFont(Theme.font(Font.PLAIN, 14f));
        words.add(greeting, BorderLayout.NORTH);
        words.add(subtitle, BorderLayout.SOUTH);
        header.add(avatar, BorderLayout.WEST);
        header.add(words, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        statRow.setOpaque(false);
        grid.setOpaque(false);
        JPanel body = new JPanel(new BorderLayout(0, 22));
        body.setOpaque(false);
        body.add(statRow, BorderLayout.NORTH);
        JPanel gridHolder = new JPanel(new BorderLayout(0, 10));
        gridHolder.setOpaque(false);
        JLabel sectionsTitle = new JLabel("Where to?  A guest's visit goes Step 1 → 4.");
        sectionsTitle.setFont(Theme.font(Font.BOLD, 16f));
        gridHolder.add(sectionsTitle, BorderLayout.NORTH);
        gridHolder.add(grid, BorderLayout.CENTER);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(gridHolder, BorderLayout.NORTH);
        body.add(top, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    /** Fewer tiles per row on narrow windows so nothing gets cut off. */
    @Override
    public void doLayout() {
        int width = getWidth();
        int statColumns = width >= 980 ? Math.max(1, statRow.getComponentCount()) : 3;
        int cardColumns = width >= 860 ? 3 : 2;
        if (((GridLayout) statRow.getLayout()).getColumns() != statColumns
                || ((GridLayout) grid.getLayout()).getColumns() != cardColumns) {
            statRow.setLayout(new GridLayout(0, statColumns, 14, 14));
            grid.setLayout(new GridLayout(0, cardColumns, 18, 18));
            statRow.invalidate();
            grid.invalidate();
            // the page around the dashboard needs the new height so it can scroll to the end
            javax.swing.SwingUtilities.invokeLater(this::revalidate);
        }
        super.doLayout();
    }

    public void setPortal(Portal portal) {
        this.portal = Objects.requireNonNull(portal);
    }

    public void setUser(AuthenticatedUser user) {
        String firstName = user.displayName().split(" ")[0];
        avatar.setPerson(user.displayName(), user.employeeId());
        greeting.setText(timeGreeting() + ", " + firstName + "!");
        subtitle.setText(portal.label() + " Portal  ·  " + user.roleName() + "  ·  "
                + LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));

        statRow.removeAll();
        statLabels.clear();
        shownStats.clear();
        for (Stat stat : STATS) {
            if (stat.roles().contains(user.roleName())) {
                shownStats.add(stat);
                statRow.add(statTile(stat));
            }
        }

        grid.removeAll();
        visibleSections.clear();
        for (KouzinaFrame.Section section : KouzinaFrame.SECTIONS) {
            if (!section.key().equals(KouzinaFrame.DASHBOARD) && section.allows(user.roleName())) {
                visibleSections.add(section.key());
                grid.add(card(section));
            }
        }
        refreshData();
        revalidate();
        repaint();
    }

    @Override
    public void refreshData() {
        AppState state = services.repository().snapshot();
        for (int index = 0; index < shownStats.size(); index++) {
            statLabels.get(index)[0].setText(String.valueOf(shownStats.get(index).counter().applyAsLong(state)));
        }
    }

    public boolean isSectionVisible(String cardName) {
        return visibleSections.contains(cardName);
    }

    private JComponent statTile(Stat stat) {
        RoundedPanel tile = new RoundedPanel(new BorderLayout(0, 2), Theme.WHITE, 18)
                .padding(14, 16, 14, 16);
        Color[] colors = Theme.toneColors(stat.tone());
        JComponent dot = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(colors[1]);
                g2.fillOval(0, (getHeight() - 9) / 2, 9, 9);
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(9, 9));
        JLabel name = new JLabel(stat.label());
        name.setFont(Theme.font(Font.BOLD, 13f));
        name.setForeground(Theme.MUTED);
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        top.add(dot, BorderLayout.WEST);
        top.add(name, BorderLayout.CENTER);
        JLabel value = new JLabel("0");
        value.setFont(Theme.font(Font.BOLD, 30f));
        JLabel hint = new JLabel(stat.hint());
        hint.setFont(Theme.font(Font.PLAIN, 12f));
        hint.setForeground(Theme.MUTED);
        tile.add(top, BorderLayout.NORTH);
        tile.add(value, BorderLayout.CENTER);
        tile.add(hint, BorderLayout.SOUTH);
        statLabels.add(new JLabel[]{value});
        return tile;
    }

    private JComponent card(KouzinaFrame.Section section) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 12), Theme.WHITE, 22)
                .padding(18, 20, 16, 20);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setPreferredSize(new Dimension(260, 200));

        JComponent picture = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.ORANGE_TINT);
                g2.fillOval(0, 0, 52, 52);
                g2.dispose();
                Icons.paint((Graphics2D) g, section.icon(), 14, 14, 24, Theme.ORANGE_DARK);
            }
        };
        picture.setPreferredSize(new Dimension(52, 52));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(picture, BorderLayout.WEST);
        String step = STEPS.get(section.key());
        JLabel key = new JLabel(step == null ? section.shortcut() : step + "  ·  " + section.shortcut());
        key.setFont(Theme.font(Font.BOLD, 11.5f));
        key.setForeground(step == null ? Theme.MUTED : Theme.ORANGE_DARK);
        key.setVerticalAlignment(JLabel.TOP);
        top.add(key, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        JPanel text = new JPanel(new BorderLayout(0, 4));
        text.setOpaque(false);
        JLabel title = new JLabel(section.label());
        title.setFont(Theme.font(Font.BOLD, 18f));
        JLabel description = new JLabel("<html>" + DESCRIPTIONS.getOrDefault(section.key(), "") + "</html>");
        description.setForeground(Theme.MUTED);
        text.add(title, BorderLayout.NORTH);
        text.add(description, BorderLayout.CENTER);
        card.add(text, BorderLayout.CENTER);

        JLabel open = new JLabel("Open  →");
        open.setFont(Theme.font(Font.BOLD, 13f));
        open.setForeground(Theme.ORANGE_DARK);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottom.setOpaque(false);
        bottom.add(open);
        card.add(bottom, BorderLayout.SOUTH);

        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { card.outline(Theme.ORANGE); }
            @Override public void mouseExited(MouseEvent e) {
                if (!card.contains(e.getPoint())) {
                    card.outline(null);
                }
            }
            @Override public void mouseClicked(MouseEvent e) {
                card.outline(null);
                navigator.open(section.key());
            }
        });
        return card;
    }

    private static String timeGreeting() {
        int hour = LocalTime.now().getHour();
        if (hour < 12) {
            return "Good morning";
        }
        return hour < 18 ? "Good afternoon" : "Good evening";
    }
}
