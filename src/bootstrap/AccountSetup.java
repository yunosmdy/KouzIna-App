package bootstrap;
import domain.*;
import persistence.AppState;
import java.util.*;

/** One-time conversion; never recreates default accounts on startup. */
public final class AccountSetup {
    private AccountSetup() { }
    public static boolean migrate(AppState state) {
        if (state.getAccountVersion() == 2) return false;
        if (state.getAccountVersion() != 0) throw new IllegalStateException("Unsupported account data version.");
        Set<String> usernames = new HashSet<>();
        String recovery = null;
        boolean personalManager = false;
        for (Employee employee : state.getEmployees()) {
            if (!usernames.add(employee.getUsername().trim().toLowerCase(Locale.ROOT)))
                throw new IllegalStateException("Conflicting usernames in old data. Restore the backup and resolve the collision.");
            if (employee instanceof SharedLogin shared && shared.isForManagers()
                    && employee.getAccountStatus() == domain.enums.AccountStatus.ACTIVE) recovery = employee.getId();
            employee.normalizeLegacy();
            if (employee instanceof Manager && employee.isActive()) personalManager = true;
        }
        state.finishAccountMigration(personalManager ? null : recovery);
        return true;
    }
}
