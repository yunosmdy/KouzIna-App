package ui;

/** Lets a screen open another screen (implemented by KouzinaFrame). */
public interface Navigator {
    /** Opens a screen by its key, e.g. KouzinaFrame.ORDERING. */
    void open(String screen);

    /** Opens a screen and selects this order there (null = keep the current selection). */
    void open(String screen, String orderId);

    /** Whether the signed-in person is allowed to use that screen. */
    boolean canOpen(String screen);
}
