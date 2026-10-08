package ui;

/** Screens that can select a specific order when they are opened from another step. */
interface OrderFocus {
    void focusOrder(String orderId);
}
