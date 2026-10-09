package domain;

import domain.enums.*;
import security.PasswordHasher;
import java.io.*;
import java.util.*;

/** Stable identity; role subclasses supply permissions. Old serialized fields keep their names. */
public abstract class Employee implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private final String id;
    private String username;
    private String name;
    private PasswordHasher.Credential credential;
    private boolean active;
    // Null/default values are deliberately normalized by AccountSetup when reading old saves.
    private AccountStatus accountStatus;
    private boolean needsAccountSetup;
    private boolean passwordChangeRequired;
    private long accountRevision;

    protected Employee(String id, String username, String name, PasswordHasher.Credential credential) {
        this.id = text(id); this.username = text(username).toLowerCase(Locale.ROOT);
        this.name = text(name); this.credential = Objects.requireNonNull(credential);
        active = true; accountStatus = AccountStatus.ACTIVE;
    }
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getName() { return name; }
    public void rename(String fullName) { name = text(fullName); }
    public AccountStatus getAccountStatus() {
        return accountStatus == null ? (active ? AccountStatus.ACTIVE : AccountStatus.INACTIVE) : accountStatus;
    }
    public boolean needsAccountSetup() { return needsAccountSetup; }
    public boolean isPasswordChangeRequired() { return passwordChangeRequired; }
    public long getAccountRevision() { return accountRevision; }
    public boolean isActive() {
        return getAccountStatus() == AccountStatus.ACTIVE && !needsAccountSetup && !passwordChangeRequired
                && !(this instanceof SharedLogin) && !(this instanceof Staff) && !(this instanceof PendingEmployee);
    }
    public void setAccountStatus(AccountStatus status) {
        accountStatus = Objects.requireNonNull(status); active = status == AccountStatus.ACTIVE; accountRevision++;
    }
    public void activate() { setAccountStatus(AccountStatus.ACTIVE); }
    public void deactivate() { setAccountStatus(AccountStatus.INACTIVE); }
    public final boolean verifyPassword(char[] password, PasswordHasher hasher) {
        return Objects.requireNonNull(hasher).verify(password, credential);
    }
    public final boolean allows(Permission permission) {
        return isActive() && permission != null && permissions().contains(permission);
    }
    public void configureCredentials(String username, PasswordHasher.Credential credential, boolean temporary) {
        this.username = text(username).toLowerCase(Locale.ROOT);
        this.credential = Objects.requireNonNull(credential);
        needsAccountSetup = false; passwordChangeRequired = temporary; accountRevision++;
    }
    /** Retire a shared login name without changing its historical identity. */
    public void retireLogin(String archivedUsername) {
        username = text(archivedUsername).toLowerCase(Locale.ROOT);
        setAccountStatus(AccountStatus.INACTIVE);
    }
    /** The simple account flow accepts the password already assigned to this account. */
    public void acceptCurrentPassword() { passwordChangeRequired = false; accountRevision++; }
    public void finishPasswordChange(PasswordHasher.Credential credential) {
        this.credential = Objects.requireNonNull(credential); passwordChangeRequired = false; accountRevision++;
    }
    public void normalizeLegacy() {
        boolean wasActive = active;
        needsAccountSetup = username.equals(id) || this instanceof SharedLogin || this instanceof Staff;
        username = username.trim().toLowerCase(Locale.ROOT);
        accountStatus = needsAccountSetup ? (wasActive ? AccountStatus.PENDING : AccountStatus.INACTIVE)
                : (wasActive ? AccountStatus.ACTIVE : AccountStatus.INACTIVE);
        if (this instanceof SharedLogin) accountStatus = AccountStatus.INACTIVE;
        active = accountStatus == AccountStatus.ACTIVE; accountRevision++;
    }
    /** A role change replaces the subtype but preserves identity, credentials and lifecycle. */
    public Employee withRole(Role role) {
        Employee result = switch (Objects.requireNonNull(role)) {
            case WAITER -> new Waiter(id, username, name, credential);
            case CHEF -> new Chef(id, username, name, credential);
            case CASHIER -> new Cashier(id, username, name, credential);
            case MANAGER -> new Manager(id, username, name, credential);
        };
        result.accountStatus = getAccountStatus(); result.active = active;
        result.needsAccountSetup = needsAccountSetup; result.passwordChangeRequired = passwordChangeRequired;
        result.accountRevision = accountRevision + 1;
        return result;
    }
    protected abstract Set<Permission> permissions();
    public abstract String getRoleName();
    private static String text(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Identity fields are required.");
        return value.trim();
    }
}
