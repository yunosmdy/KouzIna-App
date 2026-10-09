package ui;
import bootstrap.*;
import domain.*;
import domain.enums.*;
import exception.*;
import persistence.*;
import security.PasswordHasher;
import service.*;
import java.nio.file.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;

/** Behavioral acceptance tests, including real bytes serialized by the pre-change application. */
public final class AccountChecks {
    static int checks;
    static char[] pw(String s) { return s.toCharArray(); }
    static void check(boolean condition,String message) { if(!condition) throw new AssertionError(message);checks++; }
    static void denied(Runnable action,String message) {
        try { action.run(); } catch(AuthorizationException expected) { checks++;return; }
        throw new AssertionError(message);
    }
    static void invalid(Runnable action,String message) {
        try { action.run(); } catch(DomainException expected) { checks++;return; }
        throw new AssertionError(message);
    }
    static ApplicationServices app(AppState state) {
        return new ApplicationServices(new InMemoryAppStateRepository(state),new PasswordHasher(1000),new UuidIdGenerator(),Clock.systemUTC());
    }
    static AuthenticatedUser login(ApplicationServices app,String username,String password) { return app.authentication().login(username,pw(password)); }
    static String register(ApplicationServices app,String name,String username) { return app.authentication().register(name,username,pw("personal123"),pw("personal123")); }
    public static void run() throws Exception {
        AppState seed=SampleDataFactory.create();PresetAccounts.install(seed,new PasswordHasher(1000));
        ApplicationServices fresh=app(seed);
        AuthenticatedUser preset=login(fresh,"manager","manager123");
        String managerId=register(fresh,"Initial Manager","first.manager");
        denied(() -> login(fresh,"first.manager","personal123"),"New Manager waits for approval just like employees");
        fresh.staff().approve(preset.sessionToken(),managerId,Role.MANAGER);
        AuthenticatedUser manager=login(fresh," FIRST.MANAGER ","personal123");String mt=manager.sessionToken();
        check(manager.employeeId().equals(managerId)&&manager.displayName().equals("Initial Manager"),"Manager has separate name and username");
        for(String input:new String[]{"", "ab", "bad space", "bad@name", "_bad", "x".repeat(31)})
            invalid(() -> register(fresh,"Name",input),"Invalid username: "+input);
        invalid(() -> register(fresh," ","blank.name"),"Blank full name rejected");
        invalid(() -> register(fresh,"x".repeat(81),"long.name"),"Overlong name rejected");
        invalid(() -> fresh.authentication().register("Name","mismatch",pw("password123"),pw("different123")),"Password confirmation required");
        invalid(() -> fresh.authentication().register("Name","shortpw",pw("short"),pw("short")),"Short password rejected");
        invalid(() -> fresh.authentication().register("Name","longpw",pw("x".repeat(129)),pw("x".repeat(129))),"Overlong password rejected");
        String waiterId=register(fresh,"Jael Castillo","Jael.C");
        String chefId=register(fresh,"Jael Castillo","jael.chef");
        String cashierId=register(fresh,"Cashier Name","cashier.name");
        Employee pending=fresh.repository().snapshot().getEmployeeOrThrow(waiterId);
        check(pending instanceof PendingEmployee&&pending.getAccountStatus()==AccountStatus.PENDING,"Registration is Pending and unassigned");
        check(pending.getUsername().equals("jael.c")&&pending.getName().equals("Jael Castillo"),"Normalized username differs from display name");
        for(Permission permission:Permission.values()) check(!pending.allows(permission),"Pending permission denied: "+permission);
        invalid(() -> register(fresh,"Other"," JAEL.C "),"Duplicate normalized username rejected");
        denied(() -> login(fresh,"jael.c","personal123"),"Pending login blocked");
        denied(() -> login(fresh,"jael.c","wrong"),"Wrong pending credentials blocked");
        denied(() -> fresh.staff().accounts(waiterId),"An employee ID is not a session token");
        denied(() -> fresh.staff().accounts(null),"Unauthenticated users cannot list accounts");
        fresh.staff().approve(mt,waiterId,Role.WAITER);fresh.staff().approve(mt,chefId,Role.CHEF);fresh.staff().approve(mt,cashierId,Role.CASHIER);
        AuthenticatedUser waiter=login(fresh,"jael.c","personal123"),chef=login(fresh,"jael.chef","personal123"),cashier=login(fresh,"cashier.name","personal123");
        String wt=waiter.sessionToken(),ct=chef.sessionToken(),cat=cashier.sessionToken();
        check(waiter.employeeId().equals(waiterId),"Approval retains ID");
        check(waiter.roleName().equals("Waiter")&&chef.roleName().equals("Chef")&&cashier.roleName().equals("Cashier"),"Roles resolved from account");
        for(String token:new String[]{wt,ct,cat}) {
            denied(() -> fresh.staff().accounts(token),"Non-Manager cannot list registrations");
            denied(() -> fresh.staff().approve(token,waiterId,Role.MANAGER),"Non-Manager cannot approve or promote");
            denied(() -> fresh.staff().setActive(token,waiterId,false),"Non-Manager cannot deactivate others");
            denied(() -> fresh.menu().adjustStock(token,"menu-1",1),"Non-Manager cannot adjust stock");
        }
        for(Permission permission:Permission.values()) {
            Employee w=fresh.repository().snapshot().getEmployeeOrThrow(waiterId),c=fresh.repository().snapshot().getEmployeeOrThrow(chefId),ca=fresh.repository().snapshot().getEmployeeOrThrow(cashierId);
            boolean wa=Set.of(Permission.MANAGE_CUSTOMERS,Permission.MANAGE_RESERVATIONS,Permission.MANAGE_WAITLIST,Permission.CHECK_IN_GUESTS,
                    Permission.MANAGE_DRAFT_ORDERS,Permission.CONFIRM_ORDERS,Permission.MARK_ORDERS_SERVED).contains(permission);
            boolean ch=Set.of(Permission.VIEW_KITCHEN_QUEUE,Permission.UPDATE_KITCHEN_STATUS).contains(permission);
            boolean cash=Set.of(Permission.ISSUE_BILLS,Permission.ACCEPT_PAYMENTS).contains(permission);
            check(w.allows(permission)==wa,"Waiter permission: "+permission);check(c.allows(permission)==ch,"Chef permission: "+permission);
            check(ca.allows(permission)==cash,"Cashier permission: "+permission);
            check(fresh.repository().snapshot().getEmployeeOrThrow(managerId).allows(permission),"Manager permission: "+permission);
        }
        SeatingResult visit=fresh.seating().registerWalkIn(wt,"customer-1",2,waiterId);
        fresh.orders().addItem(wt,visit.orderId(),"menu-1",1);fresh.orders().confirmOrder(wt,visit.orderId());
        denied(() -> fresh.kitchen().markPreparing(wt,visit.orderId()),"Waiter cannot prepare food");
        denied(() -> fresh.orders().addItem(ct,visit.orderId(),"menu-1",1),"Chef cannot edit orders");
        denied(() -> fresh.seating().registerWalkIn(cat,"customer-2",2,waiterId),"Cashier cannot seat guests");
        denied(() -> fresh.billing().issueBill(wt,visit.sessionId(),BigDecimal.ZERO,BigDecimal.ZERO),"Waiter cannot issue bills");
        fresh.kitchen().markPreparing(ct,visit.orderId());fresh.kitchen().markReady(ct,visit.orderId());
        fresh.kitchen().markServed(wt,visit.orderId());String bill=fresh.billing().issueBill(cat,visit.sessionId(),BigDecimal.ZERO,BigDecimal.ZERO);
        denied(() -> fresh.billing().acceptCash(ct,bill,new BigDecimal("200")),"Chef cannot accept payment");
        fresh.billing().acceptCash(cat,bill,new BigDecimal("200"));
        check(fresh.repository().snapshot().getTableOrThrow(visit.tableId()).isAvailable(),"Separate roles complete a restaurant visit");
        for(AuditLog log:fresh.audit().getAuditLog(mt)) {
            check(fresh.repository().snapshot().getEmployees().stream().anyMatch(e -> e.getId().equals(log.getEmployeeId())),"Audit stores permanent identity, never token: "+log.getAction()+" / "+log.getEmployeeId());
            check(!log.getDetail().contains("personal123")&&!log.getDetail().contains("personal123"),"Audit has no passwords");
        }
        String rejected=register(fresh,"Rejected User","rejected.user");fresh.staff().reject(mt,rejected);
        denied(() -> login(fresh,"rejected.user","personal123"),"Rejected login denied");
        invalid(() -> fresh.staff().setActive(mt,rejected,true),"Rejected request cannot be reactivated around approval");
        invalid(() -> register(fresh,"Other","REJECTED.USER"),"Rejected username reserved");
        invalid(() -> fresh.staff().approve(mt,waiterId,Role.MANAGER),"Approved account cannot be approved twice");
        fresh.staff().setActive(mt,waiterId,false);
        denied(() -> fresh.customers().createCustomer(wt,"Blocked","Action","0917-555-0101"),"Deactivation stops existing session");
        denied(() -> login(fresh,"jael.c","personal123"),"Inactive login denied");
        invalid(() -> register(fresh,"Other","JAEL.C"),"Inactive username reserved");
        fresh.staff().setActive(mt,waiterId,true);
        AuthenticatedUser again=login(fresh,"jael.c","personal123");
        check(again.employeeId().equals(waiterId),"Reactivation preserves identity");
        fresh.staff().changeRole(mt,waiterId,Role.CHEF);
        denied(() -> fresh.authentication().validateSession(again),"Role change invalidates stale session");
        AuthenticatedUser changed=login(fresh,"jael.c","personal123");check(changed.roleName().equals("Chef"),"Role change preserves personal credentials");
        fresh.authentication().logout(changed);
        denied(() -> fresh.kitchen().getQueue(changed.sessionToken()),"Logout revokes service access");
        invalid(() -> fresh.staff().setActive(mt,managerId,false),"Cannot deactivate own last Manager");
        invalid(() -> fresh.staff().changeRole(mt,managerId,Role.CASHIER),"Cannot demote own last Manager");
        String extra=register(fresh,"Second Manager","second.manager");fresh.staff().approve(mt,extra,Role.MANAGER);
        check(login(fresh,"second.manager","personal123").roleName().equals("Manager"),"Only Manager approval grants additional Manager access");
        Path persisted=Files.createTempDirectory("accounts-reload-").resolve("state.dat");
        String pendingId=register(fresh,"Persistent Pending","persistent.pending");
        new FileAppStateRepository(persisted,()->fresh.repository().snapshot());ApplicationServices reopen=ApplicationServices.forDataFile(persisted);
        check(reopen.repository().snapshot().getEmployeeOrThrow(pendingId).getAccountStatus()==AccountStatus.PENDING,"Pending registration survives restart");
        check(login(reopen,"second.manager","personal123").employeeId().equals(extra),"Personal Manager survives restart");
        // Changing an assigned waiter does not destroy an existing dining session.
        String historicalWaiter=register(fresh,"Historical Waiter","historical.waiter");fresh.staff().approve(mt,historicalWaiter,Role.WAITER);
        String historicalToken=login(fresh,"historical.waiter","personal123").sessionToken();
        SeatingResult oldVisit=fresh.seating().registerWalkIn(historicalToken,"customer-2",2,historicalWaiter);
        fresh.orders().addItem(historicalToken,oldVisit.orderId(),"menu-1",1);fresh.orders().confirmOrder(historicalToken,oldVisit.orderId());
        fresh.staff().changeRole(mt,historicalWaiter,Role.CHEF);
        denied(() -> fresh.seating().registerWalkIn(mt,"customer-3",2,historicalWaiter),"Former waiter cannot receive new assignments");
        fresh.kitchen().markPreparing(mt,oldVisit.orderId());fresh.kitchen().markReady(mt,oldVisit.orderId());fresh.kitchen().markServed(mt,oldVisit.orderId());
        String historyBill=fresh.billing().issueBill(mt,oldVisit.sessionId(),BigDecimal.ZERO,BigDecimal.ZERO);
        fresh.billing().acceptCash(mt,historyBill,new BigDecimal("200"));
        check(fresh.repository().snapshot().getSessionOrThrow(oldVisit.sessionId()).getWaiterId().equals(historicalWaiter),"Historical waiter ID retained after role change");
        check(!fresh.repository().snapshot().getSessionOrThrow(oldVisit.sessionId()).isOpen(),"Existing session can complete after waiter role change");
        migrationChecks();
        System.out.println("PASS: "+checks+" individual-account, role, session and migration checks");
    }
    static Path fixture(String name) {
        Path working=Path.of(System.getProperty("kouzina.testFixtures", "test/fixtures"),name);
        if(Files.exists(working))return working;
        throw new AssertionError("Run tests from the project root; missing fixture "+working);
    }
    static void migrationChecks() throws Exception {
        for(String fixture:new String[]{"individual.dat","shared.dat","renamed-shared.dat","inactive-shared.dat"}) {
            Path file=Files.createTempDirectory("simple-accounts-").resolve("state.dat");Files.copy(fixture(fixture),file);
            byte[] original=Files.readAllBytes(file);var before=new FileAppStateRepository(file,SampleDataFactory::create).snapshot();ApplicationServices updated=ApplicationServices.forDataFile(file);
            check(Arrays.equals(original,Files.readAllBytes(file.resolveSibling("state.dat.pre-accounts-v2.bak"))),"Original data backed up: "+fixture);
            check(updated.repository().snapshot().hasPresetAccounts(),"Preset update saved");
            check(updated.repository().snapshot().getRecoveryLoginId()==null,"No Manager migration needed");
            AuthenticatedUser owner=login(updated,"manager","manager123");
            check(owner.roleName().equals("Manager"),"Preset Manager can log in immediately");
            check(owner.employeeId().equals("employee-manager"),"Existing default Manager identity preserved");
            check(updated.staff().accounts(owner.sessionToken()).stream().noneMatch(AccountRow::needsSetup),"Account list only shows usable accounts and requests");
            if(!fixture.equals("individual.dat")) {
                denied(() -> login(updated,"employee","employee123"),"Shared employee login remains retired");
                check(updated.repository().snapshot().getPayments().size()==before.getPayments().size(),"Paid transaction retained");
                check(updated.repository().snapshot().getMenuItemOrThrow("menu-1").getStockQuantity()==before.getMenuItemOrThrow("menu-1").getStockQuantity(),"Stock retained");
            }
            check(login(updated,"chef","chef12345").roleName().equals("Chef"),"Old Chef demo password upgraded consistently");
            String applicant=register(updated,"New Manager","new.manager");
            denied(() -> login(updated,"new.manager","personal123"),"Manager applicant has no access before approval");
            updated.staff().approve(owner.sessionToken(),applicant,Role.MANAGER);
            check(login(updated,"new.manager","personal123").roleName().equals("Manager"),"Manager registration uses normal approval");
            byte[] once=Files.readAllBytes(file);int count=updated.repository().snapshot().getEmployees().size();
            ApplicationServices twice=ApplicationServices.forDataFile(file);
            check(twice.repository().snapshot().getEmployees().size()==count,"Restart does not duplicate accounts");
            check(Arrays.equals(once,Files.readAllBytes(file)),"Restart does not rewrite data");
        }
        Path freshFile=Files.createTempDirectory("preset-accounts-").resolve("state.dat");
        ApplicationServices presets=ApplicationServices.forDataFile(freshFile);
        for(String[] account:new String[][]{{"manager","manager123","Manager"},{"waiter","waiter123","Waiter"},{"chef","chef12345","Chef"},{"cashier","cashier123","Cashier"}})
            check(login(presets,account[0],account[1]).roleName().equals(account[2]),"Preset login: "+account[0]);
        AuthenticatedUser admin=login(presets,"manager","manager123");
        String chef=presets.repository().snapshot().findEmployeeByUsername("chef").orElseThrow().getId();
        presets.staff().setActive(admin.sessionToken(),chef,false);
        presets.repository().transact(state -> {
            state.findEmployeeByUsername("waiter").orElseThrow().configureCredentials("waiter",new PasswordHasher(1000).hash(pw("customPassword123")),false);return null;
        });
        ApplicationServices reopened=ApplicationServices.forDataFile(freshFile);
        denied(() -> login(reopened,"chef","chef12345"),"Restart must not reactivate a disabled preset");
        check(login(reopened,"waiter","customPassword123").roleName().equals("Waiter"),"Restart preserves changed preset password");
        denied(() -> login(reopened,"waiter","waiter123"),"Restart does not restore original preset password");
        Path collision=Files.createTempDirectory("collision-accounts-").resolve("state.dat");Files.copy(fixture("collision.dat"),collision);
        byte[] collisionBytes=Files.readAllBytes(collision);
        try { ApplicationServices.forDataFile(collision);throw new AssertionError("Username collision must fail migration"); }
        catch(IllegalStateException expected) { checks++; }
        check(Arrays.equals(collisionBytes,Files.readAllBytes(collision)),"Failed migration preserves exact old data without partial normalization");
        check(Arrays.equals(collisionBytes,Files.readAllBytes(collision.resolveSibling("state.dat.pre-accounts-v2.bak"))),"Failed migration still preserves exact backup");
        Path interrupted=Files.createTempDirectory("interrupted-accounts-").resolve("state.dat");Files.copy(fixture("individual.dat"),interrupted);
        byte[] interruptedBytes=Files.readAllBytes(interrupted);Files.createDirectory(interrupted.resolveSibling("state.dat.tmp"));
        try { ApplicationServices.forDataFile(interrupted);throw new AssertionError("Blocked temporary path must fail migration"); }
        catch(IllegalStateException expected) { checks++; }
        check(Arrays.equals(interruptedBytes,Files.readAllBytes(interrupted)),"Disk failure cannot save half-converted state");
        Files.deleteIfExists(interrupted.resolveSibling("state.dat.tmp"));
        check(ApplicationServices.forDataFile(interrupted).repository().snapshot().getAccountVersion()==2,"Migration retries successfully after disk problem is resolved");
        Path corrupt=Files.createTempDirectory("bad-account-data-").resolve("state.dat");byte[] bytes={1,2,3};Files.write(corrupt,bytes);
        try { ApplicationServices.forDataFile(corrupt);throw new AssertionError("Corruption must fail"); } catch(IllegalStateException expected) { checks++; }
        check(Arrays.equals(bytes,Files.readAllBytes(corrupt)),"Unreadable save is never reset");
    }
    public static void main(String[] args) throws Exception { run(); }
}
