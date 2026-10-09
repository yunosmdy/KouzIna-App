package service;
import domain.Employee;
import domain.enums.AccountStatus;
/** Safe account administration view: never includes password material. */
public record AccountRow(String id,String fullName,String username,String role,AccountStatus status,
                         boolean needsSetup,boolean needsPasswordChange) {
    static AccountRow of(Employee e) {
        return new AccountRow(e.getId(),e.getName(),e.getUsername(),e.getRoleName(),e.getAccountStatus(),e.needsAccountSetup(),e.isPasswordChangeRequired());
    }
}
