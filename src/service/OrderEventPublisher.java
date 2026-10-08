package service;

import domain.enums.OrderStatus;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/** Inaabisuhan ang naka-register na UI listeners kapag committed na ang order change. */
public final class OrderEventPublisher {
    private final CopyOnWriteArrayList<OrderListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(OrderListener listener) {
        listeners.addIfAbsent(Objects.requireNonNull(listener, "Order listener is required."));
    }

    public void publish(String orderId, OrderStatus status) {
        for (OrderListener listener : listeners) {
            try {
                listener.orderStatusChanged(orderId, status);
            } catch (RuntimeException ignored) {
                // Committed na ang action, kaya hindi ito dapat ma-rollback kapag pumalya ang UI listener.
            }
        }
    }
}
