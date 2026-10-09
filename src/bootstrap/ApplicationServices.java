package bootstrap;

import persistence.AppStateRepository;
import persistence.FileAppStateRepository;
import security.PasswordHasher;
import service.AuditService;
import service.AuthenticationService;
import service.BillingService;
import service.CustomerService;
import service.IdGenerator;
import service.KitchenService;
import service.MenuService;
import service.OrderService;
import service.OrderEventPublisher;
import service.ReportService;
import service.ReservationService;
import service.SeatingService;
import service.StaffService;
import service.UuidIdGenerator;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;

/** Dito sama sama yung repository at services na gagamitin ng screens. */
public final class ApplicationServices {
    private final AppStateRepository repository;
    private final AuthenticationService authenticationService;
    private final CustomerService customerService;
    private final MenuService menuService;
    private final ReservationService reservationService;
    private final SeatingService seatingService;
    private final OrderService orderService;
    private final OrderEventPublisher orderEvents;
    private final KitchenService kitchenService;
    private final BillingService billingService;
    private final ReportService reportService;
    private final AuditService auditService;
    private final StaffService staffService;

    public static ApplicationServices forDataFile(Path dataFile) {
        PasswordHasher hasher = new PasswordHasher();
        AppStateRepository repository = new FileAppStateRepository(
                dataFile, SampleDataFactory::create);
        if (repository.snapshot().getAccountVersion() != 2) {
            // Copy the exact old bytes before the first conversion. Never overwrite this backup.
            Path backup = dataFile.resolveSibling(dataFile.getFileName() + ".pre-accounts-v2.bak");
            try {
                if (!java.nio.file.Files.exists(backup)) java.nio.file.Files.copy(dataFile, backup);
            } catch (java.io.IOException error) {
                throw new IllegalStateException("Cannot back up old data; migration was not started.", error);
            }
            repository.transact(state -> AccountSetup.migrate(state));
        }
        if (!repository.snapshot().hasPresetAccounts()) {
            Path backup = dataFile.resolveSibling(dataFile.getFileName() + ".before-simple-login.bak");
            try {
                if (!java.nio.file.Files.exists(backup)) java.nio.file.Files.copy(dataFile, backup);
            } catch (java.io.IOException error) {
                throw new IllegalStateException("Cannot back up saved data before account update.", error);
            }
            repository.transact(state -> { PresetAccounts.install(state,hasher); return null; });
        }
        return new ApplicationServices(
                repository, hasher, new UuidIdGenerator(), Clock.systemDefaultZone());
    }

    public ApplicationServices(
            AppStateRepository repository,
            PasswordHasher passwordHasher,
            IdGenerator idGenerator,
            Clock clock) {
        this.repository = Objects.requireNonNull(repository, "Repository is required.");
        PasswordHasher hasher = Objects.requireNonNull(
                passwordHasher, "Password hasher is required.");
        IdGenerator ids = Objects.requireNonNull(idGenerator, "ID generator is required.");
        Clock applicationClock = Objects.requireNonNull(clock, "Clock is required.");

        authenticationService = new AuthenticationService(repository, hasher);
        customerService = new CustomerService(repository, ids);
        menuService = new MenuService(repository, ids, applicationClock);
        reservationService = new ReservationService(repository, ids, applicationClock);
        seatingService = new SeatingService(repository, ids, applicationClock);
        orderEvents = new OrderEventPublisher();
        orderService = new OrderService(repository, ids, applicationClock, orderEvents);
        kitchenService = new KitchenService(repository, ids, applicationClock, orderEvents);
        billingService = new BillingService(repository, ids, applicationClock);
        reportService = new ReportService(repository);
        auditService = new AuditService(repository);
        staffService = new StaffService(repository, ids, applicationClock);
    }

    public AppStateRepository repository() {
        return repository;
    }

    public AuthenticationService authentication() {
        return authenticationService;
    }

    public CustomerService customers() {
        return customerService;
    }

    public MenuService menu() {
        return menuService;
    }

    public ReservationService reservations() {
        return reservationService;
    }

    public SeatingService seating() {
        return seatingService;
    }

    public OrderService orders() {
        return orderService;
    }

    public OrderEventPublisher orderEvents() {
        return orderEvents;
    }

    public KitchenService kitchen() {
        return kitchenService;
    }

    public BillingService billing() {
        return billingService;
    }

    public ReportService reports() {
        return reportService;
    }

    public AuditService audit() {
        return auditService;
    }

    public StaffService staff() {
        return staffService;
    }
}
