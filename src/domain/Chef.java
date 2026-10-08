package domain;

import domain.enums.Permission;
import security.PasswordHasher;

import java.io.Serial;
import java.util.Set;

/** Employee na nag mmanager sa kitchen queue at preparation status. */
public final class Chef extends Employee {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final Set<Permission> PERMISSIONS = Set.of(
            Permission.VIEW_KITCHEN_QUEUE,
            Permission.UPDATE_KITCHEN_STATUS
    );

    public Chef(String id, String username, String name,
                PasswordHasher.Credential credential) {
        super(id, username, name, credential);
    }

    @Override
    protected Set<Permission> permissions() {
        return PERMISSIONS;
    }

    @Override
    public String getRoleName() {
        return "Chef";
    }
}
