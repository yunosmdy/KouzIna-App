package service;

import domain.DiningSession;
import domain.Order;
import domain.OrderItem;
import domain.RestaurantTable;
import domain.enums.OrderStatus;
import persistence.AppState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Read-only copy ng orders na kailangan asikasuhin sa kitchen. */
public final class KitchenQueue {
    private final List<KitchenTicket> tickets;

    private KitchenQueue(List<KitchenTicket> tickets) {
        this.tickets = List.copyOf(tickets);
    }

    public static KitchenQueue from(AppState state) {
        AppState requiredState = Objects.requireNonNull(state, "Application state is required.");
        List<KitchenTicket> tickets = new ArrayList<>();
        for (Order order : requiredState.getOrders()) {
            if (!isKitchenStatus(order.getStatus())) {
                continue;
            }
            DiningSession session = requiredState.getSessionOrThrow(order.getSessionId());
            RestaurantTable table = requiredState.getTableOrThrow(session.getTableId());
            List<String> summaries = order.getItems().stream()
                    .map(KitchenQueue::summarize)
                    .toList();
            tickets.add(new KitchenTicket(
                    order.getId(),
                    session.getId(),
                    table.getId(),
                    table.getTableNumber(),
                    order.getStatus(),
                    summaries));
        }
        return new KitchenQueue(tickets);
    }

    public List<KitchenTicket> tickets() {
        return tickets;
    }

    private static String summarize(OrderItem item) {
        return item.getQuantity() + " x " + item.getItemName();
    }

    private static boolean isKitchenStatus(OrderStatus status) {
        return status == OrderStatus.CONFIRMED
                || status == OrderStatus.PREPARING
                || status == OrderStatus.READY;
    }
}
