package domain;

import domain.enums.Permission;
import security.PasswordHasher;

import java.io.Serial;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Employee na may permission sa lahat ng supported operations. */
public final class Manager extends Employee {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final Set<Permission> PERMISSIONS =
            Collections.unmodifiableSet(EnumSet.allOf(Permission.class));

    public Manager(String id, String username, String name,
                   PasswordHasher.Credential credential) {
        super(id, username, name, credential);
    }

    @Override
    protected Set<Permission> permissions() {
        return PERMISSIONS;
    }

    @Override
    public String getRoleName() {
        return "Manager";
    }
}
