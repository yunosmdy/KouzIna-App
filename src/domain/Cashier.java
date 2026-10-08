package domain;

import domain.enums.Permission;
import security.PasswordHasher;

import java.io.Serial;
import java.util.Set;

/** Employee na humahawak sa billing at simulated payments. */
public final class Cashier extends Employee {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final Set<Permission> PERMISSIONS = Set.of(
            Permission.ISSUE_BILLS,
            Permission.ACCEPT_PAYMENTS
    );

    public Cashier(String id, String username, String name,
                   PasswordHasher.Credential credential) {
        super(id, username, name, credential);
    }

    @Override
    protected Set<Permission> permissions() {
        return PERMISSIONS;
    }

    @Override
    public String getRoleName() {
        return "Cashier";
    }
}
