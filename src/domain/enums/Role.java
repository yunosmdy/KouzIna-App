package domain.enums;
public enum Role {
    WAITER("Waiter"), CHEF("Chef"), CASHIER("Cashier"), MANAGER("Manager");
    private final String label;
    Role(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
