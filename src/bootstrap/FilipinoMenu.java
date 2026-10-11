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
            new Dish("Chicharon", "Snack bowl", "90.00", 25),
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
            new Dish("Balut", "One egg", "30.00", 30),
            new Dish("Bulalo", "Sharing bowl", "450.00", 10),
            new Dish("Buko Pie", "One slice", "110.00", 16),
            new Dish("Turon", "2 pieces", "70.00", 24),
            new Dish("Ilocos Empanada", "One piece", "95.00", 18),
            new Dish("Longaniza", "Rice meal", "180.00", 20),
            new Dish("Isaw", "3 sticks", "75.00", 24),
            new Dish("Bangus", "Rice meal", "210.00", 16));

    private FilipinoMenu() {
    }

    /** True when at least one dish (or the Tablea drink) is not on the menu yet. */
    public static boolean anyMissing(AppState state) {
        return DISHES.stream().anyMatch(dish -> !has(state, dish.name())) || !has(state, TABLEA);
    }

    /** Adds every dish that is not on the menu yet. */
    public static void addMissing(AppState state) {
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
