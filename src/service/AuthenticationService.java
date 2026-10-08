package service;

import domain.Employee;
import domain.SharedLogin;
import exception.AuthorizationException;
import persistence.AppState;
import persistence.AppStateRepository;
import security.PasswordHasher;

import java.util.Objects;

/** Naglo-login ng active employees nang hindi inilalabas ang saved password credentials. */
public final class AuthenticationService {
    private static final String LOGIN_FAILURE = "Invalid username or password.";

    private final AppStateRepository repository;
    private final PasswordHasher passwordHasher;

    public AuthenticationService(
            AppStateRepository repository, PasswordHasher passwordHasher) {
        this.repository = Objects.requireNonNull(repository, "Repository is required.");
        this.passwordHasher = Objects.requireNonNull(
                passwordHasher, "Password hasher is required.");
    }

    /** Login ng employee; caller ang maglilinis ng password char array pagkatapos. */
    public AuthenticatedUser login(String username, char[] password) {
        if (username == null || username.isBlank() || password == null) {
            throw new AuthorizationException(LOGIN_FAILURE);
        }

        AppState state = repository.snapshot();
        Employee employee = state.findEmployeeByUsername(username.trim())
                .orElseThrow(() -> new AuthorizationException(LOGIN_FAILURE));
        if (!employee.verifyPassword(password, passwordHasher)) {
            throw new AuthorizationException(LOGIN_FAILURE);
        }

        return toUser(employee);
    }

    /** True when the user signed in with a shared login and still has to pick a profile. */
    public boolean isSharedLogin(AuthenticatedUser user) {
        if (user == null) {
            return false;
        }
        return repository.snapshot().getEmployees().stream()
                .anyMatch(e -> e.getId().equals(user.employeeId()) && e instanceof SharedLogin);
    }

    /** Profile picked after a shared login (like choosing a profile on Netflix). */
    public AuthenticatedUser chooseProfile(AuthenticatedUser login, String profileId) {
        if (login == null || profileId == null) {
            throw new AuthorizationException("Choose a profile.");
        }
        AppState state = repository.snapshot();
        Employee account = state.getEmployeeOrThrow(login.employeeId());
        if (!(account instanceof SharedLogin sharedLogin) || !sharedLogin.isActive()) {
            throw new AuthorizationException("Sign in again to choose a profile.");
        }
        Employee profile = state.getEmployeeOrThrow(profileId);
        if (!sharedLogin.opens(profile)) {
            throw new AuthorizationException("This profile cannot be used with this login.");
        }
        return toUser(profile);
    }

    private static AuthenticatedUser toUser(Employee employee) {
        return new AuthenticatedUser(
                employee.getId(),
                employee.getUsername(),
                employee.getName(),
                employee.getRoleName());
    }
}
