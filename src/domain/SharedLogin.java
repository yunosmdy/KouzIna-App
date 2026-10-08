package domain;

import domain.enums.Permission;
import security.PasswordHasher;

import java.io.Serial;
import java.util.Set;

/**
 * Shared login for the restaurant computer, e.g. username "employee" or "manager".
 * It cannot do any work by itself: after signing in, the user picks a profile
 * (like Netflix) and every action is recorded under that profile's name.
 */
public final class SharedLogin extends Employee {
    @Serial
    private static final long serialVersionUID = 1L;

    private final boolean forManagers;

    public SharedLogin(String id, String username, String name,
                       PasswordHasher.Credential credential, boolean forManagers) {
        super(id, username, name, credential);
        this.forManagers = forManagers;
    }

    /** true = shows Manager profiles, false = shows Staff profiles. */
    public boolean isForManagers() {
        return forManagers;
    }

    /** Whether the given employee is one of the profiles this login can open. */
    public boolean opens(Employee profile) {
        return profile.isActive()
                && (forManagers ? profile instanceof Manager : profile instanceof Staff);
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
