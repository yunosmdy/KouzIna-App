package service;

import domain.enums.OrderStatus;

/** Observer na inaabisuhan pagkatapos ma-commit ang order-status change. */
@FunctionalInterface
public interface OrderListener {
    void orderStatusChanged(String orderId, OrderStatus newStatus);
}
