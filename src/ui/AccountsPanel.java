package ui;
import bootstrap.ApplicationServices;
import service.*;
import domain.enums.Role;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.function.Supplier;

/** Manager-only approval and account administration. */
final class AccountsPanel extends JPanel implements Refreshable {
    private final ApplicationServices services;
    private final Supplier<AuthenticatedUser> user;
    private final DefaultTableModel model=UiSupport.readOnlyModel("Full name","Username","Role","Status");
    private final JTable table=new JTable(model);
    private final JComboBox<Role> role=new JComboBox<>(Role.values());
    private List<AccountRow> rows=List.of();
    AccountsPanel(ApplicationServices services,Supplier<AuthenticatedUser> user) {
        super(new BorderLayout(0,12));this.services=services;this.user=user;setOpaque(false);
        JPanel controls=new JPanel(new GridLayout(0,3,8,8));controls.setOpaque(false);
        controls.add(new JLabel("Assign role:"));controls.add(role);
        button(controls,"Approve",() -> services.staff().approve(token(),selected().id(),(Role)role.getSelectedItem()));
        button(controls,"Reject",() -> services.staff().reject(token(),selected().id()));
        button(controls,"Change role",() -> services.staff().changeRole(token(),selected().id(),(Role)role.getSelectedItem()));
        button(controls,"Deactivate",() -> services.staff().setActive(token(),selected().id(),false));
        button(controls,"Reactivate",() -> services.staff().setActive(token(),selected().id(),true));
        JButton delete=Theme.outline(new JButton("Delete account"));delete.setForeground(Theme.BAD_TEXT);controls.add(delete);
        delete.addActionListener(e -> deleteSelected());
        JPanel top=new JPanel(new BorderLayout());top.setOpaque(false);top.add(controls,BorderLayout.CENTER);
        JTextArea explanation = new JTextArea("Select an account, choose its job, and approve access. New Managers follow the same process. Deactivate blocks sign-in but keeps the account; Delete removes it for good.");
        explanation.setLineWrap(true); explanation.setWrapStyleWord(true); explanation.setEditable(false); explanation.setOpaque(false);
        top.add(explanation,BorderLayout.SOUTH);
        add(top,BorderLayout.NORTH);add(UiSupport.tablePane(table),BorderLayout.CENTER);
    }
    private String token() { return user.get().sessionToken(); }
    private AccountRow selected() {
        String id=UiSupport.selectedId(table,0,"account");
        return rows.stream().filter(r -> r.id().equals(id)).findFirst().orElseThrow();
    }
    private void button(JPanel parent,String text,Runnable action) {
        JButton button=Theme.outline(new JButton(text));parent.add(button);
        button.addActionListener(e -> { if(UiSupport.confirm(this,text+" for the selected account?")) UiSupport.perform(this,action,this::refreshData); });
    }
    private void deleteSelected() {
        AccountRow row;
        try { row=selected(); }
        catch(RuntimeException error) { UiSupport.perform(this,() -> { throw error; },() -> { }); return; }
        if(!UiSupport.confirm(this,"Permanently delete "+row.fullName()+" ("+row.username()+")?\n"
                +"They will no longer be able to sign in, and this cannot be undone.\n"
                +"Tip: use Deactivate instead if they might come back.")) return;
        UiSupport.perform(this,() -> {
            String name=services.staff().delete(token(),row.id());
            try { Photos.removePerson(name); } catch(java.io.IOException ignored) { /* account is already deleted */ }
        },this::refreshData);
    }
    @Override public void refreshData() {
        rows=services.staff().accounts(token());String selected=UiSupport.selectedOrNull(table);model.setRowCount(0);
        for(AccountRow row:rows) model.addRow(new Object[]{new Choice(row.id(),row.fullName()),row.username(),row.role(),
                row.status()});
        UiSupport.restoreSelection(table,selected);
    }
}
