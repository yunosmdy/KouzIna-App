package bootstrap;

import domain.Employee;
import domain.Manager;
import domain.SharedLogin;
import domain.Staff;
import domain.Waiter;
import persistence.AppState;
import security.PasswordHasher;

import java.security.SecureRandom;
import java.util.Objects;

/**
 * Default logins, profiles, and waiters.
 *
 * How signing in works:
 *   Employee portal -> username "employee", password "employee123" -> pick a Staff profile
 *   Manager portal  -> username "manager",  password "manager123"  -> pick a Manager profile
 *
 * The username is only the shared login. The name shown in the app comes from the profile
 * the user picks, so "manager" no longer shows up as "Morgan Manager".
 *
 * ensureDefaults() only adds what is missing, so it is safe to run on every start.
 * Older save files get the new profiles and waiters without losing any records.
 */
public final class AccountSetup {
    public static final String EMPLOYEE_LOGIN_ID = "login-employee";
    public static final String MANAGER_LOGIN_ID = "login-manager";
    public static final String EMPLOYEE_USERNAME = "employee";
    public static final String EMPLOYEE_PASSWORD = "employee123";
    public static final String MANAGER_USERNAME = "manager";
    public static final String MANAGER_PASSWORD = "manager123";

    /** Employee portal profiles (id, name). Edit names here. */
    static final String[][] STAFF_PROFILES = {
            {"staff-1", "Jael Castillo"},
            {"staff-2", "Lui Vence"},
            {"staff-3", "Gian Liit"},
            {"staff-4", "Vera Malinao"}};

    /** Manager portal profiles (id, name). employee-manager keeps its old ID for saved data. */
    static final String[][] MANAGER_PROFILES = {
            {SampleDataFactory.MANAGER_ID, "Jessie James"},
            {"manager-2", "Jerald Anderson"}};

    /** Waiters that can be assigned to tables (id, name). They do not sign in. */
    static final String[][] WAITERS = {
            {"waiter-2", "Jessie James"},
            {"waiter-3", "Kurt Pangan"},
            {"waiter-4", "Jerald Anderson"},
            {"waiter-5", "Lebron James"},
            {"waiter-6", "Reign Magtaca"}};

    private static final SecureRandom RANDOM = new SecureRandom();

    private AccountSetup() {
    }

    /** Adds missing default accounts. Returns true when something was added. */
    public static boolean ensureDefaults(AppState state, PasswordHasher hasher) {
        Objects.requireNonNull(state, "State is required.");
        Objects.requireNonNull(hasher, "Password hasher is required.");
        boolean changed = false;
        changed |= addIfMissing(state, new SharedLoginFactory(hasher, false));
        changed |= addIfMissing(state, new SharedLoginFactory(hasher, true));
        for (String[] profile : STAFF_PROFILES) {
            changed |= addIfMissing(state, new Staff(profile[0], profile[0], profile[1], noLogin()));
        }
        for (String[] profile : MANAGER_PROFILES) {
            changed |= addIfMissing(state, new Manager(profile[0], profile[0], profile[1], noLogin()));
        }
        for (String[] waiter : WAITERS) {
            changed |= addIfMissing(state, new Waiter(waiter[0], waiter[0], waiter[1], noLogin()));
        }
        return changed;
    }

    /**
     * Credential for profiles and waiters that never type a password.
     * It is a long random password nobody knows, so these records cannot sign in directly.
     */
    public static PasswordHasher.Credential noLogin() {
        char[] secret = new char[32];
        for (int i = 0; i < secret.length; i++) {
            secret[i] = (char) ('!' + RANDOM.nextInt(90));
        }
        PasswordHasher.Credential credential = new PasswordHasher(1_000).hash(secret);
        java.util.Arrays.fill(secret, '\0');
        return credential;
    }

    private static boolean addIfMissing(AppState state, Employee employee) {
        boolean idTaken = state.getEmployees().stream().anyMatch(e -> e.getId().equals(employee.getId()));
        if (idTaken || state.findEmployeeByUsername(employee.getUsername()).isPresent()) {
            return false;
        }
        state.addEmployee(employee);
        return true;
    }

    private static boolean addIfMissing(AppState state, SharedLoginFactory factory) {
        String id = factory.managers ? MANAGER_LOGIN_ID : EMPLOYEE_LOGIN_ID;
        String username = factory.managers ? MANAGER_USERNAME : EMPLOYEE_USERNAME;
        boolean idTaken = state.getEmployees().stream().anyMatch(e -> e.getId().equals(id));
        if (idTaken || state.findEmployeeByUsername(username).isPresent()) {
            return false;
        }
        state.addEmployee(factory.create());
        return true;
    }

    /** Hashing is slow on purpose, so shared logins are only hashed when they are really added. */
    private record SharedLoginFactory(PasswordHasher hasher, boolean managers) {
        SharedLogin create() {
            String password = managers ? MANAGER_PASSWORD : EMPLOYEE_PASSWORD;
            return new SharedLogin(
                    managers ? MANAGER_LOGIN_ID : EMPLOYEE_LOGIN_ID,
                    managers ? MANAGER_USERNAME : EMPLOYEE_USERNAME,
                    managers ? "Manager login" : "Employee login",
                    hasher.hash(password.toCharArray()),
                    managers);
        }
    }
}
