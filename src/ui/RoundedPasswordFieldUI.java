package ui;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPasswordFieldUI;
import java.awt.Graphics;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/** Same look as RoundedTextFieldUI, for password fields. */
public class RoundedPasswordFieldUI extends BasicPasswordFieldUI {
    private final FocusListener repaintOnFocus = new FocusAdapter() {
        @Override public void focusGained(FocusEvent e) { getComponent().repaint(); }
        @Override public void focusLost(FocusEvent e) { getComponent().repaint(); }
    };

    public static ComponentUI createUI(JComponent c) {
        return new RoundedPasswordFieldUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        getComponent().setOpaque(false);
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        getComponent().addFocusListener(repaintOnFocus);
    }

    @Override
    protected void uninstallListeners() {
        getComponent().removeFocusListener(repaintOnFocus);
        super.uninstallListeners();
    }

    @Override
    protected void paintSafely(Graphics g) {
        RoundedTextFieldUI.paintField(g, getComponent());
        super.paintSafely(g);
        RoundedTextFieldUI.paintPlaceholder(g, getComponent());
    }
}
