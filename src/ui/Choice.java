package ui;

/**
 * One option in a dropdown or one cell in a table: the user sees the label,
 * the code uses the hidden id. Example: label "Table 3", id "table-3".
 */
public record Choice(String id, String label) {
    @Override
    public String toString() {
        return label;
    }
}
