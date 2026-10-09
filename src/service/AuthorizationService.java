package service;
import domain.*;
import domain.enums.Permission;
import exception.AuthorizationException;
import persistence.AppState;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Services require a live login token, never just a predictable employee ID. */
public final class AuthorizationService {
    private record Session(String securityId, String employeeId, long revision) { }
    private static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>();
    private AuthorizationService() { }
    static AuthenticatedUser issue(AppState state, Employee employee) {
        String token = UUID.randomUUID().toString();
        SESSIONS.put(token, new Session(state.getSecurityId(), employee.getId(), employee.getAccountRevision()));
        return new AuthenticatedUser(employee.getId(), employee.getUsername(), employee.getName(), employee.getRoleName(), token);
    }
    static void revoke(String token) { if (token != null) SESSIONS.remove(token); }
    public static Employee sessionEmployee(AppState state, String token) {
        Session session = token == null ? null : SESSIONS.get(token);
        if (session == null || !Objects.equals(session.securityId(), state.getSecurityId()))
            throw new AuthorizationException("Your session has ended. Please sign in again.");
        Employee employee = state.getEmployees().stream()
                .filter(e -> e.getId().equals(session.employeeId())).findFirst().orElse(null);
        if (employee == null) {
            revoke(token);
            throw new AuthorizationException("Your account was removed. Please contact a Manager.");
        }
        if (!employee.isActive() || employee.getAccountRevision() != session.revision()) {
            revoke(token);
            throw new AuthorizationException("Your account access changed. Please sign in again.");
        }
        return employee;
    }
    public static Employee require(AppState state, String token, Permission permission) {
        Employee employee = sessionEmployee(state, token);
        if (!employee.allows(Objects.requireNonNull(permission)))
            throw new AuthorizationException(employee.getRoleName() + " is not allowed to perform this action.");
        return employee;
    }
    public static String employeeId(AppState state, String token) { return sessionEmployee(state, token).getId(); }
    public static Waiter requireActiveWaiter(AppState state, String waiterId) {
        Employee employee = state.getEmployeeOrThrow(waiterId);
        if (!(employee instanceof Waiter waiter) || !waiter.isActive())
            throw new AuthorizationException("An active waiter must be assigned.");
        return waiter;
    }
}
