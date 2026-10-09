package bootstrap;

import domain.*;
import domain.enums.Role;
import persistence.AppState;
import security.PasswordHasher;
import java.util.UUID;

/** One-time starter accounts for the classroom demo. Never resets accounts on restart. */
public final class PresetAccounts {
    private PresetAccounts() { }
    public static void install(AppState state, PasswordHasher hasher) {
        if (state.hasPresetAccounts()) return;
        add(state,hasher,"manager","manager123",Role.MANAGER,"Restaurant Manager");
        add(state,hasher,"waiter","waiter123",Role.WAITER,"Restaurant Waiter");
        add(state,hasher,"chef","chef12345",Role.CHEF,"Noning Ry");
        add(state,hasher,"cashier","cashier123",Role.CASHIER,"Iruma");
        for (Employee employee : state.getEmployees())
            if (employee.isPasswordChangeRequired()) employee.acceptCurrentPassword();
        state.completePresetAccounts();
    }
    private static void add(AppState state,PasswordHasher hasher,String username,String password,Role role,String name) {
        Employee existing=state.findEmployeeByUsername(username).orElse(null);
        if (existing instanceof SharedLogin) {
            String archived;
            do { archived="retired-"+UUID.randomUUID(); } while(state.findEmployeeByUsername(archived).isPresent());
            existing.retireLogin(archived);
            state.replaceEmployee(existing);
        } else if (existing != null) {
            // Upgrade only the known seven-character Chef demo password, preserving status.
            // Personal passwords, assigned roles and deactivations remain unchanged.
            if (existing instanceof Chef && username.equals("chef")
                    && existing.verifyPassword("chef123".toCharArray(),hasher))
                existing.configureCredentials(username,hasher.hash(password.toCharArray()),false);
            return;
        }
        // Reuse the old default person's ID when it has no individual credentials yet.
        Employee profile=state.getEmployees().stream()
                .filter(e -> e.getId().equals("employee-"+username) && e.needsAccountSetup())
                .findFirst().orElse(null);
        var credential=hasher.hash(password.toCharArray());
        if(profile != null) {
            Employee replacement=profile.withRole(role);
            replacement.configureCredentials(username,credential,false);
            replacement.activate();state.replaceEmployee(replacement);
        } else {
            Employee preset=new Manager(UUID.randomUUID().toString(),username,name,credential).withRole(role);
            state.addEmployee(preset);
        }
    }
}
