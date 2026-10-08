package domain;

import domain.enums.Permission;
import security.PasswordHasher;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

/** Base class ng restaurant employees na puwedeng mag-login. */
public abstract class Employee implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String username;
    private final String name;
    private final PasswordHasher.Credential credential;
    private boolean active;

    protected Employee(String id, String username, String name,
                       PasswordHasher.Credential credential) {
        this.id = requireNonBlank(id, "Employee ID");
        this.username = requireNonBlank(username, "Username");
        this.name = requireNonBlank(name, "Employee name");
        this.credential = Objects.requireNonNull(credential, "Credential is required.");
        this.active = true;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }

    public final boolean verifyPassword(char[] password, PasswordHasher passwordHasher) {
        Objects.requireNonNull(passwordHasher, "Password hasher is required.");
        return active && passwordHasher.verify(password, credential);
    }

    public final boolean allows(Permission permission) {
        return permission != null && permissions().contains(permission);
    }

    protected abstract Set<Permission> permissions();

    public abstract String getRoleName();

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }
}
