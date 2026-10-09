package domain;
import domain.enums.*;
import security.PasswordHasher;
import java.io.Serial;
import java.util.Set;
/** A registration has an identity and credentials, but no operational role. */
public final class PendingEmployee extends Employee {
    @Serial private static final long serialVersionUID = 1L;
    public PendingEmployee(String id, String username, String name, PasswordHasher.Credential credential) {
        super(id, username, name, credential); setAccountStatus(AccountStatus.PENDING);
    }
    @Override protected Set<Permission> permissions() { return Set.of(); }
    @Override public String getRoleName() { return "Unassigned"; }
}
