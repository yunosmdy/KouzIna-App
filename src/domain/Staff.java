package domain;

import domain.enums.Permission;
import security.PasswordHasher;

import java.io.Serial;
import java.util.Set;

/**
 * All-around crew member (employee profile). Can do the whole guest workflow:
 * seat guests, take orders, update the kitchen, and take payment.
 * Manager-only work (menu, stock, reports, staff, cancelling sent orders) is not included.
 */
public final class Staff extends Employee {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final Set<Permission> PERMISSIONS = Set.of(
            Permission.MANAGE_CUSTOMERS,
            Permission.MANAGE_RESERVATIONS,
            Permission.MANAGE_WAITLIST,
            Permission.CHECK_IN_GUESTS,
            Permission.MANAGE_DRAFT_ORDERS,
            Permission.CONFIRM_ORDERS,
            Permission.MARK_ORDERS_SERVED,
            Permission.VIEW_KITCHEN_QUEUE,
            Permission.UPDATE_KITCHEN_STATUS,
            Permission.ISSUE_BILLS,
            Permission.ACCEPT_PAYMENTS
    );

    public Staff(String id, String username, String name,
                 PasswordHasher.Credential credential) {
        super(id, username, name, credential);
    }

    @Override
    protected Set<Permission> permissions() {
        return PERMISSIONS;
    }

    @Override
    public String getRoleName() {
        return "Staff";
    }
}
