package ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;

/**
 * Kóuz 'Inà-styled pop-ups. They stay open when something is wrong, show the problem in red
 * right inside the pop-up, and let the user fix it and try again (no need to start over).
 */
final class Dialogs {
    static final Color ERROR = Theme.BAD_TEXT;
    static final Color WARNING = Theme.WAIT_TEXT;
    static final Color OK = Theme.GOOD_TEXT;

    private Dialogs() {
    }

    /** Empty modal pop-up on top of the given screen. Escape closes it. */
    static JDialog create(Component parent, String title) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), title,
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        return dialog;
    }

    /** Column with a big heading and a grey line under it. Add the rest with add(...). */
    static JPanel body(String heading, String subheading) {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        JLabel title = new JLabel(heading);
        title.setFont(Theme.font(Font.BOLD, 20f));
        add(body, title, 4);
        if (subheading != null) {
            add(body, UiSupport.muted(subheading), 14);
        }
        return body;
    }

    /** Adds a component at full width, followed by some space. */
    static void add(JPanel body, JComponent component, int spaceAfter) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(component);
        if (spaceAfter > 0) {
            body.add(Box.createVerticalStrut(spaceAfter));
        }
    }

    static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(Font.BOLD, 12.5f));
        return label;
    }

    /** Line that shows problems (red), warnings (amber), or good news (green). */
    static JLabel message() {
        JLabel label = new JLabel(" ");
        label.setFont(Theme.font(Font.BOLD, 13f));
        return label;
    }

    static void say(JLabel label, String text, Color color) {
        label.setForeground(color);
        label.setText(text == null || text.isBlank() ? " "
                : "<html><div style='width:360px'>" + text.replace("&", "&amp;").replace("<", "&lt;") + "</div></html>");
    }

    /** White card with "label .... value" rows, e.g. a bill breakdown. */
    static RoundedPanel summary() {
        RoundedPanel card = new RoundedPanel(new GridLayout(0, 1, 0, 4), Theme.WHITE, 16).padding(12, 14, 12, 14);
        return card;
    }

    static JLabel[] row(JPanel summary, String name, String value, boolean strong) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        JLabel left = new JLabel(name);
        JLabel right = new JLabel(value, JLabel.RIGHT);
        float size = strong ? 18f : 13.5f;
        left.setFont(Theme.font(strong ? Font.BOLD : Font.PLAIN, strong ? 15f : 13.5f));
        right.setFont(Theme.font(strong ? Font.BOLD : Font.PLAIN, size));
        if (!strong) {
            left.setForeground(Theme.MUTED);
        }
        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        summary.add(row);
        return new JLabel[]{left, right};
    }

    /** Lays out the pop-up (body + buttons on the bottom right) and shows it. Blocks until closed. */
    static void show(JDialog dialog, JComponent body, int width, JButton... buttons) {
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(Theme.PAPER);
        content.setBorder(BorderFactory.createEmptyBorder(20, 22, 18, 22));
        content.add(body, BorderLayout.CENTER);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        row.setOpaque(false);
        for (JButton button : buttons) {
            row.add(button);
        }
        content.add(row, BorderLayout.SOUTH);
        dialog.setContentPane(content);
        if (buttons.length > 0) {
            dialog.getRootPane().setDefaultButton(buttons[buttons.length - 1]);
        }
        dialog.pack();
        dialog.setSize(new Dimension(Math.max(width, dialog.getWidth()), dialog.getHeight()));
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(dialog.getOwner());
        UiSupport.installWheelForwarding(content);
        dialog.setVisible(true);
    }

    /**
     * Asks the user to pick one of the buttons. Returns the index of the chosen button
     * (0 = first), or -1 when the pop-up was closed. The last button is the main one.
     */
    static int choose(Component parent, String title, String heading, String message, String... options) {
        JDialog dialog = create(parent, title);
        JPanel body = body(heading, null);
        JLabel text = new JLabel("<html><div style='width:380px'>"
                + message.replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>") + "</div></html>");
        text.setFont(Theme.font(Font.PLAIN, 14f));
        add(body, text, 0);
        int[] chosen = {-1};
        JButton[] buttons = new JButton[options.length];
        for (int i = 0; i < options.length; i++) {
            int index = i;
            JButton button = new JButton(options[i]);
            if (i < options.length - 1) {
                Theme.outline(button);
            }
            button.addActionListener(e -> {
                chosen[0] = index;
                dialog.dispose();
            });
            buttons[i] = button;
        }
        show(dialog, body, 440, buttons);
        return chosen[0];
    }

    /** Peso amount for pop-ups. */
    static String peso(BigDecimal amount) {
        return UiSupport.peso(amount);
    }
}
