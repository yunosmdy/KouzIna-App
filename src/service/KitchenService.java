package service;

import domain.AuditLog;
import domain.DiningSession;
import domain.Order;
import domain.RestaurantTable;
import domain.enums.OrderStatus;
import domain.enums.Permission;
import persistence.AppState;
import persistence.AppStateRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Dito kinukuha yung kitchen queue, check permission muna bago palit status. */
public final class KitchenService {
    private final AppStateRepository repository;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final OrderEventPublisher orderEvents;

    public KitchenService(
            AppStateRepository repository, IdGenerator idGenerator, Clock clock) {
        this(repository, idGenerator, clock, new OrderEventPublisher());
    }

    public KitchenService(
            AppStateRepository repository,
            IdGenerator idGenerator,
            Clock clock,
            OrderEventPublisher orderEvents) {
        this.repository = Objects.requireNonNull(repository, "Repository is required.");
        this.idGenerator = Objects.requireNonNull(idGenerator, "ID generator is required.");
        this.clock = Objects.requireNonNull(clock, "Clock is required.");
        this.orderEvents = Objects.requireNonNull(orderEvents, "Order events are required.");
    }

    /** Build ulit yung queue gamit current orders, same order kung pano na-add. */
    public List<KitchenTicket> getQueue(String actorId) {
        AppState state = repository.snapshot();
        AuthorizationService.require(state, actorId, Permission.VIEW_KITCHEN_QUEUE);

        return KitchenQueue.from(state).tickets();
    }

    public void markPreparing(String actorId, String orderId) {
        changeStatus(
                actorId,
                orderId,
                Permission.UPDATE_KITCHEN_STATUS,
                Order::markPreparing,
                OrderStatus.PREPARING,
                "ORDER_PREPARING",
                "Marked order %s as preparing at table %d.");
    }

    public void markReady(String actorId, String orderId) {
        changeStatus(
                actorId,
                orderId,
                Permission.UPDATE_KITCHEN_STATUS,
                Order::markReady,
                OrderStatus.READY,
                "ORDER_READY",
                "Marked order %s as ready at table %d.");
    }

    public void markServed(String actorId, String orderId) {
        changeStatus(
                actorId,
                orderId,
                Permission.MARK_ORDERS_SERVED,
                Order::markServed,
                OrderStatus.SERVED,
                "ORDER_SERVED",
                "Marked order %s as served at table %d.");
    }

    private void changeStatus(
            String actorId,
            String orderId,
            Permission permission,
            Consumer<Order> transition,
            OrderStatus newStatus,
            String auditAction,
            String auditDetailFormat) {
        repository.transact(state -> {
            AuthorizationService.require(state, actorId, permission);
            Order order = state.getOrderOrThrow(orderId);
            RestaurantTable table = resolveTable(state, order);

            transition.accept(order);
            state.addAuditLog(new AuditLog(
                    idGenerator.nextId(),
                    LocalDateTime.now(clock),
                    actorId,
                    auditAction,
                    auditDetailFormat.formatted(order.getId(), table.getTableNumber())));
            return null;
        });
        orderEvents.publish(orderId, newStatus);
    }

    private RestaurantTable resolveTable(AppState state, Order order) {
        DiningSession session = state.getSessionOrThrow(order.getSessionId());
        return state.getTableOrThrow(session.getTableId());
    }

}
