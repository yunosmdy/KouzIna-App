package service;

import domain.DiningSession;
import domain.Order;
import persistence.AppState;

/**
 * Short staff-facing references belong to visits, not individual food orders.
 * New visits have a V-number internally; each order has its own independent ID.
 * Deriving the display reference from the visit keeps existing save schemas compatible.
 */
public final class OrderReferences {
    private OrderReferences() { }

    /** Allocate inside the repository transaction so numbering follows saved state. */
    public static String nextVisitId(AppState state) {
        long maximum = 0;
        for (Order order : state.getOrders()) maximum = Math.max(maximum, number(order.getId()));
        for (DiningSession visit : state.getSessions()) maximum = Math.max(maximum, number(visit.getId()));
        return "V-" + String.format(java.util.Locale.ROOT, "%04d", Math.addExact(maximum, 1));
    }

    /** Use the orders' own sequence, independently of visit numbering. */
    public static String nextOrderId(AppState state) {
        long maximum = 0;
        for (Order order : state.getOrders()) {
            String id = order.getId();
            if (!id.matches("ORD-[0-9]+")) continue;
            try { maximum = Math.max(maximum, Long.parseLong(id.substring(4))); }
            catch (NumberFormatException error) {
                throw new IllegalStateException("Order reference is too large.", error);
            }
        }
        return "ORD-" + String.format(java.util.Locale.ROOT, "%04d", Math.addExact(maximum, 1));
    }

    private static long number(String id) {
        // O-numbers were previously shared between the visit and order.
        if (!id.matches("[OV]-[0-9]+")) return 0;
        try { return Long.parseLong(id.substring(2)); }
        catch (NumberFormatException error) { throw new IllegalStateException("Visit reference is too large."); }
    }

    /** Follow the order's explicit visit link; never assume their IDs are equal. */
    public static String display(AppState state, String orderId) {
        Order order = state.getOrderOrThrow(orderId);
        return displayVisit(state, order.getSessionId());
    }

    public static String displayVisit(AppState state, String visitId) {
        DiningSession visit = state.getSessionOrThrow(visitId);
        if (visit.getId().matches("V-[0-9]+")) return "O-" + visit.getId().substring(2);

        // Preserve references already shown for existing saved visits.
        String originalOrderId = visit.getOrderId();
        if (originalOrderId.matches("O-[0-9]+")) return originalOrderId;
        int index = 0;
        for (Order originalOrder : state.getOrders()) {
            index++;
            if (originalOrder.getId().equals(originalOrderId))
                return String.format(java.util.Locale.ROOT, "L-%04d", index);
        }
        throw new IllegalArgumentException("The visit's original order was not found.");
    }
}
