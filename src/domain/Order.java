package domain;

import domain.enums.OrderStatus;
import exception.ConflictException;
import exception.InvalidTransitionException;
import exception.ValidationException;
import util.Money;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Isang order lang sa bawat dining session. */
public final class Order implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String sessionId;
    private final List<OrderItem> items;
    private OrderStatus status;

    public Order(String id, String sessionId) {
        this.id = requireNonBlank(id, "Order ID");
        this.sessionId = requireNonBlank(sessionId, "Session ID");
        this.items = new ArrayList<>();
        this.status = OrderStatus.DRAFT;
    }

    public String getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    /** Kopya ng current items ang ibinabalik; hindi puwedeng baguhin ang list. */
    public List<OrderItem> getItems() {
        return List.copyOf(items);
    }

    public BigDecimal getSubtotal() {
        BigDecimal subtotal = Money.ZERO;
        for (OrderItem item : items) {
            subtotal = Money.add(subtotal, item.getLineTotal());
        }
        return subtotal;
    }

    public void addItem(OrderItem item) {
        requireDraft("add items to");
        if (item == null) {
            throw new ValidationException("Order item is required.");
        }
        if (findItemIndex(item.getMenuItemId()) >= 0) {
            throw new ConflictException(
                    "Menu item " + item.getMenuItemId() + " is already in the order.");
        }
        items.add(item);
    }

    public void addItem(MenuItem menuItem, int quantity) {
        addItem(new OrderItem(menuItem, quantity));
    }

    public void addItem(String menuItemId, String itemName, BigDecimal unitPrice, int quantity) {
        addItem(new OrderItem(menuItemId, itemName, unitPrice, quantity));
    }

    public void updateItemQuantity(String menuItemId, int quantity) {
        requireDraft("update items in");
        String normalizedId = requireNonBlank(menuItemId, "Menu item ID");
        if (quantity <= 0) {
            throw new ValidationException("Order item quantity must be positive.");
        }
        int itemIndex = findItemIndex(normalizedId);
        if (itemIndex < 0) {
            throw new ValidationException("Menu item " + normalizedId + " is not in the order.");
        }
        items.set(itemIndex, items.get(itemIndex).withQuantity(quantity));
    }

    public void removeItem(String menuItemId) {
        requireDraft("remove items from");
        String normalizedId = requireNonBlank(menuItemId, "Menu item ID");
        int itemIndex = findItemIndex(normalizedId);
        if (itemIndex < 0) {
            throw new ValidationException("Menu item " + normalizedId + " is not in the order.");
        }
        items.remove(itemIndex);
    }

    public void confirm() {
        requireStatus(OrderStatus.DRAFT, "confirm");
        if (items.isEmpty()) {
            throw new ValidationException("An order must contain at least one item before confirmation.");
        }
        status = OrderStatus.CONFIRMED;
    }

    /** Ibinabalik sa DRAFT ang order na naipadala na pero hindi pa sinisimulang lutuin. */
    public void recall() {
        transition(OrderStatus.CONFIRMED, OrderStatus.DRAFT, "take back from the kitchen");
    }

    public void markPreparing() {
        transition(OrderStatus.CONFIRMED, OrderStatus.PREPARING, "mark as preparing");
    }

    public void markReady() {
        transition(OrderStatus.PREPARING, OrderStatus.READY, "mark as ready");
    }

    public void markServed() {
        transition(OrderStatus.READY, OrderStatus.SERVED, "mark as served");
    }

    /** Puwede lang mag-cancel bago magsimula ang kitchen preparation. */
    public void cancel() {
        if (status != OrderStatus.DRAFT && status != OrderStatus.CONFIRMED) {
            throw invalidTransition("cancel");
        }
        status = OrderStatus.CANCELLED;
    }

    private void transition(OrderStatus expected, OrderStatus target, String action) {
        requireStatus(expected, action);
        status = target;
    }

    private void requireDraft(String action) {
        requireStatus(OrderStatus.DRAFT, action);
    }

    private void requireStatus(OrderStatus expected, String action) {
        if (status != expected) {
            throw invalidTransition(action);
        }
    }

    private InvalidTransitionException invalidTransition(String action) {
        return new InvalidTransitionException(
                "Cannot " + action + " order while status is " + status + ".");
    }

    private int findItemIndex(String menuItemId) {
        for (int index = 0; index < items.size(); index++) {
            if (items.get(index).getMenuItemId().equals(menuItemId)) {
                return index;
            }
        }
        return -1;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required.");
        }
        return value.trim();
    }
}
