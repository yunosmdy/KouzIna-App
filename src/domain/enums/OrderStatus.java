package domain.enums;

/** Mga status ng order; hiwalay ang payment ah, di ito order status. */
public enum OrderStatus {
    DRAFT,
    CONFIRMED,
    PREPARING,
    READY,
    SERVED,
    CANCELLED
}
