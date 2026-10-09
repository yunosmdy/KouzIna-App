package ui;
import bootstrap.ApplicationServices;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/** One account setup form for every job, including Manager. */
final class AccountDialogs {
    private AccountDialogs() { }
    static void register(Component parent, ApplicationServices services) {
        JDialog dialog=new JDialog(SwingUtilities.getWindowAncestor(parent),"Account setup",Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel body=new JPanel(new BorderLayout(12,16));body.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        JPanel fields=new JPanel(new GridBagLayout());
        JTextField name=new JTextField(22),username=new JTextField(22);
        JPasswordField password=new JPasswordField(22),confirm=new JPasswordField(22);
        String[] labels={"Full name","Username","Password","Confirm password"};
        JComponent[] inputs={name,username,password,confirm};
        for(int row=0;row<labels.length;row++) {
            GridBagConstraints c=new GridBagConstraints();c.gridy=row;c.gridx=0;c.anchor=GridBagConstraints.WEST;c.insets=new Insets(6,0,6,12);
            fields.add(new JLabel(labels[row]),c);c.gridx=1;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;fields.add(inputs[row],c);
        }
        body.add(fields,BorderLayout.CENTER);
        JLabel note=new JLabel("<html>Choose a password with at least 8 characters.<br>Your Manager will approve your account and assign your job.</html>");
        body.add(note,BorderLayout.NORTH);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));JButton cancel=new JButton("Cancel"),submit=new JButton("Create account");
        buttons.add(cancel);buttons.add(submit);body.add(buttons,BorderLayout.SOUTH);
        cancel.addActionListener(e -> dialog.dispose());
        submit.addActionListener(e -> {
            String fullName=name.getText(),login=username.getText();char[] secret=password.getPassword(),confirmation=confirm.getPassword();
            password.setText("");confirm.setText("");
            BackgroundTask.run(dialog,submit,() -> {
                try { return services.authentication().register(fullName,login,secret,confirmation); }
                finally { Arrays.fill(secret,'\0');Arrays.fill(confirmation,'\0'); }
            },id -> { dialog.dispose();JOptionPane.showMessageDialog(parent,"Account created. Wait for your Manager to give access."); });
        });
        dialog.setContentPane(body);dialog.getRootPane().setDefaultButton(submit);dialog.pack();dialog.setLocationRelativeTo(parent);dialog.setVisible(true);
        password.setText("");confirm.setText("");
    }
}
