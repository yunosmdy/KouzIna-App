package domain;

import domain.enums.Permission;
import security.PasswordHasher;

import java.io.Serial;
import java.util.Set;

/** Employee na humahawak sa guests at orders. */
public final class Waiter extends Employee {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final Set<Permission> PERMISSIONS = Set.of(
            Permission.MANAGE_CUSTOMERS,
            Permission.MANAGE_RESERVATIONS,
            Permission.MANAGE_WAITLIST,
            Permission.CHECK_IN_GUESTS,
            Permission.MANAGE_DRAFT_ORDERS,
            Permission.CONFIRM_ORDERS,
            Permission.MARK_ORDERS_SERVED
    );

    public Waiter(String id, String username, String name,
                  PasswordHasher.Credential credential) {
        super(id, username, name, credential);
    }

    @Override
    protected Set<Permission> permissions() {
        return PERMISSIONS;
    }

    @Override
    public String getRoleName() {
        return "Waiter";
    }
}
