package domain.enums;

/** Mga operation na chine-check ng services kung may permission ang employee. */
public enum Permission {
    MANAGE_CUSTOMERS,
    MANAGE_RESERVATIONS,
    MANAGE_WAITLIST,
    CHECK_IN_GUESTS,
    MANAGE_DRAFT_ORDERS,
    CONFIRM_ORDERS,
    VIEW_KITCHEN_QUEUE,
    UPDATE_KITCHEN_STATUS,
    MARK_ORDERS_SERVED,
    ISSUE_BILLS,
    ACCEPT_PAYMENTS,
    MANAGE_MENU,
    MANAGE_STOCK,
    VIEW_REPORTS,
    VIEW_AUDIT_LOG,
    CANCEL_CONFIRMED_ORDERS,
    MANAGE_STAFF
}
