package service;

import domain.Employee;
import domain.Waiter;
import domain.enums.Permission;
import exception.AuthorizationException;
import exception.DomainException;
import persistence.AppState;

/** Iisang lugar para sa permission checks ng services na nagbabago ng state. */
public final class AuthorizationService {
    private AuthorizationService() {
    }

    public static Employee require(
            AppState state, String actorId, Permission permission) {
        if (state == null) {
            throw new IllegalArgumentException("Application state is required.");
        }
        if (permission == null) {
            throw new IllegalArgumentException("Permission is required.");
        }

        Employee actor = resolveEmployee(state, actorId, "Employee is not authorized.");
        if (!actor.isActive() || !actor.allows(permission)) {
            throw new AuthorizationException(
                    actor.getRoleName() + " is not allowed to perform this action.");
        }
        return actor;
    }

    public static Waiter requireActiveWaiter(AppState state, String waiterId) {
        Employee employee = resolveEmployee(
                state, waiterId, "Assigned waiter is not available.");
        if (!(employee instanceof Waiter waiter) || !waiter.isActive()) {
            throw new AuthorizationException("An active waiter must be assigned.");
        }
        return waiter;
    }

    private static Employee resolveEmployee(
            AppState state, String employeeId, String failureMessage) {
        if (state == null) {
            throw new IllegalArgumentException("Application state is required.");
        }
        try {
            return state.getEmployeeOrThrow(employeeId);
        } catch (DomainException exception) {
            throw new AuthorizationException(failureMessage);
        }
    }
}
