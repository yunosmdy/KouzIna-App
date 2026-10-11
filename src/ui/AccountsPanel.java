package ui;
import bootstrap.ApplicationServices;
import service.*;
import domain.enums.Role;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.function.Supplier;

/**
 * Manager-only approval and account administration.
 * Same layout as Menu & Stock: the actions on the left (they scroll on short windows),
 * the accounts table on the right using the full height.
 */
final class AccountsPanel extends JPanel implements Refreshable {
    private final ApplicationServices services;
    private final Supplier<AuthenticatedUser> user;
    private final DefaultTableModel model=UiSupport.readOnlyModel("Full name","Username","Role","Status");
    private final JTable table=new JTable(model);
    private final JComboBox<Role> role=new JComboBox<>(Role.values());
    private List<AccountRow> rows=List.of();
    AccountsPanel(ApplicationServices services,Supplier<AuthenticatedUser> user) {
        super(new BorderLayout(14,0));this.services=services;this.user=user;setOpaque(false);

        FormCard form=new FormCard("Selected account");
        form.note("Select an account in the table, choose its job, then approve it. New Managers follow the same process.");
        form.field("Job",role);
        JButton approve=new JButton("Approve");
        approve.addActionListener(e -> run("Approve",() -> services.staff().approve(token(),selected().id(),(Role)role.getSelectedItem())));
        form.buttons(approve,button("Reject",() -> services.staff().reject(token(),selected().id())));
        form.section("Approved accounts");
        form.buttons(button("Change role",() -> services.staff().changeRole(token(),selected().id(),(Role)role.getSelectedItem())));
        form.buttons(button("Deactivate",() -> services.staff().setActive(token(),selected().id(),false)),
                button("Reactivate",() -> services.staff().setActive(token(),selected().id(),true)));
        form.note("Deactivate blocks sign-in but keeps the account. Delete removes it for good.");
        JButton delete=Theme.outline(new JButton("Delete account"));delete.setForeground(Theme.BAD_TEXT);
        delete.addActionListener(e -> deleteSelected());
        form.buttons(delete);

        UiSupport.columnWidths(table,190,140,110,110);
        add(form.inColumn(),BorderLayout.WEST);
        add(UiSupport.card("Accounts",UiSupport.tablePane(table)),BorderLayout.CENTER);
    }
    private String token() { return user.get().sessionToken(); }
    private AccountRow selected() {
        String id=UiSupport.selectedId(table,0,"account");
        return rows.stream().filter(r -> r.id().equals(id)).findFirst().orElseThrow();
    }
    private JButton button(String text,Runnable action) {
        JButton button=Theme.outline(new JButton(text));
        button.addActionListener(e -> run(text,action));
        return button;
    }
    private void run(String text,Runnable action) {
        if(UiSupport.confirm(this,text+" for the selected account?")) UiSupport.perform(this,action,this::refreshData);
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
