package service;
import domain.*;
import domain.enums.*;
import exception.*;
import persistence.*;
import security.PasswordHasher;
import java.time.LocalDateTime;
import java.util.*;

public final class AuthenticationService {
    private final AppStateRepository repository;
    private final PasswordHasher hasher;
    public AuthenticationService(AppStateRepository repository, PasswordHasher hasher) {
        this.repository = Objects.requireNonNull(repository); this.hasher = Objects.requireNonNull(hasher);
    }
    public AuthenticatedUser login(String username, char[] password) {
        String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || password == null) throw failure();
        Employee verified = repository.transact(state -> {
            Employee employee = state.findEmployeeByUsername(normalized).orElseThrow(AuthenticationService::failure);
            if (!employee.verifyPassword(password, hasher) || employee instanceof SharedLogin || employee instanceof Staff) throw failure();
            if (employee.needsAccountSetup()) throw new AuthorizationException("Ask your manager to set up your individual account.");
            if (employee.getAccountStatus() != AccountStatus.ACTIVE)
                throw new AuthorizationException("Account is " + employee.getAccountStatus().toString().toLowerCase(Locale.ROOT)
                        + (employee.getAccountStatus() == AccountStatus.PENDING ? ". Wait for manager approval." : ". Contact your manager."));
            if (employee.isPasswordChangeRequired())
                throw new AuthorizationException("Account setup is incomplete. Contact your Manager.");
            if (!employee.isActive()) throw failure();
            audit(state, employee.getId(), "LOGIN", "Individual account signed in.");
            return employee;
        });
        return AuthorizationService.issue(repository.snapshot(), verified);
    }
    public String register(String fullName, String username, char[] password, char[] confirmation) {
        String name = AccountValidation.name(fullName), normalized = AccountValidation.username(username);
        AccountValidation.passwords(password, confirmation);
        PasswordHasher.Credential credential = hasher.hash(password);
        return repository.transact(state -> {
            String id = UUID.randomUUID().toString();
            state.addEmployee(new PendingEmployee(id, normalized, name, credential));
            // The applicant is the subject of registration, never the approving authority.
            audit(state, id, "REGISTRATION_SUBMITTED", "Applicant requested access; no role or approval granted.");
            return id;
        });
    }
    public AuthenticatedUser validateSession(AuthenticatedUser user) {
        if (user == null) throw failure();
        Employee employee = AuthorizationService.sessionEmployee(repository.snapshot(), user.sessionToken());
        if (!employee.getId().equals(user.employeeId())) throw failure();
        return user;
    }
    public void logout(AuthenticatedUser user) {
        if (user == null) return;
        try {
            repository.transact(state -> {
                Employee e = AuthorizationService.sessionEmployee(state, user.sessionToken());
                audit(state, e.getId(), "LOGOUT", "Individual account signed out."); return null;
            });
        } finally { AuthorizationService.revoke(user.sessionToken()); }
    }
    private static AuthorizationException failure() { return new AuthorizationException("Invalid username or password."); }
    private static void audit(AppState state, String id, String action, String detail) {
        state.addAuditLog(new AuditLog(UUID.randomUUID().toString(), LocalDateTime.now(), id, action, detail));
    }
}
