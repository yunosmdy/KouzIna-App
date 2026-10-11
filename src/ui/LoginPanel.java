package ui;
import bootstrap.ApplicationServices;
import service.AuthenticatedUser;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.function.Consumer;

/** One individual login. Account role determines the destination automatically. */
public final class LoginPanel extends BrandSplitPanel {
    private ApplicationServices services;
    private Consumer<AuthenticatedUser> onLogin;
    private JTextField username;
    private JPasswordField password;
    private JButton login,register;
    private JLabel message;
    public LoginPanel(ApplicationServices services,Consumer<AuthenticatedUser> onLogin) {
        super();this.services=services;this.onLogin=onLogin;refreshMode();
    }
    @Override protected JComponent content() {
        JPanel form=new JPanel();form.setOpaque(false);form.setLayout(new BoxLayout(form,BoxLayout.Y_AXIS));
        JLabel title=new JLabel("LOGIN");title.setFont(heavyFont(46f));title.setForeground(Color.BLACK);form.add(leftAligned(title));
        message=new JLabel();message.setFont(Theme.font(Font.PLAIN,13f));form.add(leftAligned(message));form.add(Box.createVerticalStrut(26));
        username=new JTextField(22);password=new JPasswordField(22);
        form.add(leftAligned(new JLabel("Username")));form.add(leftAligned(username));form.add(Box.createVerticalStrut(12));
        form.add(leftAligned(new JLabel("Password")));form.add(leftAligned(password));form.add(Box.createVerticalStrut(18));
        login=new JButton("Sign in");register=new JButton("Create account");
        for(JButton button:new JButton[]{login,register}) { form.add(leftAligned(button));form.add(Box.createVerticalStrut(8)); }
        login.addActionListener(e -> login());password.addActionListener(e -> login());username.addActionListener(e -> password.requestFocusInWindow());
        register.addActionListener(e -> AccountDialogs.register(this,services));
        return form;
    }
    /** Extra-bold font for the LOGIN title: Segoe UI Black on Windows, else Arial Black, else bold. */
    private static Font heavyFont(float size) {
        java.util.Set<String> installed=java.util.Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for(String family:new String[]{"Segoe UI Black","Arial Black","Inter Black"})
            if(installed.contains(family)) return new Font(family,Font.PLAIN,Math.round(size));
        return Theme.font(Font.BOLD,size);
    }
    public void refreshMode() {
        message.setText("Sign in, or create an account for Manager approval.");
        revalidate();repaint();
    }
    public void clearFields() { username.setText("");password.setText("");username.requestFocusInWindow(); }
    private void login() {
        if(!login.isEnabled()) return;
        String input=username.getText();char[] secret=password.getPassword();password.setText("");
        BackgroundTask.run(this,login,() -> {
            try { return services.authentication().login(input,secret); }
            finally { Arrays.fill(secret,'\0'); }
        },onLogin);
    }
}
