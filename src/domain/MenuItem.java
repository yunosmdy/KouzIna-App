package domain;

import util.Money;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/** Base class ng mga item na binebenta sa menu. */
public abstract class MenuItem implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private String name;
    private BigDecimal unitPrice;
    private boolean active;
    private int stockQuantity;

    protected MenuItem(String id, String name, BigDecimal unitPrice, int stockQuantity) {
        this.id = requireNonBlank(id, "Menu item ID");
        this.name = requireNonBlank(name, "Menu item name");
        this.unitPrice = requireNonNegativeMoney(unitPrice, "Unit price");
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative.");
        }
        this.stockQuantity = stockQuantity;
        this.active = true;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public boolean isActive() {
        return active;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public boolean isAvailable() {
        return active && stockQuantity > 0;
    }

    public boolean isAvailable(int requestedQuantity) {
        return requestedQuantity > 0 && active && stockQuantity >= requestedQuantity;
    }

    public void updateDetails(String name, BigDecimal unitPrice) {
        this.name = requireNonBlank(name, "Menu item name");
        this.unitPrice = requireNonNegativeMoney(unitPrice, "Unit price");
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }

    /** Add or minus sa stock, dapat di maging negative ang total. */
    public void adjustStock(int quantityChange) {
        long resultingStock = (long) stockQuantity + quantityChange;
        if (resultingStock < 0) {
            throw new IllegalArgumentException("Stock cannot be reduced below zero.");
        }
        if (resultingStock > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Stock quantity is too large.");
        }
        stockQuantity = (int) resultingStock;
    }

    public abstract String getCategoryName();

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    private static BigDecimal requireNonNegativeMoney(BigDecimal amount, String fieldName) {
        return Money.requireNonNegative(amount, fieldName);
    }
}
