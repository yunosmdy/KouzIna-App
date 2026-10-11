package service;
import exception.ValidationException;
import java.util.*;
final class AccountValidation {
    private AccountValidation() { }
    static String username(String input) {
        String value = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
        if (!value.matches("[a-z0-9][a-z0-9._-]{2,29}"))
            throw new ValidationException("Username: 3–30 characters, letters, numbers, dot, underscore or hyphen; start with a letter or number.");
        return value;
    }
    static String name(String input) {
        String value = input == null ? "" : input.trim().replaceAll("\\s+", " ");
        if (value.isBlank() || value.length() > 80) throw new ValidationException("Enter a full name of 1–80 characters.");
        return value;
    }
    static void passwords(char[] password, char[] confirmation) {
        if (password == null || password.length < 8 || password.length > 128)
            throw new ValidationException("Password must contain 8–128 characters.");
        if (!Arrays.equals(password, confirmation)) throw new ValidationException("Passwords do not match.");
    }
}
