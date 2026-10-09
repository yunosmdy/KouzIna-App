package domain;

import domain.enums.Permission;
import security.PasswordHasher;

import java.io.Serial;
import java.util.Set;

/** Retired broad Staff subtype; retained only for old serialized identities. It grants no permissions. */
public final class Staff extends Employee {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final Set<Permission> PERMISSIONS = Set.of();

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
