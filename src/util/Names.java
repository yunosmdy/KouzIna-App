package util;

import exception.ValidationException;

import java.util.regex.Pattern;

/**
 * One rule for people's names (guests and employee accounts): letters and spaces only.
 * Letters include ñ and accented letters (\p{L}), so "Peña" and "José" are fine.
 * Numbers and special characters like 1, @, -, ' and . are rejected.
 */
public final class Names {
    /** One or more words of letters, separated by single spaces. */
    private static final Pattern NAME = Pattern.compile("\\p{L}+( \\p{L}+)*");

    private Names() { }

    /** True for a character a name may contain (used by the screens to block typing). */
    public static boolean isAllowed(char ch) {
        return Character.isLetter(ch) || ch == ' ';
    }

    /** Trims and tidies the spaces, then checks the rule. Returns the cleaned name. */
    public static String require(String value, String fieldName, int maxLength) {
        String cleaned = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (cleaned.isEmpty()) {
            throw new ValidationException(fieldName + " is required.");
        }
        if (cleaned.length() > maxLength) {
            throw new ValidationException(fieldName + " can have at most " + maxLength + " characters.");
        }
        if (!NAME.matcher(cleaned).matches()) {
            throw new ValidationException(fieldName + " can only have letters and spaces (no numbers or symbols).");
        }
        return cleaned;
    }
}
