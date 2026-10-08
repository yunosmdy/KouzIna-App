package ui;

import bootstrap.ApplicationServices;
import domain.Employee;
import service.AuthenticatedUser;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * "Would you like to log in as" screen, shown after the shared employee or manager login.
 * Works like picking a Netflix profile: click a picture to continue, "Add profile" makes a new one,
 * and "Manage profiles" lets you change a photo or remove a profile.
 */
public final class ProfilePickerPanel extends JPanel {
    private static final Color BACKGROUND = Theme.TEXT;               // deep brown
    private static final Color SOFT_TEXT = new Color(0xD9CFC2);       // light text on brown
    private static final int TILE = 150;
    private static final int MAX_COLUMNS = 5;

    private final ApplicationServices services;
    private final Consumer<AuthenticatedUser> onChosen;
    private final JLabel heading = new JLabel("", JLabel.CENTER);
    private final JLabel subheading = new JLabel("", JLabel.CENTER);
    private final JPanel tiles = new JPanel();
    private final JButton manage = new JButton("Manage profiles");
    private AuthenticatedUser login;
    private Portal portal = Portal.EMPLOYEE;
    private boolean managing;

    public ProfilePickerPanel(ApplicationServices services, Consumer<AuthenticatedUser> onChosen, Runnable onBack) {
        this.services = Objects.requireNonNull(services);
        this.onChosen = Objects.requireNonNull(onChosen);
        setLayout(new GridBagLayout());
        setBackground(BACKGROUND);

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        LogoSpot logo = new LogoSpot(56, SOFT_TEXT);
        logo.setAlignmentX(CENTER_ALIGNMENT);
        column.add(logo);
        column.add(Box.createVerticalStrut(18));
        heading.setFont(Theme.font(Font.BOLD, 36f));
        heading.setForeground(Theme.WHITE);
        heading.setAlignmentX(CENTER_ALIGNMENT);
        column.add(heading);
        column.add(Box.createVerticalStrut(6));
        subheading.setFont(Theme.font(Font.PLAIN, 15f));
        subheading.setForeground(SOFT_TEXT);
        subheading.setAlignmentX(CENTER_ALIGNMENT);
        column.add(subheading);
        column.add(Box.createVerticalStrut(34));

        tiles.setOpaque(false);
        JScrollPane scroll = new JScrollPane(tiles);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.setAlignmentX(CENTER_ALIGNMENT);
        column.add(scroll);
        column.add(Box.createVerticalStrut(34));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttons.setOpaque(false);
        darkOutline(manage);
        manage.setIcon(Icons.icon("pencil", 16, SOFT_TEXT));
        manage.setIconTextGap(8);
        manage.addActionListener(event -> setManaging(!managing));
        JButton back = new JButton("Log out");
        darkOutline(back);
        back.setIcon(Icons.icon("logout", 16, SOFT_TEXT));
        back.setIconTextGap(8);
        back.addActionListener(event -> {
            setManaging(false);
            onBack.run();
        });
        buttons.add(manage);
        buttons.add(back);
        buttons.setAlignmentX(CENTER_ALIGNMENT);
        column.add(buttons);
        add(column);

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "stop-managing");
        getActionMap().put("stop-managing", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (managing) {
                    setManaging(false);
                }
            }
        });
    }

    /** Called right after the shared login succeeds. */
    public void showFor(AuthenticatedUser sharedLogin, Portal portal) {
        this.login = Objects.requireNonNull(sharedLogin);
        this.portal = Objects.requireNonNull(portal);
        managing = false;
        rebuild();
    }

    private void setManaging(boolean value) {
        managing = value;
        rebuild();
    }

    private void rebuild() {
        heading.setText(managing ? "Manage profiles" : "Would you like to log in as");
        subheading.setText(managing
                ? "Click a profile to change its photo or remove it."
                : portal.label() + " portal  ·  choose your profile");
        manage.setText(managing ? "Done" : "Manage profiles");
        manage.setIcon(Icons.icon(managing ? "check" : "pencil", 16, SOFT_TEXT));

        tiles.removeAll();
        List<Employee> profiles = login == null ? List.of() : services.staff().profilesFor(login.employeeId());
        int count = profiles.size() + 1;
        int columns = Math.min(count, MAX_COLUMNS);
        tiles.setLayout(new GridLayout(0, columns, 30, 26));
        for (Employee profile : profiles) {
            tiles.add(new ProfileTile(profile.getName(), profile.getId(), () -> {
                if (managing) {
                    editProfile(profile);
                } else {
                    choose(profile);
                }
            }));
        }
        tiles.add(new ProfileTile(null, null, this::addProfile));
        int rows = (count + columns - 1) / columns;
        int width = columns * TILE + (columns - 1) * 30;
        int height = Math.min(rows, 2) * (TILE + 40) + (Math.min(rows, 2) - 1) * 26 + (rows > 2 ? 40 : 0);
        Component scroll = SwingUtilities.getAncestorOfClass(JScrollPane.class, tiles);
        if (scroll instanceof JScrollPane pane) {
            Dimension size = new Dimension(width + 8 + (rows > 2 ? 14 : 0), height + 4);
            pane.setPreferredSize(size);
            pane.setMaximumSize(size);
        }
        revalidate();
        repaint();
    }

    private void choose(Employee profile) {
        UiSupport.perform(this, () -> onChosen.accept(
                services.authentication().chooseProfile(login, profile.getId())), () -> { });
    }

    // ---------- Add / edit dialogs ----------

    private void addProfile() {
        JTextField name = new JTextField(20);
        UiSupport.placeholder(name, "First and last name");
        UiSupport.formatOnLeave(name, UiSupport::formatName);
        File[] photo = {null};
        Avatar preview = new Avatar(96, false);
        preview.setPerson("?", "new");
        JLabel photoNote = UiSupport.muted("No photo yet — initials will be shown.");

        JButton choosePhoto = Theme.outline(new JButton("Choose photo…"));
        choosePhoto.addActionListener(event -> {
            File file = pickImage();
            if (file != null) {
                photo[0] = file;
                photoNote.setText(file.getName());
            }
        });

        JDialog dialog = dialog("Add profile");
        JPanel body = dialogBody(dialog, "Add profile",
                (portal == Portal.MANAGER ? "New manager profile" : "New employee profile")
                        + " for the " + portal.label().toLowerCase() + " login.");
        JPanel row = new JPanel(new BorderLayout(16, 0));
        row.setOpaque(false);
        row.add(preview, BorderLayout.WEST);
        JPanel fields = new JPanel(new GridLayout(0, 1, 0, 6));
        fields.setOpaque(false);
        fields.add(bold("Name"));
        fields.add(name);
        JPanel photoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        photoRow.setOpaque(false);
        photoRow.add(choosePhoto);
        fields.add(photoRow);
        fields.add(photoNote);
        row.add(fields, BorderLayout.CENTER);
        body.add(row);
        name.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void update() { preview.setPerson(name.getText().isBlank() ? "?" : name.getText(), "new"); }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        JButton cancel = Theme.outline(new JButton("Cancel"));
        cancel.addActionListener(event -> dialog.dispose());
        JButton create = new JButton("Add profile");
        Runnable save = () -> UiSupport.perform(dialog, () -> {
            String cleaned = UiSupport.formatName(name.getText());
            services.staff().createProfile(login.employeeId(), cleaned);
            if (photo[0] != null) {
                try {
                    Photos.savePerson(cleaned, photo[0]);
                } catch (IOException error) {
                    UiSupport.showSuccess(dialog, "The profile was added, but the photo could not be copied: "
                            + error.getMessage());
                }
            }
        }, () -> {
            dialog.dispose();
            rebuild();
        });
        create.addActionListener(event -> save.run());
        name.addActionListener(event -> save.run());
        showDialog(dialog, body, cancel, create);
    }

    private void editProfile(Employee profile) {
        Avatar preview = new Avatar(96, false);
        preview.setPerson(profile.getName(), profile.getId());
        JDialog dialog = dialog("Edit profile");
        JPanel body = dialogBody(dialog, profile.getName(), "Change the photo or remove this profile.");
        JPanel row = new JPanel(new BorderLayout(16, 0));
        row.setOpaque(false);
        row.add(preview, BorderLayout.WEST);
        JPanel actions = new JPanel(new GridLayout(0, 1, 0, 8));
        actions.setOpaque(false);
        JButton choosePhoto = Theme.subtle(new JButton("Choose photo…"));
        JButton removePhoto = Theme.outline(new JButton("Remove photo"));
        JButton removeProfile = Theme.outline(new JButton("Remove profile"));
        removeProfile.setForeground(Theme.BAD_TEXT);
        removePhoto.setEnabled(Photos.person(profile.getName()) != null);
        actions.add(choosePhoto);
        actions.add(removePhoto);
        actions.add(removeProfile);
        row.add(actions, BorderLayout.CENTER);
        body.add(row);

        choosePhoto.addActionListener(event -> {
            File file = pickImage();
            if (file != null) {
                try {
                    Photos.savePerson(profile.getName(), file);
                    preview.repaint();
                    removePhoto.setEnabled(true);
                    rebuild();
                } catch (IOException error) {
                    UiSupport.perform(dialog, () -> { throw new IllegalArgumentException(error.getMessage()); }, () -> { });
                }
            }
        });
        removePhoto.addActionListener(event -> {
            try {
                Photos.removePerson(profile.getName());
            } catch (IOException ignored) {
                // nothing else to do: the picture stays
            }
            preview.repaint();
            removePhoto.setEnabled(Photos.person(profile.getName()) != null);
            rebuild();
        });
        removeProfile.addActionListener(event -> {
            if (UiSupport.confirm(dialog, "Remove the profile " + profile.getName()
                    + "? Past records keep the name. A manager can bring it back in Manager Tools > Staff.")) {
                UiSupport.perform(dialog, () -> services.staff().removeProfile(login.employeeId(), profile.getId()),
                        () -> {
                            dialog.dispose();
                            rebuild();
                        });
            }
        });

        JButton close = new JButton("Done");
        close.addActionListener(event -> dialog.dispose());
        showDialog(dialog, body, close);
    }

    private File pickImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a photo");
        chooser.setFileFilter(new FileNameExtensionFilter("Pictures (.png, .jpg)", "png", "jpg", "jpeg"));
        chooser.setAcceptAllFileFilterUsed(false);
        return chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
    }

    private JDialog dialog(String title) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        return dialog;
    }

    private static JPanel dialogBody(JDialog dialog, String title, String help) {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        JLabel heading = new JLabel(title);
        heading.setFont(Theme.font(Font.BOLD, 20f));
        heading.setAlignmentX(LEFT_ALIGNMENT);
        body.add(heading);
        body.add(Box.createVerticalStrut(4));
        JLabel note = UiSupport.muted(help);
        note.setAlignmentX(LEFT_ALIGNMENT);
        body.add(note);
        body.add(Box.createVerticalStrut(16));
        return body;
    }

    private static void showDialog(JDialog dialog, JPanel body, JButton... buttons) {
        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setBackground(Theme.PAPER);
        content.setBorder(BorderFactory.createEmptyBorder(20, 22, 18, 22));
        for (Component child : body.getComponents()) {
            if (child instanceof JComponent component) {
                component.setAlignmentX(LEFT_ALIGNMENT);
            }
        }
        content.add(body, BorderLayout.CENTER);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        row.setOpaque(false);
        for (JButton button : buttons) {
            row.add(button);
        }
        content.add(row, BorderLayout.SOUTH);
        dialog.setContentPane(content);
        dialog.getRootPane().setDefaultButton(buttons[buttons.length - 1]);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(440, dialog.getHeight()));
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(dialog.getOwner());
        dialog.setVisible(true);
    }

    private static JLabel bold(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(Font.BOLD, 12.5f));
        return label;
    }

    private static void darkOutline(JButton button) {
        button.setBackground(BACKGROUND);
        button.setForeground(SOFT_TEXT);
        button.putClientProperty(Theme.OUTLINE, new Color(0x7A6A5C));
        button.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
    }

    // ---------- Tiles ----------

    /** One profile picture with the name under it. name == null draws the "Add profile" tile. */
    private final class ProfileTile extends JComponent {
        private final String name;
        private final String colorKey;
        private final Runnable action;
        private boolean hover;

        ProfileTile(String name, String colorKey, Runnable action) {
            this.name = name;
            this.colorKey = colorKey;
            this.action = action;
            setPreferredSize(new Dimension(TILE, TILE + 40));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFocusable(true);
            setToolTipText(name == null ? "Add a new profile" : managing ? "Edit " + name : "Log in as " + name);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) {
                    hover = false;
                    action.run();
                }
            });
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { repaint(); }
                @Override public void focusLost(FocusEvent e) { repaint(); }
            });
            getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "open");
            getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "open");
            getActionMap().put("open", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { action.run(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            boolean active = hover || isFocusOwner();
            float arc = TILE * 0.18f;
            if (name == null) {
                g2.setColor(active ? new Color(0x4A3C32) : new Color(0x43362D));
                g2.fill(new RoundRectangle2D.Float(1, 1, TILE - 2, TILE - 2, arc, arc));
                g2.setColor(active ? Theme.ORANGE : new Color(0x8C7B6D));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        10f, new float[]{8f, 7f}, 0f));
                g2.draw(new RoundRectangle2D.Float(1, 1, TILE - 2, TILE - 2, arc, arc));
                Icons.paint(g2, "plus", (TILE - 54) / 2, (TILE - 54) / 2, 54, active ? Theme.ORANGE : SOFT_TEXT);
            } else {
                Avatar.paintAvatar(g2, name, colorKey, 0, 0, TILE, false);
                if (managing) {
                    g2.setColor(new Color(30, 22, 17, 175));
                    g2.fill(new RoundRectangle2D.Float(0, 0, TILE, TILE, arc, arc));
                    g2.setColor(Theme.WHITE);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawOval(TILE / 2 - 26, TILE / 2 - 26, 52, 52);
                    Icons.paint(g2, "pencil", TILE / 2 - 14, TILE / 2 - 14, 28, Theme.WHITE);
                }
            }
            if (active) {
                g2.setColor(name == null ? Theme.ORANGE : Theme.WHITE);
                g2.setStroke(new BasicStroke(4f));
                g2.draw(new RoundRectangle2D.Float(2, 2, TILE - 4, TILE - 4, arc, arc));
            }
            String full = name == null ? "Add profile" : name;
            g2.setFont(Theme.font(active ? Font.BOLD : Font.PLAIN, 15f));
            FontMetrics fm = g2.getFontMetrics();
            String text = full;
            for (int keep = full.length() - 1; fm.stringWidth(text) > TILE && keep > 3; keep--) {
                text = full.substring(0, keep).trim() + "…";
            }
            g2.setColor(active ? Theme.WHITE : SOFT_TEXT);
            g2.drawString(text, (TILE - fm.stringWidth(text)) / 2, TILE + 14 + fm.getAscent());
            g2.dispose();
        }
    }
}
