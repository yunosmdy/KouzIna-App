package service;

import domain.AuditLog;
import domain.DiningSession;
import domain.MenuItem;
import domain.Order;
import domain.OrderItem;
import domain.Reservation;
import domain.RestaurantTable;
import domain.enums.OrderStatus;
import domain.enums.Permission;
import domain.enums.ReservationStatus;
import exception.InsufficientStockException;
import exception.InvalidTransitionException;
import exception.UnavailableException;
import persistence.AppState;
import persistence.AppStateRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/** Humahawak sa draft edits, stock deduction sa isang transaction, at order cancellation. */
public final class OrderService {
    private final AppStateRepository repository;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final OrderEventPublisher orderEvents;

    public OrderService(
            AppStateRepository repository, IdGenerator idGenerator, Clock clock) {
        this(repository, idGenerator, clock, new OrderEventPublisher());
    }

    public OrderService(
            AppStateRepository repository,
            IdGenerator idGenerator,
            Clock clock,
            OrderEventPublisher orderEvents) {
        this.repository = Objects.requireNonNull(repository, "Repository is required.");
        this.idGenerator = Objects.requireNonNull(idGenerator, "ID generator is required.");
        this.clock = Objects.requireNonNull(clock, "Clock is required.");
        this.orderEvents = Objects.requireNonNull(orderEvents, "Order events are required.");
    }

    /** Idinadagdag ang item kasama ang current name at price nito sa order. */
    public void addItem(
            String sessionToken, String orderId, String menuItemId, int quantity) {
        repository.transact(state -> {
            AuthorizationService.require(state, sessionToken, Permission.MANAGE_DRAFT_ORDERS);
            Order order = state.getOrderOrThrow(orderId);
            requireDraft(order, "add items to");
            MenuItem menuItem = state.getMenuItemOrThrow(menuItemId);
            order.addItem(menuItem, quantity);
            return null;
        });
    }

    public void updateItemQuantity(
            String sessionToken, String orderId, String menuItemId, int quantity) {
        repository.transact(state -> {
            AuthorizationService.require(state, sessionToken, Permission.MANAGE_DRAFT_ORDERS);
            state.getOrderOrThrow(orderId).updateItemQuantity(menuItemId, quantity);
            return null;
        });
    }

    public void removeItem(String sessionToken, String orderId, String menuItemId) {
        repository.transact(state -> {
            AuthorizationService.require(state, sessionToken, Permission.MANAGE_DRAFT_ORDERS);
            state.getOrderOrThrow(orderId).removeItem(menuItemId);
            return null;
        });
    }

    /** Chine-check muna ang buong order bago magbawas ng stock. */
    public void confirmOrder(String sessionToken, String orderId) {
        repository.transact(state -> {
            AuthorizationService.require(state, sessionToken, Permission.CONFIRM_ORDERS);
            Order order = state.getOrderOrThrow(orderId);
            requireDraft(order, "confirm");

            // Check muna lahat ng items bago i-deduct ang stock, para walang half-done na order.
            for (OrderItem orderItem : order.getItems()) {
                MenuItem menuItem = state.getMenuItemOrThrow(orderItem.getMenuItemId());
                if (!menuItem.isActive()) {
                    throw new UnavailableException(
                            menuItem.getName() + " is currently unavailable. Remove it from this order.");
                }
                if (menuItem.getStockQuantity() < orderItem.getQuantity()) {
                    throw new InsufficientStockException(
                            menuItem.getName() + " has only " + menuItem.getStockQuantity() + " left. Reduce its quantity before sending.");
                }
            }

            for (OrderItem orderItem : order.getItems()) {
                state.getMenuItemOrThrow(orderItem.getMenuItemId())
                        .adjustStock(-orderItem.getQuantity());
            }
            order.confirm();
            addAudit(
                    state,
                    sessionToken,
                    LocalDateTime.now(clock),
                    "ORDER_CONFIRMED",
                    "Confirmed order " + order.getId() + ".");
            return null;
        });
        orderEvents.publish(orderId, OrderStatus.CONFIRMED);
    }

    /**
     * Retry kapag mali ang naipadala: ibabalik sa "Taking order" ang order na nasa kitchen pa
     * pero hindi pa sinisimulang lutuin. Ibinabalik din ang stock na nabawas noong ipinadala.
     */
    public void recallOrder(String sessionToken, String orderId) {
        repository.transact(state -> {
            AuthorizationService.require(state, sessionToken, Permission.CONFIRM_ORDERS);
            Order order = state.getOrderOrThrow(orderId);
            if (order.getStatus() != OrderStatus.CONFIRMED) {
                throw new InvalidTransitionException(order.getStatus() == OrderStatus.DRAFT
                        ? "This order has not been sent to the kitchen yet."
                        : "The kitchen has already started this order, so it can no longer be taken back.");
            }
            for (OrderItem orderItem : order.getItems()) {
                state.getMenuItemOrThrow(orderItem.getMenuItemId())
                        .adjustStock(orderItem.getQuantity());
            }
            order.recall();
            addAudit(
                    state,
                    sessionToken,
                    LocalDateTime.now(clock),
                    "ORDER_RECALLED",
                    "Took order " + order.getId() + " back from the kitchen to fix it.");
            return null;
        });
        orderEvents.publish(orderId, OrderStatus.DRAFT);
    }

    /** Kinakansela ang order bago ang preparation at vino-void ang active dining session nito. */
    public CancellationResult cancelOrder(String sessionToken, String orderId) {
        CancellationResult result = repository.transact(state -> {
            Order order = state.getOrderOrThrow(orderId);
            boolean stockRestored;
            if (order.getStatus() == OrderStatus.DRAFT) {
                AuthorizationService.require(
                        state, sessionToken, Permission.MANAGE_DRAFT_ORDERS);
                stockRestored = false;
            } else if (order.getStatus() == OrderStatus.CONFIRMED) {
                AuthorizationService.require(
                        state, sessionToken, Permission.CANCEL_CONFIRMED_ORDERS);
                stockRestored = true;
            } else {
                throw invalidCancellation(order);
            }

            DiningSession session = state.getSessionOrThrow(order.getSessionId());
            RestaurantTable table = state.getTableOrThrow(session.getTableId());
            Reservation reservation = session.getReservationId()
                    .map(state::getReservationOrThrow)
                    .orElse(null);

            // CANCELLED orders were rejected above, kaya hindi mauulit ang stock restore.
            if (stockRestored) {
                for (OrderItem orderItem : order.getItems()) {
                    state.getMenuItemOrThrow(orderItem.getMenuItemId())
                            .adjustStock(orderItem.getQuantity());
                }
            }

            LocalDateTime now = LocalDateTime.now(clock);
            order.cancel();
            session.voidSession(now);
            if (reservation != null
                    && reservation.getStatus() == ReservationStatus.CHECKED_IN) {
                reservation.cancel();
            }
            if (!table.isAvailable()) {
                table.release();
            }
            addAudit(
                    state,
                    sessionToken,
                    now,
                    "ORDER_CANCELLED",
                    "Cancelled order " + order.getId()
                            + " and voided dining session " + session.getId() + ".");

            return new CancellationResult(
                    order.getId(), session.getId(), table.getId(), stockRestored);
        });
        orderEvents.publish(orderId, OrderStatus.CANCELLED);
        return result;
    }

    private void requireDraft(Order order, String action) {
        // Bawal na mag-edit pag confirmed; na-deduct na ang stock sa service workflow.
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new InvalidTransitionException(
                    "Cannot " + action + " order while status is "
                            + order.getStatus() + ".");
        }
    }

    private InvalidTransitionException invalidCancellation(Order order) {
        return new InvalidTransitionException(
                "Cannot cancel order while status is " + order.getStatus() + ".");
    }

    private void addAudit(
            AppState state,
            String sessionToken,
            LocalDateTime timestamp,
            String action,
            String detail) {
        state.addAuditLog(new AuditLog(
                idGenerator.nextId(), timestamp, AuthorizationService.employeeId(state, sessionToken), action, detail));
    }
}
