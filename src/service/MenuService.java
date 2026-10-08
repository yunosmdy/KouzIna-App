package service;

import domain.BeverageItem;
import domain.AuditLog;
import domain.FoodItem;
import domain.MenuItem;
import domain.enums.Permission;
import persistence.AppStateRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/** Dito yung menu at stock changes, check permission muna bago update. */
public final class MenuService {
    private final AppStateRepository repository;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public MenuService(AppStateRepository repository, IdGenerator idGenerator) {
        this(repository, idGenerator, Clock.systemDefaultZone());
    }

    public MenuService(
            AppStateRepository repository, IdGenerator idGenerator, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "Repository is required.");
        this.idGenerator = Objects.requireNonNull(idGenerator, "ID generator is required.");
        this.clock = Objects.requireNonNull(clock, "Clock is required.");
    }

    public String createFood(
            String actorId, String name, BigDecimal unitPrice, int stockQuantity,
            String portionDescription) {
        return repository.transact(state -> {
            AuthorizationService.require(state, actorId, Permission.MANAGE_MENU);
            FoodItem item = new FoodItem(
                    idGenerator.nextId(), name, unitPrice, stockQuantity, portionDescription);
            state.addMenuItem(item);
            return item.getId();
        });
    }

    public String createBeverage(
            String actorId, String name, BigDecimal unitPrice, int stockQuantity,
            int volumeMl, boolean servedCold) {
        return repository.transact(state -> {
            AuthorizationService.require(state, actorId, Permission.MANAGE_MENU);
            BeverageItem item = new BeverageItem(
                    idGenerator.nextId(), name, unitPrice, stockQuantity, volumeMl, servedCold);
            state.addMenuItem(item);
            return item.getId();
        });
    }

    public void updateDetails(
            String actorId, String menuItemId, String name, BigDecimal unitPrice) {
        repository.transact(state -> {
            AuthorizationService.require(state, actorId, Permission.MANAGE_MENU);
            state.getMenuItemOrThrow(menuItemId).updateDetails(name, unitPrice);
            return null;
        });
    }

    public void activate(String actorId, String menuItemId) {
        changeActiveStatus(actorId, menuItemId, true);
    }

    public void deactivate(String actorId, String menuItemId) {
        changeActiveStatus(actorId, menuItemId, false);
    }

    public void adjustStock(String actorId, String menuItemId, int quantityChange) {
        repository.transact(state -> {
            AuthorizationService.require(state, actorId, Permission.MANAGE_STOCK);
            MenuItem item = state.getMenuItemOrThrow(menuItemId);
            item.adjustStock(quantityChange);
            state.addAuditLog(new AuditLog(
                    idGenerator.nextId(),
                    LocalDateTime.now(clock),
                    actorId,
                    "STOCK_ADJUSTED",
                    "Adjusted stock for " + item.getId() + " by " + quantityChange + "."));
            return null;
        });
    }

    private void changeActiveStatus(String actorId, String menuItemId, boolean active) {
        repository.transact(state -> {
            AuthorizationService.require(state, actorId, Permission.MANAGE_MENU);
            MenuItem item = state.getMenuItemOrThrow(menuItemId);
            if (active) {
                item.activate();
            } else {
                item.deactivate();
            }
            return null;
        });
    }
}
