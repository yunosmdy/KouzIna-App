package ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.UIManager;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.Color;
import java.awt.Font;

/**
 * Isang lugar para sa lahat ng kulay at font ng app.
 * Palitan lang ang mga Color dito para mag-iba ang buong UI.
 *
 * Palette: "french vintage" (coolors)
 *   Orange  #DD9B64  - main tone: buttons, selected sidebar item, selected tabs/chips, login panel
 *   Blue    #C0D4D8  - second tone: table headers, selections, hover, Refresh buttons
 *   Green   #7C9362 / #9EB587 - third tone: Log out, status badge, scrollbars, stock text
 *   Cream   #ECE8DF  - sidebar and window background
 *
 * Status indicators use soft versions of green and red that match the palette (see toneOf).
 */
public final class Theme {

    // ---- Palette (edit these) ----
    public static final Color ORANGE = new Color(0xDD9B64);
    public static final Color ORANGE_DARK = new Color(0xC7824B);  // outlines, icons on tint
    public static final Color ORANGE_TINT = new Color(0xF6E3D0);  // field boxes, picture placeholders
    public static final Color CREAM = new Color(0xECE8DF);
    public static final Color PAPER = new Color(0xF7F5F0);        // main content background
    public static final Color WHITE = new Color(0xFFFFFF);        // cards, inputs, tables
    public static final Color BLUE = new Color(0xC0D4D8);
    public static final Color BLUE_DARK = new Color(0xA3BEC4);
    public static final Color BLUE_TINT = new Color(0xE2ECEE);    // hover
    public static final Color SAGE = new Color(0x9EB587);
    public static final Color GREEN = new Color(0x7C9362);
    public static final Color GREEN_DARK = new Color(0x667B4F);

    // ---- Neutrals ----
    public static final Color TEXT = new Color(0x3A2E26);         // dark brown text
    public static final Color MUTED = new Color(0x8C8177);        // secondary text
    public static final Color LINE = new Color(0xE0D9CC);         // borders, grid lines

    // ---- Status colors (soft green / red, plus amber for "in progress", blue for "finished") ----
    public static final Color GOOD_BG = new Color(0xE1EAD7);
    public static final Color GOOD_DOT = new Color(0x7C9362);
    public static final Color GOOD_TEXT = new Color(0x4E6338);
    public static final Color BAD_BG = new Color(0xF5DDD8);
    public static final Color BAD_DOT = new Color(0xCC7F6E);
    public static final Color BAD_TEXT = new Color(0x9A4A3A);
    public static final Color WAIT_BG = new Color(0xF7E6D3);
    public static final Color WAIT_DOT = new Color(0xDD9B64);
    public static final Color WAIT_TEXT = new Color(0x9A6234);
    public static final Color DONE_BG = new Color(0xDFEAEC);
    public static final Color DONE_DOT = new Color(0x8FB0B7);
    public static final Color DONE_TEXT = new Color(0x4C6A71);

    /** App name shown in the window title, sidebar, login, and dialogs. */
    public static final String BRAND = "Kóuz 'Inà";

    // ---- Fonts ----
    public static final String FONT_FAMILY = "Segoe UI";
    public static final Font BASE_FONT = new Font(FONT_FAMILY, Font.PLAIN, 14);

    /** Client property keys read by the custom UI classes. */
    public static final String ARC = "Kouzina.arc";
    static final String OUTLINE = "Kouzina.outline";
    static final String PLACEHOLDER = "Kouzina.placeholder";

    private Theme() {
    }

    public static Font font(int style, float size) {
        return BASE_FONT.deriveFont(style, size);
    }

    /** Call once at startup, before any window is created. */
    public static void apply() {
        UIManager.put("swing.boldMetal", Boolean.FALSE);
        MetalLookAndFeel.setCurrentTheme(new KouzinaMetalTheme());
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (Exception ignored) {
            // falls back to the default look and feel
        }

        // Custom rounded components (see the *UI.java files in this folder)
        ui("ButtonUI", RoundedButtonUI.class);
        ui("TextFieldUI", RoundedTextFieldUI.class);
        ui("PasswordFieldUI", RoundedPasswordFieldUI.class);
        ui("TabbedPaneUI", PillTabbedPaneUI.class);
        ui("ScrollBarUI", ThinScrollBarUI.class);
        ui("ComboBoxUI", RoundedComboBoxUI.class);
        UIManager.put("SplitPaneUI", "javax.swing.plaf.basic.BasicSplitPaneUI");

        // Window / panels
        put("Panel.background", PAPER);
        put("OptionPane.background", PAPER);
        put("OptionPane.messageForeground", TEXT);
        put("Label.foreground", TEXT);
        put("ToolTip.background", BLUE);
        put("ToolTip.foreground", TEXT);
        UIManager.put("ToolTip.border", new BorderUIResource(BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        // Buttons: orange by default
        put("Button.background", ORANGE);
        put("Button.foreground", TEXT);
        UIManager.put("Button.font", new FontUIResource(font(Font.BOLD, 13f)));

        // Text inputs
        for (String key : new String[]{"TextField", "PasswordField", "FormattedTextField", "TextArea"}) {
            put(key + ".background", WHITE);
            put(key + ".foreground", TEXT);
            put(key + ".caretForeground", TEXT);
            put(key + ".selectionBackground", BLUE);
            put(key + ".selectionForeground", TEXT);
            put(key + ".inactiveForeground", MUTED);
        }
        BorderUIResource fieldPadding = new BorderUIResource(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        UIManager.put("TextField.border", fieldPadding);
        UIManager.put("PasswordField.border", fieldPadding);
        UIManager.put("TextArea.margin", new javax.swing.plaf.InsetsUIResource(8, 10, 8, 10));

        // Combo boxes / lists
        put("ComboBox.background", WHITE);
        put("ComboBox.foreground", TEXT);
        put("ComboBox.selectionBackground", BLUE);
        put("ComboBox.selectionForeground", TEXT);
        put("ComboBox.buttonBackground", WHITE);
        put("ComboBox.buttonShadow", LINE);
        put("ComboBox.buttonDarkShadow", MUTED);
        put("ComboBox.buttonHighlight", WHITE);
        put("List.background", WHITE);
        put("List.selectionBackground", BLUE);
        put("List.selectionForeground", TEXT);

        // Tables (more styling in UiSupport.tablePane)
        put("Table.background", WHITE);
        put("Table.foreground", TEXT);
        put("Table.selectionBackground", BLUE);
        put("Table.selectionForeground", TEXT);
        put("Table.gridColor", LINE);
        put("TableHeader.background", BLUE);
        put("TableHeader.foreground", TEXT);
        UIManager.put("Table.focusCellHighlightBorder",
                new BorderUIResource(BorderFactory.createEmptyBorder(0, 10, 0, 10)));

        // Scroll panes / scrollbars
        put("ScrollPane.background", PAPER);
        put("Viewport.background", WHITE);
        UIManager.put("ScrollPane.border", new BorderUIResource(new RoundedLineBorder(LINE, 14, 1)));
        UIManager.put("ScrollBar.width", 13);

        // Tabs (painted as pills by PillTabbedPaneUI)
        put("TabbedPane.background", PAPER);
        put("TabbedPane.foreground", TEXT);
        put("TabbedPane.selectedForeground", TEXT);
        UIManager.put("TabbedPane.font", new FontUIResource(font(Font.BOLD, 13f)));
        UIManager.put("TabbedPane.contentOpaque", Boolean.FALSE);
        UIManager.put("TabbedPane.tabsOpaque", Boolean.TRUE);

        // Split panes: flat divider
        put("SplitPane.background", PAPER);
        UIManager.put("SplitPane.border", new BorderUIResource(BorderFactory.createEmptyBorder()));
        UIManager.put("SplitPaneDivider.border", new BorderUIResource(BorderFactory.createEmptyBorder()));
        UIManager.put("SplitPane.dividerSize", 12);
    }

    // ---- Status indicator colors ----

    /** Color group for a status indicator. */
    public enum Tone { GOOD, BAD, WAIT, DONE }

    /**
     * Which color each status gets. Edit this to change the indicators everywhere.
     * GOOD = green, BAD = red, WAIT = amber (in progress), DONE = blue (finished / closed).
     */
    public static Tone toneOf(String status) {
        return switch (status.toUpperCase().replace(' ', '_')) {
            case "AVAILABLE", "CONFIRMED", "CHECKED_IN", "COMPLETED", "SEATED", "READY",
                 "SERVED", "PAID", "OPEN", "ACTIVE", "TRUE", "IN_STOCK", "YES" -> Tone.GOOD;
            case "CANCELLED", "NO_SHOW", "VOIDED", "UNPAID", "OCCUPIED", "INACTIVE",
                 "FALSE", "SOLD_OUT", "NO" -> Tone.BAD;
            case "PENDING", "DRAFT", "PREPARING", "WAITING", "LOW_STOCK", "TAKING_ORDER",
                 "SENT_TO_KITCHEN", "RESERVED" -> Tone.WAIT;
            default -> Tone.DONE;
        };
    }

    public static Color[] toneColors(Tone tone) {
        return switch (tone) {
            case GOOD -> new Color[]{GOOD_BG, GOOD_DOT, GOOD_TEXT};
            case BAD -> new Color[]{BAD_BG, BAD_DOT, BAD_TEXT};
            case WAIT -> new Color[]{WAIT_BG, WAIT_DOT, WAIT_TEXT};
            case DONE -> new Color[]{DONE_BG, DONE_DOT, DONE_TEXT};
        };
    }

    /** Colors for profile pictures without a photo (picked from the person's ID). */
    private static final Color[] AVATAR_COLORS = {
            ORANGE_DARK, GREEN, DONE_TEXT, BAD_DOT, WAIT_TEXT, new Color(0x8FB0B7), GREEN_DARK};

    public static Color avatarColor(String key) {
        return AVATAR_COLORS[Math.floorMod(key == null ? 0 : key.hashCode(), AVATAR_COLORS.length)];
    }

    /** "NO_SHOW" -> "No show", true -> "Active". */
    public static String statusLabel(Object value) {
        if (value instanceof Boolean flag) {
            return flag ? "Active" : "Inactive";
        }
        String text = String.valueOf(value).replace('_', ' ').toLowerCase();
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    // ---- Button styles you can use in any panel ----

    /** Orange main-action button (this is already the default for every JButton). */
    public static JButton primary(JButton button) {
        return style(button, ORANGE, TEXT, null);
    }

    /** Green button, e.g. Log out. */
    public static JButton secondary(JButton button) {
        return style(button, GREEN, Color.WHITE, null);
    }

    /** Blue button for neutral actions like Refresh. */
    public static JButton subtle(JButton button) {
        return style(button, BLUE, TEXT, null);
    }

    /** White outlined button, e.g. Cancel. */
    public static JButton outline(JButton button) {
        return style(button, WHITE, TEXT, LINE);
    }

    /** Pill-shaped filter chip (orange when selected). */
    public static JButton chip(JButton button, boolean selected) {
        style(button, selected ? ORANGE : WHITE, TEXT, selected ? null : LINE);
        button.putClientProperty(ARC, 999);
        button.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        return button;
    }

    private static JButton style(JButton button, Color background, Color foreground, Color outline) {
        button.setBackground(background);
        button.setForeground(foreground);
        button.putClientProperty(OUTLINE, outline);
        button.repaint();
        return button;
    }

    private static void ui(String key, Class<?> uiClass) {
        UIManager.put(key, uiClass.getName());
        UIManager.put(uiClass.getName(), uiClass);
    }

    private static void put(String key, Color color) {
        UIManager.put(key, new ColorUIResource(color));
    }

    /** Metal theme base colors: controls cream, highlights blue, scrollbars green. */
    private static final class KouzinaMetalTheme extends DefaultMetalTheme {
        private final FontUIResource regular = new FontUIResource(BASE_FONT);
        private final FontUIResource bold = new FontUIResource(font(Font.BOLD, 14f));
        private final FontUIResource small = new FontUIResource(font(Font.PLAIN, 12f));

        @Override public String getName() { return "Kouzina"; }

        @Override protected ColorUIResource getPrimary1() { return new ColorUIResource(GREEN); }
        @Override protected ColorUIResource getPrimary2() { return new ColorUIResource(SAGE); }
        @Override protected ColorUIResource getPrimary3() { return new ColorUIResource(BLUE); }
        @Override protected ColorUIResource getSecondary1() { return new ColorUIResource(MUTED); }
        @Override protected ColorUIResource getSecondary2() { return new ColorUIResource(LINE); }
        @Override protected ColorUIResource getSecondary3() { return new ColorUIResource(PAPER); }
        @Override protected ColorUIResource getBlack() { return new ColorUIResource(TEXT); }
        @Override protected ColorUIResource getWhite() { return new ColorUIResource(WHITE); }

        @Override public FontUIResource getControlTextFont() { return regular; }
        @Override public FontUIResource getSystemTextFont() { return regular; }
        @Override public FontUIResource getUserTextFont() { return regular; }
        @Override public FontUIResource getMenuTextFont() { return regular; }
        @Override public FontUIResource getWindowTitleFont() { return bold; }
        @Override public FontUIResource getSubTextFont() { return small; }
    }
}
