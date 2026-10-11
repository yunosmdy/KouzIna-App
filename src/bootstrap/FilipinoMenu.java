package bootstrap;

import domain.BeverageItem;
import domain.FoodItem;
import domain.MenuItem;
import persistence.AppState;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * Classic Filipino dishes added next to the starter menu.
 * Dish list from "Food Frenzy: Classic Filipino Dishes" (brenontheroad.com).
 * Runs at every start but only adds a dish when no menu item has that name yet,
 * so restarting never duplicates them and a dish a Manager hid stays hidden.
 * Pictures: photos/menu/[dish name].jpg (see photos/menu/CREDITS.txt).
 * Street foods (Chicharon, Balut, Turon, Isaw) were taken off this list; see REMOVED.
 */
public final class FilipinoMenu {
    /** name, portion, price, starting stock */
    private record Dish(String name, String portion, String price, int stock) { }

    private static final List<Dish> DISHES = List.of(
            new Dish("Fresh Lumpia", "2 pieces", "120.00", 20),
            new Dish("Chicken Sotanghon", "Regular bowl", "160.00", 20),
            new Dish("Beef Mechado", "Rice meal", "260.00", 18),
            new Dish("Paksiyo Baboy Bisaya", "Sharing claypot", "320.00", 12),
            new Dish("Leche Flan", "One slice", "95.00", 20),
            new Dish("Lechon", "Quarter kilo", "380.00", 10),
            new Dish("Bibingka", "One piece", "85.00", 20),
            new Dish("Kare Kare", "Sharing bowl", "420.00", 12),
            new Dish("Lomi", "Regular bowl", "150.00", 20),
            new Dish("Chami", "Regular plate", "150.00", 18),
            new Dish("Kilawin na Tanigue", "Sharing plate", "280.00", 12),
            new Dish("Pancit Bihon Guisado", "Sharing plate", "220.00", 18),
            new Dish("Crispy Pata", "Sharing plate", "650.00", 8),
            new Dish("Sinigang", "Sharing bowl", "340.00", 15),
            new Dish("Kaldereta", "Rice meal", "270.00", 16),
            new Dish("Adobo", "Rice meal", "210.00", 25),
            new Dish("Ginataang Papaya", "Regular bowl", "180.00", 14),
            new Dish("Bacolod Chicken", "Quarter chicken", "230.00", 18),
            new Dish("Bulalo", "Sharing bowl", "450.00", 10),
            new Dish("Buko Pie", "One slice", "110.00", 16),
            new Dish("Ilocos Empanada", "One piece", "95.00", 18),
            new Dish("Longaniza", "Rice meal", "180.00", 20),
            new Dish("Bangus", "Rice meal", "210.00", 16));

    /**
     * Street foods added by an earlier version. On start they are removed from the menu,
     * or only hidden if an order already used them (so old orders and bills still show them).
     */
    private static final List<String> REMOVED = List.of("Chicharon", "Balut", "Turon", "Isaw");

    private FilipinoMenu() {
    }

    /** True when at least one dish (or the Tablea drink) is not on the menu yet. */
    public static boolean anyMissing(AppState state) {
        return DISHES.stream().anyMatch(dish -> !has(state, dish.name())) || !has(state, TABLEA)
                || REMOVED.stream().anyMatch(name -> needsCleanup(state, name));
    }

    /** Adds every dish that is not on the menu yet, and clears out the removed street foods. */
    public static void addMissing(AppState state) {
        for (String name : REMOVED) {
            if (needsCleanup(state, name)) {
                if (usedInAnOrder(state, id(name))) {
                    state.getMenuItemOrThrow(id(name)).deactivate();
                } else {
                    state.removeMenuItem(id(name));
                }
            }
        }
        for (Dish dish : DISHES) {
            if (!has(state, dish.name())) {
                state.addMenuItem(new FoodItem(id(dish.name()), dish.name(),
                        new BigDecimal(dish.price()), dish.stock(), dish.portion()));
            }
        }
        if (!has(state, TABLEA)) {
            // Local hot chocolate made from tablea (ground cocoa tablets): a drink, served hot
            state.addMenuItem(new BeverageItem(id(TABLEA), TABLEA, new BigDecimal("110.00"), 20, 300, false));
        }
    }

    private static final String TABLEA = "Tablea Tsokolate";

    /** Only the copy this class added (by its ID) is touched; a Manager's own item with that name stays. */
    private static boolean needsCleanup(AppState state, String name) {
        MenuItem item = state.getMenuItems().stream().filter(i -> i.getId().equals(id(name))).findFirst().orElse(null);
        return item != null && (item.isActive() || !usedInAnOrder(state, item.getId()));
    }

    private static boolean usedInAnOrder(AppState state, String menuItemId) {
        return state.getOrders().stream().flatMap(order -> order.getItems().stream())
                .anyMatch(line -> line.getMenuItemId().equals(menuItemId));
    }

    private static boolean has(AppState state, String name) {
        for (MenuItem item : state.getMenuItems()) {
            if (item.getName().equalsIgnoreCase(name) || item.getId().equals(id(name))) {
                return true;
            }
        }
        return false;
    }

    /** Stable ID like "menu-ph-kare-kare". */
    private static String id(String name) {
        return "menu-ph-" + name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    }
}
