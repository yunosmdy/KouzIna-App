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
 * Main window. Screens, in order:
 *   1. Welcome ("Log in as Employee / Manager")
 *   2. Login (username + password)
 *   3. Profile picker (only after the shared "employee" or "manager" login)
 *   4. The app: sidebar on the left, the chosen screen on the right, status bar at the bottom
 */
public final class KouzinaFrame extends JFrame implements Navigator {
    public static final String DASHBOARD = "dashboard";
    public static final String RESERVATIONS = "reservations";
    public static final String ORDERING = "ordering";
    public static final String KITCHEN = "kitchen";
    public static final String BILLING = "billing";
    public static final String MANAGER = "manager";

    private static final String WELCOME = "welcome";
    private static final String LOGIN = "login";
    private static final String PROFILES = "profiles";
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
            new Section(RESERVATIONS, "Guests & Tables", "calendar", "F2", List.of("Staff", "Waiter", "Manager")),
            new Section(ORDERING, "Orders", "order", "F3", List.of("Staff", "Waiter", "Manager")),
            new Section(KITCHEN, "Kitchen", "kitchen", "F4", List.of("Staff", "Chef", "Manager")),
            new Section(BILLING, "Billing", "billing", "F5", List.of("Staff", "Cashier", "Manager")),
            new Section(MANAGER, "Manager Tools", "chart", "F6", List.of("Manager")));

    private final ApplicationServices services;
    private final CardLayout rootLayout = new CardLayout();
    private final JPanel root = new JPanel(rootLayout);
    private final CardLayout contentLayout = new CardLayout();
    private final JPanel content = new JPanel(contentLayout);
    private final Map<String, Component> screens = new LinkedHashMap<>();
    private final LoginPanel loginPanel;
    private final ProfilePickerPanel profilePicker;
    private final DashboardPanel dashboardPanel;
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
    /** The shared "employee"/"manager" login, kept so "Switch profile" does not need the password again. */
    private AuthenticatedUser sharedLogin;
    private Portal portal = Portal.EMPLOYEE;
    private String currentScreen = DASHBOARD;

    public KouzinaFrame(ApplicationServices services) {
        super(Theme.BRAND);
        this.services = services;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fitToScreen();

        loginPanel = new LoginPanel(services.authentication(), this::loggedIn, () -> rootLayout.show(root, WELCOME));
        profilePicker = new ProfilePickerPanel(services, this::startSession, this::logout);
        dashboardPanel = new DashboardPanel(services, this);
        sidebar = new Sidebar(this::switchProfile, this::logout);

        addScreen(DASHBOARD, dashboardPanel);
        addScreen(RESERVATIONS, new ReservationsPanel(services, this::currentUser, this));
        addScreen(ORDERING, new OrderingPanel(services, this::currentUser, this));
        addScreen(KITCHEN, new KitchenPanel(services, this::currentUser, this));
        addScreen(BILLING, new BillingPanel(services, this::currentUser, this));
        addScreen(MANAGER, new ManagerPanel(services, this::currentUser));
        for (Section section : SECTIONS) {
            sidebar.addItem(section.key(), section.label(), section.icon(), section.shortcut(), this::open);
        }

        root.add(UiSupport.scrollPage(new RolePickerPanel(this::pickPortal), 880, 540), WELCOME);
        root.add(UiSupport.scrollPage(loginPanel, 880, 540), LOGIN);
        root.add(UiSupport.scrollPage(profilePicker, 640, 560), PROFILES);
        root.add(appShell(), APP);
        setContentPane(root);
        installShortcuts();
        // Mouse wheel moves the page when a table or list inside it has nothing more to scroll
        UiSupport.installWheelForwarding(root);

        services.orderEvents().addListener((orderId, status) -> SwingUtilities.invokeLater(this::refreshStatus));
        Timer clock = new Timer(20_000, event -> statusClock.setText(LocalDateTime.now().format(CLOCK)));
        clock.setInitialDelay(0);
        clock.start();

        rootLayout.show(root, WELCOME);
    }

    public AuthenticatedUser currentUser() {
        if (currentUser == null) {
            throw new IllegalStateException("No employee is signed in.");
        }
        return currentUser;
    }

    // ---------- Navigator ----------

    @Override
    public void open(String screen) {
        open(screen, null);
    }

    @Override
    public void open(String screen, String orderId) {
        Component target = screens.get(screen);
        if (target == null) {
            throw new IllegalArgumentException("Unknown screen " + screen + ".");
        }
        if (!canOpen(screen)) {
            return;
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
        return SECTIONS.stream().anyMatch(s -> s.key().equals(screen) && s.allows(currentUser.roleName()));
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

    /** Welcome screen choice: Employee or Manager. */
    private void pickPortal(Portal picked) {
        portal = picked;
        loginPanel.setPortal(picked);
        loginPanel.clearFields();
        rootLayout.show(root, LOGIN);
    }

    /** Username + password accepted. Shared logins go to the profile picker first. */
    private void loggedIn(AuthenticatedUser user) {
        if (services.authentication().isSharedLogin(user)) {
            sharedLogin = user;
            profilePicker.showFor(user, portal);
            rootLayout.show(root, PROFILES);
        } else {
            sharedLogin = null;
            startSession(user);
        }
    }

    /** A profile was picked (or an individual account signed in): open the dashboard. */
    private void startSession(AuthenticatedUser user) {
        currentUser = user;
        for (Section section : SECTIONS) {
            sidebar.setItemVisible(section.key(), section.allows(user.roleName()));
        }
        dashboardPanel.setPortal(portal);
        dashboardPanel.setUser(user);
        sidebar.setPortal(portal.label() + " Portal");
        sidebar.setProfile(user.displayName(), user.employeeId(), user.roleName(), sharedLogin != null);
        statusUser.setText("Signed in as " + user.displayName() + "  ·  " + user.roleName());
        open(DASHBOARD);
    }

    private void switchProfile() {
        if (sharedLogin == null) {
            return;
        }
        currentUser = null;
        profilePicker.showFor(sharedLogin, portal);
        rootLayout.show(root, PROFILES);
    }

    private void logout() {
        currentUser = null;
        sharedLogin = null;
        loginPanel.clearFields();
        rootLayout.show(root, WELCOME);
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
