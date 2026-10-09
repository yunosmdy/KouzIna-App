package domain;

import domain.enums.Permission;
import security.PasswordHasher;

import java.io.Serial;
import java.util.Set;

/** Retained only to deserialize and verify the retired Manager credential during one-time migration. */
public final class SharedLogin extends Employee {
    @Serial
    private static final long serialVersionUID = 1L;

    private final boolean forManagers;

    public SharedLogin(String id, String username, String name,
                       PasswordHasher.Credential credential, boolean forManagers) {
        super(id, username, name, credential);
        this.forManagers = forManagers;
    }

    /** Identifies which retired shared credential was the Manager credential. */
    public boolean isForManagers() {
        return forManagers;
    }


    @Override
    protected Set<Permission> permissions() {
        return Set.of();
    }

    @Override
    public String getRoleName() {
        return forManagers ? "Manager login" : "Employee login";
    }
}
