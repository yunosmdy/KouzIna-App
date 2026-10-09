package service;
/** Display identity is separate from the random, process-local authorization token. */
public record AuthenticatedUser(String employeeId, String username, String displayName,
                                String roleName, String sessionToken) { }
