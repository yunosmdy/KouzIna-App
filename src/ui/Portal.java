package ui;

/** Which side of the app the user picked on the welcome screen. */
public enum Portal {
    EMPLOYEE("Employee", "Seat guests, take orders, update the kitchen, and take payment."),
    MANAGER("Manager", "Everything, plus menu and stock, reports, staff, and the audit log.");

    private final String label;
    private final String description;

    Portal(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    /** Managers use the Manager portal; everyone else uses the Employee portal. */
    public boolean allowsRole(String roleName) {
        boolean manager = "Manager".equals(roleName) || "Manager login".equals(roleName);
        return this == MANAGER ? manager : !manager;
    }

    public String wrongAccountMessage() {
        return this == MANAGER
                ? "This is not a manager account. Go back and choose Employee."
                : "Manager accounts sign in through the Manager option. Go back and choose Manager.";
    }
}
