package ui;

import service.AuthenticatedUser;
import service.AuthenticationService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Login screen (opened after picking Employee or Manager).
 * Only accounts that match the picked portal can sign in.
 */
public final class LoginPanel extends BrandSplitPanel {
    private AuthenticationService authentication;
    private Consumer<AuthenticatedUser> onLogin;
    private Runnable onBack;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel portalTag;
    private JLabel subtitle;
    private Portal portal;

    public LoginPanel(
            AuthenticationService authentication,
            Consumer<AuthenticatedUser> onLogin,
            Runnable onBack) {
        super();
        this.authentication = Objects.requireNonNull(authentication);
        this.onLogin = Objects.requireNonNull(onLogin);
        this.onBack = Objects.requireNonNull(onBack);
    }

    @Override
    protected JComponent content() {
        usernameField = new JTextField(18);
        passwordField = new JPasswordField(18);
        portalTag = new JLabel();
        subtitle = new JLabel();

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JButton back = Theme.outline(new JButton("Change role"));
        back.setIcon(Icons.icon("back", 16, Theme.TEXT));
        back.setIconTextGap(8);
        back.putClientProperty(Theme.ARC, 999);
        back.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 14));
        back.addActionListener(event -> onBack.run());
        form.add(leftAligned(row(back)));
        form.add(Box.createVerticalStrut(22));

        portalTag.setFont(Theme.font(Font.BOLD, 12.5f));
        portalTag.setForeground(Theme.ORANGE_DARK);
        form.add(leftAligned(portalTag));
        form.add(Box.createVerticalStrut(4));
        JLabel title = new JLabel(Theme.BRAND + " Login");
        title.setFont(Theme.font(Font.BOLD, 34f));
        title.setForeground(Theme.TEXT);
        form.add(leftAligned(title));
        form.add(Box.createVerticalStrut(6));
        subtitle.setForeground(Theme.MUTED);
        form.add(leftAligned(subtitle));
        form.add(Box.createVerticalStrut(26));

        form.add(leftAligned(fieldBox("Username", usernameField)));
        form.add(Box.createVerticalStrut(12));
        form.add(leftAligned(fieldBox("Password", passwordField)));
        form.add(Box.createVerticalStrut(24));

        JButton login = new JButton("Login");
        login.setFont(Theme.font(Font.BOLD, 14f));
        login.setBorder(BorderFactory.createEmptyBorder(10, 34, 10, 34));
        login.addActionListener(event -> login());
        form.add(leftAligned(row(login)));

        usernameField.putClientProperty(Theme.PLACEHOLDER, "Enter your username");
        passwordField.putClientProperty(Theme.PLACEHOLDER, "Enter your password");
        usernameField.addActionListener(event -> passwordField.requestFocusInWindow());
        passwordField.addActionListener(event -> login());
        setPortal(Portal.EMPLOYEE);
        return form;
    }

    /** Switches between "Employee Login" and "Manager Login". */
    public void setPortal(Portal portal) {
        this.portal = portal;
        portalTag.setText(portal.label().toUpperCase() + " PORTAL");
        subtitle.setText("Sign in with your " + portal.label().toLowerCase() + " account.");
    }

    private static JComponent row(JComponent component) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.add(component);
        return row;
    }

    /** Tinted rounded box with a small label above a white input (like the mockup). */
    private static JComponent fieldBox(String label, JTextField field) {
        RoundedPanel box = new RoundedPanel(new BorderLayout(0, 5), Theme.ORANGE_TINT, 14)
                .padding(7, 8, 8, 8);
        JLabel text = new JLabel(label);
        text.setFont(Theme.font(Font.BOLD, 12.5f));
        text.setForeground(Theme.TEXT);
        text.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));
        box.add(text, BorderLayout.NORTH);
        box.add(field, BorderLayout.CENTER);
        box.setPreferredSize(new Dimension(340, box.getPreferredSize().height));
        box.setMaximumSize(new Dimension(340, box.getPreferredSize().height));
        return box;
    }

    public void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        usernameField.requestFocusInWindow();
    }

    private void login() {
        char[] password = passwordField.getPassword();
        UiSupport.perform(this, () -> {
            AuthenticatedUser user = authentication.login(usernameField.getText(), password);
            if (!portal.allowsRole(user.roleName())) {
                throw new IllegalArgumentException(portal.wrongAccountMessage());
            }
            onLogin.accept(user);
        }, () -> passwordField.setText(""));
        Arrays.fill(password, '\0');
    }
}
