package ui;

import bootstrap.ApplicationServices;
import domain.enums.OrderStatus;
import service.AuthenticatedUser;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Individual login followed by a dashboard built for the authenticated role.
 * Screen callbacks belong to one login and are discarded when it ends.
 */
public final class KouzinaFrame extends JFrame implements Navigator {
    public static final String DASHBOARD = "dashboard";
    public static final String RESERVATIONS = "reservations";
    public static final String ORDERING = "ordering";
    public static final String KITCHEN = "kitchen";
    public static final String BILLING = "billing";
    public static final String MANAGER = "manager";

    private static final String LOGIN = "login";
    private static final String APP = "app";
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("EEE, MMM d  ·  h:mm a");
    /** Smallest size a screen needs; smaller windows get scrollbars instead of cut-off buttons. */
    private static final int SCREEN_MIN_WIDTH = 680;
    private static final int SCREEN_MIN_HEIGHT = 600;

    /** Sidebar sections: key, label, icon, shortcut, roles that can open it. */
    record Section(String key, String label, String icon, String shortcut, List<String> roles) {
        boolean allows(String role) {
            return roles.isEmpty() || roles.contains(role);
        }
    }

    static final List<Section> SECTIONS = List.of(
            new Section(DASHBOARD, "Dashboard", "home", "F1", List.of()),
            new Section(RESERVATIONS, "Guests & Tables", "calendar", "F2", List.of("Waiter", "Manager")),
            new Section(ORDERING, "Orders", "order", "F3", List.of("Waiter", "Manager")),
            new Section(KITCHEN, "Kitchen", "kitchen", "F4", List.of("Chef", "Manager")),
            new Section(BILLING, "Billing", "billing", "F5", List.of("Cashier", "Manager")),
            new Section(MANAGER, "Manager Tools", "chart", "F6", List.of("Manager")));

    private final ApplicationServices services;
    private final CardLayout rootLayout = new CardLayout();
    private final JPanel root = new JPanel(rootLayout);
    private final CardLayout contentLayout = new CardLayout();
    private final JPanel content = new JPanel(contentLayout);
    private final Map<String, Component> screens = new LinkedHashMap<>();
    private final LoginPanel loginPanel;
    private DashboardPanel dashboardPanel;
    private final Sidebar sidebar;
    private final JLabel statusMessage = new JLabel();
    private final JLabel statusUser = new JLabel();
    private final JLabel statusClock = new JLabel();
    private final JLabel badge = new JLabel("0", JLabel.CENTER) {
        @Override protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    };
    private AuthenticatedUser currentUser;
    private String currentScreen = DASHBOARD;

    public KouzinaFrame(ApplicationServices services) {
        super(Theme.BRAND);
        this.services = services;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        useLogoAsAppIcon();
        fitToScreen();

        loginPanel = new LoginPanel(services, this::startSession);
        sidebar = new Sidebar(this::logout);
        for (Section section : SECTIONS) {
            sidebar.addItem(section.key(), section.label(), section.icon(), section.shortcut(), this::open);
        }

        root.add(UiSupport.scrollPage(loginPanel, 880, 540), LOGIN);
        root.add(appShell(), APP);
        setContentPane(root);
        installShortcuts();
        // Mouse wheel moves the page when a table or list inside it has nothing more to scroll
        UiSupport.installWheelForwarding(root);

        services.orderEvents().addListener((orderId, status) -> SwingUtilities.invokeLater(this::refreshStatus));
        Timer clock = new Timer(20_000, event -> statusClock.setText(LocalDateTime.now().format(CLOCK)));
        clock.setInitialDelay(0);
        clock.start();
        Timer accessCheck = new Timer(1000, event -> {
            if (currentUser != null) {
                try { currentUser(); } catch (RuntimeException ignored) { }
            }
        });
        accessCheck.start();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent event) { logout(); }
            @Override public void windowClosed(java.awt.event.WindowEvent event) { clock.stop(); accessCheck.stop(); }
        });

        rootLayout.show(root, LOGIN);
    }

    /** Window, title-bar and taskbar icon: photos/logo.png instead of the default Java icon. */
    private void useLogoAsAppIcon() {
        java.awt.image.BufferedImage logo = Photos.logo();
        if (logo == null) {
            return;
        }
        List<java.awt.Image> icons = new java.util.ArrayList<>();
        for (int size : new int[]{16, 20, 24, 32, 40, 48, 64, 128, 256}) {
            icons.add(logo.getScaledInstance(size, size, java.awt.Image.SCALE_SMOOTH));
        }
        setIconImages(icons);
        try {
            if (java.awt.Taskbar.isTaskbarSupported()
                    && java.awt.Taskbar.getTaskbar().isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) {
                java.awt.Taskbar.getTaskbar().setIconImage(icons.get(icons.size() - 1));
            }
        } catch (RuntimeException ignored) {
            // some systems do not allow changing the taskbar icon; the window icon is still set
        }
    }

    public AuthenticatedUser currentUser() {
        if (currentUser == null) {
            throw new IllegalStateException("No employee is signed in.");
        }
        try { return services.authentication().validateSession(currentUser); }
        catch (RuntimeException error) { logout(); throw error; }
    }

    // ---------- Navigator ----------

    @Override
    public void open(String screen) {
        open(screen, null);
    }

    @Override
    public void open(String screen, String orderId) {
        if (!canOpen(screen)) return;
        Component target = screens.get(screen);
        if (target == null) {
            throw new IllegalArgumentException("Unknown screen " + screen + ".");
        }
        if (target instanceof Refreshable refreshable) {
            refreshable.refreshData();
        }
        if (orderId != null && target instanceof OrderFocus focus) {
            focus.focusOrder(orderId);
        }
        currentScreen = screen;
        contentLayout.show(content, screen);
        sidebar.setSelected(screen);
        rootLayout.show(root, APP);
        refreshStatus();
    }

    @Override
    public boolean canOpen(String screen) {
        if (currentUser == null) {
            return false;
        }
        try {
            AuthenticatedUser user = currentUser();
            return SECTIONS.stream().anyMatch(s -> s.key().equals(screen) && s.allows(user.roleName()));
        } catch (RuntimeException error) { return false; }
    }

    // ---------- Layout ----------

    /** Uses most of the screen, and maximizes on small laptop screens. */
    private void fitToScreen() {
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setMinimumSize(new Dimension(Math.min(800, screen.width), Math.min(560, screen.height)));
        setSize(Math.min(1360, screen.width), Math.min(860, screen.height));
        setLocationRelativeTo(null);
        if (screen.width < 1300 || screen.height < 760) {
            setExtendedState(MAXIMIZED_BOTH);
        }
    }

    private JComponent appShell() {
        JPanel shell = new JPanel(new BorderLayout());
        shell.setBackground(Theme.CREAM);
        javax.swing.JScrollPane sidebarScroll = UiSupport.scrollPage(sidebar, 0, -1);
        sidebarScroll.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sidebarScroll.setPreferredSize(new Dimension(Sidebar.WIDTH, 0));
        sidebarScroll.setBackground(Theme.CREAM);
        shell.add(sidebarScroll, BorderLayout.WEST);

        content.setOpaque(false);
        RoundedPanel contentCard = new RoundedPanel(new BorderLayout(), Theme.PAPER, 22).padding(12, 12, 12, 12);
        contentCard.add(content, BorderLayout.CENTER);
        JPanel contentWrap = new JPanel(new BorderLayout());
        contentWrap.setOpaque(false);
        contentWrap.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 14));
        contentWrap.add(contentCard, BorderLayout.CENTER);
        shell.add(contentWrap, BorderLayout.CENTER);
        shell.add(statusBar(), BorderLayout.SOUTH);
        return shell;
    }

    private JComponent statusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.CREAM);
        bar.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 22));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(new JLabel(Icons.icon("bell", 18, Theme.TEXT)));
        badge.setBackground(Theme.GREEN);
        badge.setForeground(Theme.WHITE);
        badge.setFont(Theme.font(Font.BOLD, 11.5f));
        badge.setPreferredSize(new Dimension(24, 20));
        left.add(badge);
        statusMessage.setForeground(Theme.TEXT);
        left.add(statusMessage);
        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        right.setOpaque(false);
        statusUser.setForeground(Theme.MUTED);
        statusClock.setForeground(Theme.MUTED);
        right.add(statusUser);
        right.add(statusClock);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void installShortcuts() {
        JComponent rootPane = getRootPane();
        for (Section section : SECTIONS) {
            String actionKey = "open-" + section.key();
            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(section.shortcut()), actionKey);
            rootPane.getActionMap().put(actionKey, new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) {
                    if (canOpen(section.key())) {
                        open(section.key());
                    }
                }
            });
        }
    }

    private void addScreen(String name, JComponent component) {
        screens.put(name, component);
        content.add(UiSupport.scrollPage(component, SCREEN_MIN_WIDTH,
                name.equals(DASHBOARD) ? -1 : SCREEN_MIN_HEIGHT), name);
    }

    // ---------- Signing in ----------

    /** Every screen callback is bound to this login; old callbacks cannot act as the next user. */
    private void startSession(AuthenticatedUser user) {
        services.authentication().validateSession(user);
        currentUser = user;
        java.util.function.Supplier<AuthenticatedUser> sessionUser = () -> {
            if (currentUser != user) throw new exception.AuthorizationException("This screen belongs to a previous login.");
            return currentUser();
        };
        screens.clear(); content.removeAll();
        dashboardPanel = new DashboardPanel(services, this);
        addScreen(DASHBOARD, dashboardPanel);
        if (canOpen(RESERVATIONS)) addScreen(RESERVATIONS, new ReservationsPanel(services, sessionUser, this));
        if (canOpen(ORDERING)) addScreen(ORDERING, new OrderingPanel(services, sessionUser, this));
        if (canOpen(KITCHEN)) addScreen(KITCHEN, new KitchenPanel(services, sessionUser, this));
        if (canOpen(BILLING)) addScreen(BILLING, new BillingPanel(services, sessionUser, this));
        if (canOpen(MANAGER)) addScreen(MANAGER, new ManagerPanel(services, sessionUser));
        for (Section section : SECTIONS) sidebar.setItemVisible(section.key(), section.allows(user.roleName()));
        dashboardPanel.setUser(user);
        sidebar.setPortal("Individual account");
        sidebar.setProfile(user.displayName(), user.employeeId(), user.roleName());
        statusUser.setText("Signed in as " + user.displayName() + "  ·  " + user.roleName());
        open(DASHBOARD);
    }

    private void logout() {
        AuthenticatedUser previous = currentUser;
        currentUser = null;
        try { services.authentication().logout(previous); }
        catch (RuntimeException error) { /* Logout still revokes the token if saving the audit fails. */ }
        for (java.awt.Window window : getOwnedWindows()) window.dispose();
        screens.clear(); content.removeAll();
        loginPanel.clearFields(); loginPanel.refreshMode();
        rootLayout.show(root, LOGIN);
    }

    /** Bottom bar: how many orders are ready to be served. */
    private void refreshStatus() {
        if (currentUser == null) {
            return;
        }
        long ready = services.repository().snapshot().getOrders().stream()
                .filter(order -> order.getStatus() == OrderStatus.READY)
                .count();
        badge.setText(String.valueOf(ready));
        badge.setBackground(ready > 0 ? Theme.ORANGE_DARK : Theme.GREEN);
        statusMessage.setText(ready == 0
                ? "All caught up — no orders waiting to be served."
                : ready + (ready == 1 ? " order is" : " orders are") + " ready to serve — see Orders.");
    }
}
