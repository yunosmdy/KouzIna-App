package service;

import domain.MenuItem;

import java.util.Objects;

/** Menu stock details para sa report, di pwede i-edit dito. */
public final class StockReportRow {
    private final String menuItemId;
    private final String name;
    private final String category;
    private final boolean active;
    private final int stockQuantity;

    public StockReportRow(MenuItem menuItem) {
        this(
                Objects.requireNonNull(menuItem, "Menu item is required.").getId(),
                menuItem.getName(),
                menuItem.getCategoryName(),
                menuItem.isActive(),
                menuItem.getStockQuantity());
    }

    public StockReportRow(
            String menuItemId, String name, String category,
            boolean active, int stockQuantity) {
        this.menuItemId = requireText(menuItemId, "Menu item ID");
        this.name = requireText(name, "Menu item name");
        this.category = requireText(category, "Menu item category");
        this.active = active;
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative.");
        }
        this.stockQuantity = stockQuantity;
    }

    public String getMenuItemId() {
        return menuItemId;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public boolean isActive() {
        return active;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }
}
