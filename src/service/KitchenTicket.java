package service;

import domain.enums.OrderStatus;

import java.util.List;
import java.util.Objects;

/** Order details at table para sa kitchen, pang basa lang to. */
public record KitchenTicket(
        String orderId,
        String sessionId,
        String tableId,
        int tableNumber,
        OrderStatus status,
        List<String> itemSummaries) {

    public KitchenTicket {
        orderId = requireNonBlank(orderId, "Order ID");
        sessionId = requireNonBlank(sessionId, "Session ID");
        tableId = requireNonBlank(tableId, "Table ID");
        if (tableNumber <= 0) {
            throw new IllegalArgumentException("Table number must be positive.");
        }
        status = Objects.requireNonNull(status, "Order status is required.");
        itemSummaries = List.copyOf(
                Objects.requireNonNull(itemSummaries, "Item summaries are required."));
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }
}
