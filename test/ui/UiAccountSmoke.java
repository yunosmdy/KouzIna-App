package ui;
import bootstrap.*;
import domain.enums.Role;
import persistence.*;
import security.PasswordHasher;
import service.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.time.Clock;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/** Headful integration smoke test, using application components rather than desktop automation.
 * Test windows stay off-screen. Run separately: java -cp out ui.UiAccountSmoke [screenshot folder]. */
public final class UiAccountSmoke {
    static int checks;
    static KouzinaFrame frame;
    static void check(boolean condition,String message) { if(!condition)throw new AssertionError(message);checks++; }
    static void edt(Runnable work) throws Exception { SwingUtilities.invokeAndWait(work); }
    static <T> T read(java.util.concurrent.Callable<T> value) throws Exception {
        AtomicReference<T> result=new AtomicReference<>();edt(() -> { try{result.set(value.call());}catch(Exception e){throw new RuntimeException(e);} });return result.get();
    }
    static List<Component> descendants(Container parent) {
        List<Component> result=new ArrayList<>();
        for(Component child:parent.getComponents()){result.add(child);if(child instanceof Container container)result.addAll(descendants(container));}
        return result;
    }
    static JButton button(Container parent,String text) {
        return descendants(parent).stream().filter(c -> c instanceof JButton b&&b.isShowing()&&text.equals(b.getText())).map(c -> (JButton)c).findFirst().orElseThrow(() -> new AssertionError("Missing button: "+text));
    }
    static void click(Container parent,String text) { SwingUtilities.invokeLater(() -> button(parent,text).doClick()); }
    static void waitFor(BooleanSupplier condition) throws Exception {
        long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(12);
        while(!condition.getAsBoolean()){if(System.nanoTime()>deadline)throw new AssertionError("UI operation timed out");Thread.sleep(50);}
    }
    static JDialog dialog(String title) throws Exception {
        AtomicReference<JDialog> result=new AtomicReference<>();
        waitFor(() -> {
            try { edt(() -> {for(Window w:Window.getWindows())if(w instanceof JDialog d&&d.isShowing()&&title.equals(d.getTitle()))result.set(d);}); }
            catch(Exception e){throw new RuntimeException(e);}return result.get()!=null;
        });return result.get();
    }
    static void acknowledge() throws Exception {
        AtomicReference<JDialog> result=new AtomicReference<>();
        waitFor(() -> {try{edt(() -> {
            for(Window w:Window.getWindows())if(w instanceof JDialog d&&d.isShowing()
                    &&descendants(d).stream().anyMatch(c -> c instanceof JButton b&&"OK".equals(b.getText())))result.set(d);
        });}catch(Exception e){throw new RuntimeException(e);}return result.get()!=null;});
        click(result.get(),"OK");
        waitFor(() -> !result.get().isShowing());
    }
    static JTextField field(JDialog dialog,String label) {
        JLabel found=descendants(dialog).stream().filter(c -> c instanceof JLabel l&&label.equals(l.getText())).map(c -> (JLabel)c).findFirst().orElseThrow();
        Container parent=found.getParent();GridBagLayout layout=(GridBagLayout)parent.getLayout();int row=layout.getConstraints(found).gridy;
        return Arrays.stream(parent.getComponents()).filter(c -> c instanceof JTextField&&layout.getConstraints(c).gridy==row).map(c -> (JTextField)c).findFirst().orElseThrow();
    }
    static void fill(JDialog dialog,String name,String username,String password) throws Exception {
        edt(() -> {if(name!=null)field(dialog,"Full name").setText(name);field(dialog,"Username").setText(username);
            field(dialog,"Password").setText(password);field(dialog,"Confirm password").setText(password);});
    }
    static LoginPanel loginPanel() {return descendants(frame).stream().filter(c -> c instanceof LoginPanel).map(c -> (LoginPanel)c).findFirst().orElseThrow();}
    static void signIn(String username,String password) throws Exception {
        edt(() -> {try {
            LoginPanel panel=loginPanel();var u=LoginPanel.class.getDeclaredField("username");u.setAccessible(true);((JTextField)u.get(panel)).setText(username);
            var p=LoginPanel.class.getDeclaredField("password");p.setAccessible(true);((JPasswordField)p.get(panel)).setText(password);
        }catch(Exception e){throw new RuntimeException(e);}});
        click(frame,"Sign in");
        waitFor(() -> {try{return read(() -> frame.canOpen(KouzinaFrame.DASHBOARD));}catch(Exception e){return false;}});
    }
    static void logout() throws Exception {
        edt(() -> {try{var method=KouzinaFrame.class.getDeclaredMethod("logout");method.setAccessible(true);method.invoke(frame);}catch(Exception e){throw new RuntimeException(e);}});
        check(!read(() -> frame.canOpen(KouzinaFrame.DASHBOARD)),"Logout blocks dashboard");
    }
    static void screenshot(Path folder,String name) throws Exception {
        if(folder==null)return;Files.createDirectories(folder);
        edt(() -> {try {
            BufferedImage image=new BufferedImage(frame.getWidth(),frame.getHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics2D g=image.createGraphics();frame.printAll(g);g.dispose();ImageIO.write(image,"png",folder.resolve(name+".png").toFile());
        }catch(Exception e){throw new RuntimeException(e);}});
    }
    public static void main(String[] args) throws Exception {
        if(GraphicsEnvironment.isHeadless())throw new IllegalStateException("Run this smoke test with java.awt.headless=false.");
        Path screenshots=args.length==0?null:Path.of(args[0]);
        Thread.setDefaultUncaughtExceptionHandler((thread,error) -> {error.printStackTrace();System.exit(1);});
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if(event instanceof WindowEvent e&&e.getID()==WindowEvent.WINDOW_OPENED)e.getWindow().setLocation(-10000,-10000);
        },AWTEvent.WINDOW_EVENT_MASK);
        ApplicationServices services=ApplicationServices.forDataFile(Files.createTempDirectory("simple-ui-").resolve("state.dat"));
        edt(() -> {Theme.apply();Photos.useFolder(Path.of("photos"));frame=new KouzinaFrame(services);frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);frame.setLocation(-10000,-10000);frame.setVisible(true);});
        try {
            check(!read(() -> frame.canOpen(KouzinaFrame.MANAGER)),"No Manager navigation before authentication");
            check(read(() -> descendants(loginPanel()).stream().filter(c -> c instanceof JButton).count())==2,"Login contains only Sign in and Create account");
            check(read(() -> button(frame,"Sign in").isEnabled()),"Login immediately available");
            screenshot(screenshots,"01-simple-login");
            for(String[] person:new String[][]{{"Jael Castillo","jael.login"},{"Kitchen Person","chef.login"},{"Cashier Person","cashier.login"}}) {
                click(frame,"Create account");JDialog form=dialog("Account setup");fill(form,person[0],person[1],"personal123");click(form,"Create account");acknowledge();
                check(services.repository().snapshot().findEmployeeByUsername(person[1]).orElseThrow().getAccountStatus()==domain.enums.AccountStatus.PENDING,"Registration UI saves Pending account");
            }
            signIn("manager","manager123");
            check(read(() -> frame.currentUser().displayName().equals("Restaurant Manager")),"Login displays full name");
            edt(() -> frame.open(KouzinaFrame.MANAGER));
            AccountsPanel accounts=read(() -> descendants(frame).stream().filter(c -> c instanceof AccountsPanel).map(c -> (AccountsPanel)c).findFirst().orElseThrow());
            edt(() -> {JTabbedPane tabs=(JTabbedPane)SwingUtilities.getAncestorOfClass(JTabbedPane.class,accounts);tabs.setSelectedComponent(accounts);});
            screenshot(screenshots,"02-manager-accounts");
            for(String[] person:new String[][]{{"jael.login","WAITER"},{"chef.login","CHEF"},{"cashier.login","CASHIER"}}) {
                edt(() -> {
                    JTable table=descendants(accounts).stream().filter(c -> c instanceof JTable).map(c -> (JTable)c).findFirst().orElseThrow();
                    for(int i=0;i<table.getRowCount();i++)if(person[0].equals(table.getValueAt(i,1)))table.setRowSelectionInterval(i,i);
                    JComboBox<?> roles=descendants(accounts).stream().filter(c -> c instanceof JComboBox).map(c -> (JComboBox<?>)c).findFirst().orElseThrow();roles.setSelectedItem(Role.valueOf(person[1]));
                });
                click(accounts,"Approve");acknowledge();
                waitFor(() -> services.repository().snapshot().findEmployeeByUsername(person[0]).orElseThrow().isActive());
                check(services.repository().snapshot().findEmployeeByUsername(person[0]).orElseThrow().getRoleName().equals(Role.valueOf(person[1]).toString()),"Manager approval UI assigns selected role");
            }
            String managerToken=read(() -> frame.currentUser().sessionToken());logout();
            for(String[] person:new String[][]{{"jael.login","Waiter"},{"chef.login","Chef"},{"cashier.login","Cashier"}}) {
                signIn(person[0],"personal123");
                check(read(() -> frame.currentUser().roleName().equals(person[1])),"Login automatically resolves "+person[1]);
                check(!read(() -> frame.canOpen(KouzinaFrame.MANAGER)),"Employee cannot open Manager Tools");
                for(String screen:new String[]{KouzinaFrame.RESERVATIONS,KouzinaFrame.ORDERING,KouzinaFrame.KITCHEN,KouzinaFrame.BILLING}) {
                    boolean expected=person[1].equals("Waiter")?(screen.equals(KouzinaFrame.RESERVATIONS)||screen.equals(KouzinaFrame.ORDERING)):
                            person[1].equals("Chef")?screen.equals(KouzinaFrame.KITCHEN):screen.equals(KouzinaFrame.BILLING);
                    check(read(() -> frame.canOpen(screen))==expected,"Navigation matches role: "+person[1]+" / "+screen);
                    if(expected)edt(() -> frame.open(screen));
                }
                edt(() -> frame.getRootPane().getActionMap().get("open-manager").actionPerformed(new ActionEvent(frame,0,"F6")));
                check(!read(() -> frame.canOpen(KouzinaFrame.MANAGER)),"F6 cannot elevate employee");
                if(person[1].equals("Waiter"))screenshot(screenshots,"03-waiter-orders");
                logout();
            }
            // Old Manager token was revoked by the frame's logout.
            try{services.staff().accounts(managerToken);throw new AssertionError("Stale Manager token accepted");}catch(exception.AuthorizationException expected){checks++;}
            signIn("manager","manager123");logout();
            signIn("jael.login","personal123");AuthenticatedUser stale=read(() -> frame.currentUser());
            AuthenticatedUser manager=services.authentication().login("manager","manager123".toCharArray());
            services.staff().setActive(manager.sessionToken(),stale.employeeId(),false);
            check(!read(() -> frame.canOpen(KouzinaFrame.ORDERING)),"Deactivation sends open employee UI back to login");
            check(read(() -> button(frame,"Sign in").isShowing()),"Login visible after session invalidation");
            check(!read(() -> frame.canOpen(KouzinaFrame.DASHBOARD)),"Invalidated session cannot reopen dashboard");
            services.authentication().logout(manager);
            edt(() -> frame.dispose());
            Path oldFile=Files.createTempDirectory("simple-old-ui-").resolve("state.dat");
            Files.copy(AccountChecks.fixture("shared.dat"),oldFile);
            ApplicationServices legacy=ApplicationServices.forDataFile(oldFile);
            edt(() -> {frame=new KouzinaFrame(legacy);frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);frame.setLocation(-10000,-10000);frame.setVisible(true);});
            check(read(() -> descendants(loginPanel()).stream().filter(c -> c instanceof JButton).count())==2,"Old save has the same simple login");
            signIn("manager","manager123");
            check(read(() -> frame.currentUser().employeeId().equals("employee-manager")),"Existing Manager signs in directly with preserved identity");
            logout();
            click(frame,"Create account");JDialog request=dialog("Account setup");
            fill(request,"New Manager","new.manager","personal123");click(request,"Create account");acknowledge();
            check(legacy.repository().snapshot().findEmployeeByUsername("new.manager").orElseThrow().getAccountStatus()==domain.enums.AccountStatus.PENDING,"New Manager follows standard account setup");
            System.out.println("PASS: "+checks+" window, registration, approval, login, navigation, logout and preset-account smoke checks");
        } finally { edt(() -> {for(Window window:Window.getWindows())window.dispose();}); }
        System.exit(0);
    }
}
