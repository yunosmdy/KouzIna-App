package domain;

import java.io.Serial;
import java.math.BigDecimal;


public final class FoodItem extends MenuItem {
    @Serial
    private static final long serialVersionUID = 1L;

    private String portionDescription;

    public FoodItem(String id, String name, BigDecimal unitPrice, int stockQuantity,
                    String portionDescription) {
        super(id, name, unitPrice, stockQuantity);
        setPortionDescription(portionDescription);
    }

    @Override
    public String getCategoryName() {
        return "Food";
    }

    public String getPortionDescription() {
        return portionDescription;
    }

    public void setPortionDescription(String portionDescription) {
        if (portionDescription == null || portionDescription.isBlank()) {
            throw new IllegalArgumentException("Portion description is required.");
        }
        this.portionDescription = portionDescription.trim();
    }
}
