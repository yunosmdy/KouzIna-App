package service;

import bootstrap.AccountSetup;
import domain.AuditLog;
import domain.Employee;
import domain.Manager;
import domain.SharedLogin;
import domain.Staff;
import domain.Waiter;
import domain.enums.Permission;
import exception.AuthorizationException;
import exception.ConflictException;
import exception.ValidationException;
import persistence.AppState;
import persistence.AppStateRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Profiles and staff accounts.
 * - Profile screen: list, create, and remove profiles for the shared employee/manager login.
 * - Manager Tools > Staff: add waiters or profiles, deactivate or reactivate people.
 *
 * Profiles and waiters do not have their own passwords. Their username is the same as their ID.
 */
public final class StaffService {
    /** Kinds of people a manager can add from Manager Tools. */
    public static final List<String> KINDS = List.of("Staff", "Waiter", "Manager");

    private static final int MAX_NAME_LENGTH = 40;

    private final AppStateRepository repository;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public StaffService(AppStateRepository repository, IdGenerator idGenerator, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "Repository is required.");
        this.idGenerator = Objects.requireNonNull(idGenerator, "ID generator is required.");
        this.clock = Objects.requireNonNull(clock, "Clock is required.");
    }

    /** Active profiles the shared login can open, in the order they were added. */
    public List<Employee> profilesFor(String loginId) {
        AppState state = repository.snapshot();
        SharedLogin login = requireLogin(state, loginId);
        return state.getEmployees().stream().filter(login::opens).toList();
    }

    /** "Add profile" tile. Employee login adds Staff, manager login adds a Manager. */
    public String createProfile(String loginId, String fullName) {
        return repository.transact(state -> {
            SharedLogin login = requireLogin(state, loginId);
            String name = cleanName(fullName);
            boolean duplicate = state.getEmployees().stream()
                    .anyMatch(e -> login.opens(e) && e.getName().equalsIgnoreCase(name));
            if (duplicate) {
                throw new ConflictException("There is already a profile named " + name + ".");
            }
            String kind = login.isForManagers() ? "Manager" : "Staff";
            Employee profile = newEmployee(state, kind, name);
            state.addEmployee(profile);
            audit(state, loginId, "PROFILE_CREATED", kind + " profile created: " + name + ".");
            return profile.getId();
        });
    }

    /** "Manage profiles" > Remove. The record is kept (deactivated) so history still shows the name. */
    public void removeProfile(String loginId, String profileId) {
        repository.transact(state -> {
            SharedLogin login = requireLogin(state, loginId);
            Employee profile = state.getEmployeeOrThrow(profileId);
            if (!login.opens(profile)) {
                throw new AuthorizationException("This profile belongs to a different login.");
            }
            profile.deactivate();
            audit(state, loginId, "PROFILE_REMOVED", "Profile removed: " + profile.getName() + ".");
            return null;
        });
    }

    /** Manager Tools > Staff > Add person. kind is one of KINDS. */
    public String addEmployee(String actorId, String fullName, String kind) {
        return repository.transact(state -> {
            AuthorizationService.require(state, actorId, Permission.MANAGE_STAFF);
            if (!KINDS.contains(kind)) {
                throw new ValidationException("Choose Staff, Waiter, or Manager.");
            }
            String name = cleanName(fullName);
            boolean duplicate = state.getEmployees().stream().anyMatch(e -> e.isActive()
                    && e.getRoleName().equals(kind) && e.getName().equalsIgnoreCase(name));
            if (duplicate) {
                throw new ConflictException("There is already an active " + kind.toLowerCase()
                        + " named " + name + ".");
            }
            Employee employee = newEmployee(state, kind, name);
            state.addEmployee(employee);
            audit(state, actorId, "STAFF_ADDED", kind + " added: " + name + ".");
            return employee.getId();
        });
    }

    /** Manager Tools > Staff > Activate / Deactivate. */
    public void setActive(String actorId, String employeeId, boolean active) {
        repository.transact(state -> {
            AuthorizationService.require(state, actorId, Permission.MANAGE_STAFF);
            Employee employee = state.getEmployeeOrThrow(employeeId);
            if (employee instanceof SharedLogin) {
                throw new ValidationException("Shared logins cannot be turned off here.");
            }
            if (!active && employee.getId().equals(actorId)) {
                throw new ValidationException("You cannot deactivate the profile you are using.");
            }
            if (active) {
                employee.activate();
            } else {
                employee.deactivate();
            }
            audit(state, actorId, active ? "STAFF_ACTIVATED" : "STAFF_DEACTIVATED",
                    employee.getName() + " (" + employee.getRoleName() + ").");
            return null;
        });
    }

    /** Short explanation of how this person uses the app (shown in Manager Tools > Staff). */
    public static String signInDescription(Employee employee) {
        if (employee instanceof SharedLogin login) {
            return "Shared login: " + login.getUsername();
        }
        boolean profileOnly = employee.getUsername().equals(employee.getId());
        if (!profileOnly) {
            return "Own login: " + employee.getUsername();
        }
        if (employee instanceof Staff) {
            return "Profile on the employee login";
        }
        if (employee instanceof Manager) {
            return "Profile on the manager login";
        }
        return "Assigned to tables (no sign-in)";
    }

    private Employee newEmployee(AppState state, String kind, String name) {
        String prefix = switch (kind) {
            case "Manager" -> "manager-";
            case "Waiter" -> "waiter-";
            default -> "staff-";
        };
        String id = nextId(state, prefix);
        return switch (kind) {
            case "Manager" -> new Manager(id, id, name, AccountSetup.noLogin());
            case "Waiter" -> new Waiter(id, id, name, AccountSetup.noLogin());
            default -> new Staff(id, id, name, AccountSetup.noLogin());
        };
    }

    /** staff-1, staff-2, ... continues after the highest number already saved. */
    private static String nextId(AppState state, String prefix) {
        int highest = 0;
        for (Employee employee : state.getEmployees()) {
            String id = employee.getId();
            if (id.startsWith(prefix) && id.substring(prefix.length()).matches("\\d{1,9}")) {
                highest = Math.max(highest, Integer.parseInt(id.substring(prefix.length())));
            }
        }
        return prefix + (highest + 1);
    }

    private static String cleanName(String fullName) {
        String name = fullName == null ? "" : fullName.trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) {
            throw new ValidationException("Enter a name.");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new ValidationException("Names can be up to " + MAX_NAME_LENGTH + " characters.");
        }
        return name;
    }

    private static SharedLogin requireLogin(AppState state, String loginId) {
        Employee employee;
        try {
            employee = state.getEmployeeOrThrow(loginId);
        } catch (RuntimeException error) {
            throw new AuthorizationException("Sign in again to manage profiles.");
        }
        if (!(employee instanceof SharedLogin login) || !login.isActive()) {
            throw new AuthorizationException("Sign in again to manage profiles.");
        }
        return login;
    }

    private void audit(AppState state, String actorId, String action, String detail) {
        state.addAuditLog(new AuditLog(
                idGenerator.nextId(), LocalDateTime.now(clock), actorId, action, detail));
    }
}
