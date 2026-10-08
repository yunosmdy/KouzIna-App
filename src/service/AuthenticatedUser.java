package service;

/** Employee details matapos mag-login; walang password credentials na kasama. */
public record AuthenticatedUser(
        String employeeId,
        String username,
        String displayName,
        String roleName) {

    public AuthenticatedUser {
        employeeId = requireNonBlank(employeeId, "Employee ID");
        username = requireNonBlank(username, "Username");
        displayName = requireNonBlank(displayName, "Display name");
        roleName = requireNonBlank(roleName, "Role name");
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }
}
