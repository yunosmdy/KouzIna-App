package ui;

import javax.swing.JComboBox;
import java.util.List;
import java.util.Objects;

/**
 * Dropdown that shows friendly names but remembers the ID behind each one.
 * Example: shows "Sairus Andurei | 0917-555-0101", returns "customer-1".
 */
public final class ChoiceBox extends JComboBox<Choice> {

    public ChoiceBox() {
        setPrototypeDisplayValue(new Choice("", "Mmmmmmmmmmmmmmmmmm"));
    }

    /** Replaces the options but keeps the current selection if it is still there. */
    public void setChoices(List<Choice> choices) {
        String keep = getSelectedChoiceId();
        removeAllItems();
        choices.forEach(this::addItem);
        if (keep != null) {
            selectId(keep);
        }
    }

    public void selectId(String id) {
        for (int index = 0; index < getItemCount(); index++) {
            if (Objects.equals(getItemAt(index).id(), id)) {
                setSelectedIndex(index);
                return;
            }
        }
    }

    /** The selected id, or null when nothing is selected. */
    public String getSelectedChoiceId() {
        Object selected = getSelectedItem();
        return selected instanceof Choice choice ? choice.id() : null;
    }

    /** The selected id; shows a friendly error when nothing is picked. */
    public String requireId(String fieldName) {
        String id = getSelectedChoiceId();
        if (id == null) {
            throw new IllegalArgumentException("Please choose a " + fieldName + " first.");
        }
        return id;
    }

    /** Fills the box with the numbers from..to (shown as text). */
    public ChoiceBox numbers(int from, int to) {
        List<Choice> numbers = new java.util.ArrayList<>();
        for (int value = from; value <= to; value++) {
            numbers.add(new Choice(String.valueOf(value), String.valueOf(value)));
        }
        setChoices(numbers);
        return this;
    }

    /** For number dropdowns like party size. */
    public int requireInt(String fieldName) {
        return Integer.parseInt(requireId(fieldName));
    }
}
