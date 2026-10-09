package service;
import domain.*;
import domain.enums.*;
import exception.*;
import persistence.*;
import java.time.*;
import java.util.*;

/** Manager-authorized lifecycle; role changes retain the permanent identity. */
public final class StaffService {
    private final AppStateRepository repository;
    private final IdGenerator ids;
    private final Clock clock;
    public StaffService(AppStateRepository repository, IdGenerator ids, Clock clock) {
        this.repository=repository; this.ids=ids; this.clock=clock;
    }
    public List<AccountRow> accounts(String token) {
        AppState state=repository.snapshot(); AuthorizationService.require(state,token,Permission.MANAGE_STAFF);
        return state.getEmployees().stream().filter(e -> !(e instanceof SharedLogin) && !e.needsAccountSetup()).map(AccountRow::of).toList();
    }
    public void approve(String token,String id,Role role) {
        repository.transact(state -> {
            Employee manager=AuthorizationService.require(state,token,Permission.MANAGE_STAFF);
            Employee e=state.getEmployeeOrThrow(id);
            if(e instanceof SharedLogin || e.needsAccountSetup() || e.getAccountStatus()!=AccountStatus.PENDING)
                throw new ValidationException("Choose a pending account request.");
            Employee replacement=e.withRole(role); replacement.activate(); state.replaceEmployee(replacement);
            audit(state,manager.getId(),"ACCOUNT_APPROVED",id,"Assigned role: "+role); return null;
        });
    }
    public void reject(String token,String id) {
        repository.transact(state -> {
            Employee manager=AuthorizationService.require(state,token,Permission.MANAGE_STAFF);
            Employee e=state.getEmployeeOrThrow(id);
            if(e instanceof SharedLogin || e.getAccountStatus()!=AccountStatus.PENDING)
                throw new ValidationException("Only pending requests can be rejected.");
            e.setAccountStatus(AccountStatus.REJECTED); audit(state,manager.getId(),"ACCOUNT_REJECTED",id,"Registration rejected."); return null;
        });
    }
    public void changeRole(String token,String id,Role role) {
        repository.transact(state -> {
            Employee manager=AuthorizationService.require(state,token,Permission.MANAGE_STAFF);
            Employee e=state.getEmployeeOrThrow(id); requireApproved(e); protectManager(state,manager,e,role==Role.MANAGER);
            state.replaceEmployee(e.withRole(role)); audit(state,manager.getId(),"ROLE_CHANGED",id,"New role: "+role); return null;
        });
    }
    public void setActive(String token,String id,boolean active) {
        repository.transact(state -> {
            Employee manager=AuthorizationService.require(state,token,Permission.MANAGE_STAFF);
            Employee e=state.getEmployeeOrThrow(id); requireApproved(e);
            if(!active) protectManager(state,manager,e,false);
            e.setAccountStatus(active?AccountStatus.ACTIVE:AccountStatus.INACTIVE);
            audit(state,manager.getId(),active?"ACCOUNT_REACTIVATED":"ACCOUNT_DEACTIVATED",id,"Access updated."); return null;
        });
    }
    /** Permanently deletes an account (approved, pending or rejected). Returns the deleted person's name. */
    public String delete(String token,String id) {
        return repository.transact(state -> {
            Employee manager=AuthorizationService.require(state,token,Permission.MANAGE_STAFF);
            Employee e=state.getEmployeeOrThrow(id);
            if(e instanceof SharedLogin || e.needsAccountSetup())
                throw new ValidationException("Choose an employee account to delete.");
            if(e.getId().equals(manager.getId()))
                throw new ValidationException("You cannot delete your own account.");
            protectManager(state,manager,e,false);
            boolean serving=state.getSessions().stream().anyMatch(s -> s.isOpen() && id.equals(s.getWaiterId()));
            if(serving)
                throw new ValidationException(e.getName()+" is still assigned to an open table. "
                        +"Finish billing that table first, or deactivate the account instead.");
            String name=e.getName();
            state.removeEmployee(id);
            audit(state,manager.getId(),"ACCOUNT_DELETED",id,"Deleted "+name+" ("+e.getUsername()+", "+e.getRoleName()+").");
            return name;
        });
    }
    private static void requireApproved(Employee e) {
        if(e instanceof PendingEmployee || e instanceof Staff || e instanceof SharedLogin || e.needsAccountSetup()
                || (e.getAccountStatus()!=AccountStatus.ACTIVE && e.getAccountStatus()!=AccountStatus.INACTIVE))
            throw new ValidationException("Choose an approved account.");
    }
    private static void protectManager(AppState state,Employee actor,Employee target,boolean remainsManager) {
        if(target instanceof Manager && !remainsManager) {
            if(target.getId().equals(actor.getId())) throw new ValidationException("You cannot remove your own Manager access.");
            if(target.getAccountStatus()==AccountStatus.ACTIVE && state.getEmployees().stream()
                    .filter(e -> e instanceof Manager && e.isActive()).count()<=1)
                throw new ValidationException("At least one active Manager must remain.");
        }
    }
    private void audit(AppState state,String actor,String action,String target,String detail) {
        state.addAuditLog(new AuditLog(ids.nextId(),LocalDateTime.now(clock),actor,action,"Account "+target+": "+detail));
    }
}
