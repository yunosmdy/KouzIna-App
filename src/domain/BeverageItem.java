package domain;

import java.io.Serial;
import java.math.BigDecimal;


public final class BeverageItem extends MenuItem {
    @Serial
    private static final long serialVersionUID = 1L;

    private int volumeMl;
    private boolean servedCold;

    public BeverageItem(String id, String name, BigDecimal unitPrice, int stockQuantity,
                        int volumeMl, boolean servedCold) {
        super(id, name, unitPrice, stockQuantity);
        setVolumeMl(volumeMl);
        this.servedCold = servedCold;
    }

    @Override
    public String getCategoryName() {
        return "Beverage";
    }

    public int getVolumeMl() {
        return volumeMl;
    }

    public boolean isServedCold() {
        return servedCold;
    }

    public void setVolumeMl(int volumeMl) {
        if (volumeMl <= 0) {
            throw new IllegalArgumentException("Beverage volume must be positive.");
        }
        this.volumeMl = volumeMl;
    }

    public void setServedCold(boolean servedCold) {
        this.servedCold = servedCold;
    }
}
