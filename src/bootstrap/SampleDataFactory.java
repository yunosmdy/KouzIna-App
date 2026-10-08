package bootstrap;

import domain.BeverageItem;
import domain.Cashier;
import domain.Chef;
import domain.Customer;
import domain.FoodItem;
import domain.RestaurantTable;
import domain.Waiter;
import persistence.AppState;
import security.PasswordHasher;

import java.math.BigDecimal;
import java.util.Objects;

/** Dito galing yung starter records pag bagong setup pa yung app. */
public final class SampleDataFactory {
    public static final String WAITER_ID = "employee-waiter";
    public static final String CHEF_ID = "employee-chef";
    public static final String CASHIER_ID = "employee-cashier";
    public static final String MANAGER_ID = "employee-manager";

    private SampleDataFactory() {
    }

    public static AppState create() {
        return create(new PasswordHasher());
    }

    public static AppState create(PasswordHasher passwordHasher) {
        PasswordHasher hasher = Objects.requireNonNull(
                passwordHasher, "Password hasher is required.");
        AppState state = new AppState();
        addEmployees(state, hasher);
        addTables(state);
        addMenuItems(state);
        addCustomers(state);
        return state;
    }

    private static void addEmployees(AppState state, PasswordHasher hasher) {
        state.addEmployee(new Waiter(
                WAITER_ID, "waiter", "Wally Waiter", hash(hasher, "waiter123")));
        state.addEmployee(new Chef(
                CHEF_ID, "chef", "Casey Chef", hash(hasher, "chef123")));
        state.addEmployee(new Cashier(
                CASHIER_ID, "cashier", "Carmen Cashier", hash(hasher, "cashier123")));
        // Shared logins (employee / manager), profiles, managers, and extra waiters
        AccountSetup.ensureDefaults(state, hasher);
    }

    private static void addTables(AppState state) {
        int[] capacities = {2, 2, 4, 4, 4, 6, 6, 8, 8, 10};
        for (int index = 0; index < capacities.length; index++) {
            int number = index + 1;
            state.addTable(new RestaurantTable(
                    "table-" + number, number, capacities[index]));
        }
    }

    private static void addMenuItems(AppState state) {
        addFood(state, 1, "Classic Burger", "Single serving", "185.00", 30);
        addFood(state, 2, "Chicken Alfredo", "Single serving", "245.00", 24);
        addFood(state, 3, "Beef Tapa", "Rice meal", "220.00", 20);
        addFood(state, 4, "Grilled Salmon", "Single serving", "365.00", 14);
        addFood(state, 5, "Margherita Pizza", "10-inch pizza", "280.00", 18);
        addFood(state, 6, "Caesar Salad", "Sharing bowl", "195.00", 20);
        addFood(state, 7, "Pork Sisig", "Sharing plate", "250.00", 22);
        addFood(state, 8, "Garlic Rice", "One cup", "55.00", 50);
        addFood(state, 9, "Chocolate Cake", "One slice", "120.00", 16);
        addFood(state, 10, "Halo-Halo", "Regular bowl", "145.00", 18);
        addFood(state, 11, "French Fries", "Sharing basket", "130.00", 28);
        addFood(state, 12, "Mushroom Soup", "Regular bowl", "110.00", 20);

        addBeverage(state, 13, "Iced Tea", "75.00", 40, 450, true);
        addBeverage(state, 14, "Lemonade", "85.00", 36, 450, true);
        addBeverage(state, 15, "Cola", "65.00", 48, 330, true);
        addBeverage(state, 16, "Orange Juice", "95.00", 24, 350, true);
        addBeverage(state, 17, "Brewed Coffee", "90.00", 30, 250, false);
        addBeverage(state, 18, "Hot Chocolate", "105.00", 24, 300, false);
        addBeverage(state, 19, "Bottled Water", "45.00", 60, 500, true);
        addBeverage(state, 20, "Mango Shake", "125.00", 20, 450, true);
    }

    private static void addCustomers(AppState state) {
        state.addCustomer(new Customer(
                "customer-1", "Sairus", "Andurei", "0917-555-0101"));
        state.addCustomer(new Customer(
                "customer-2", "Yunos", "Mads", "0917-555-0102"));
        state.addCustomer(new Customer(
                "customer-3", "Alex", "Lewis", "0917-555-0103"));
    }

    private static void addFood(
            AppState state,
            int number,
            String name,
            String portion,
            String price,
            int stock) {
        state.addMenuItem(new FoodItem(
                "menu-" + number, name, new BigDecimal(price), stock, portion));
    }

    private static void addBeverage(
            AppState state,
            int number,
            String name,
            String price,
            int stock,
            int volumeMl,
            boolean servedCold) {
        state.addMenuItem(new BeverageItem(
                "menu-" + number,
                name,
                new BigDecimal(price),
                stock,
                volumeMl,
                servedCold));
    }

    private static PasswordHasher.Credential hash(PasswordHasher hasher, String password) {
        char[] characters = password.toCharArray();
        try {
            return hasher.hash(characters);
        } finally {
            java.util.Arrays.fill(characters, '\0');
        }
    }
}
