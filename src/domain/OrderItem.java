package domain;

import exception.ValidationException;
import util.Money;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Fixed na snapshot ng item sa order.
 * Naka-save ang price nung idinagdag sa order, kaya hindi mababago ng later menu edits.
 */
public final class OrderItem implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String menuItemId;
    private final String itemName;
    private final BigDecimal unitPrice;
    private final int quantity;

    public OrderItem(MenuItem menuItem, int quantity) {
        this(requireMenuItem(menuItem).getId(), menuItem.getName(), menuItem.getUnitPrice(), quantity);
    }

    public OrderItem(String menuItemId, String itemName, BigDecimal unitPrice, int quantity) {
        this.menuItemId = requireNonBlank(menuItemId, "Menu item ID");
        this.itemName = requireNonBlank(itemName, "Item name");
        this.unitPrice = requireNonNegativeMoney(unitPrice);
        if (quantity <= 0) {
            throw new ValidationException("Order item quantity must be positive.");
        }
        this.quantity = quantity;
    }

    public String getMenuItemId() {
        return menuItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getLineTotal() {
        return Money.multiply(unitPrice, BigDecimal.valueOf(quantity));
    }

    OrderItem withQuantity(int newQuantity) {
        return new OrderItem(menuItemId, itemName, unitPrice, newQuantity);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required.");
        }
        return value.trim();
    }

    private static BigDecimal requireNonNegativeMoney(BigDecimal amount) {
        if (amount == null) {
            throw new ValidationException("Unit price is required.");
        }
        try {
            return Money.requireNonNegative(amount, "Unit price");
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(exception.getMessage());
        }
    }

    private static MenuItem requireMenuItem(MenuItem menuItem) {
        if (menuItem == null) {
            throw new ValidationException("Menu item is required.");
        }
        return menuItem;
    }
}
