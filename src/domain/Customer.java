package domain;

import java.io.Serial;
import java.io.Serializable;

/** Customer record para sa reservations, walk-ins, at visit history. */
public final class Customer implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private String firstName;
    private String lastName;
    private String phoneNumber;

    public Customer(String id, String firstName, String lastName, String phoneNumber) {
        this.id = requireNonBlank(id, "Customer ID");
        updateContact(firstName, lastName, phoneNumber);
    }

    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void updateContact(String firstName, String lastName, String phoneNumber) {
        // check muna lahat para pag may blank, walang half-done na update
        String newFirstName = requireNonBlank(firstName, "First name");
        String newLastName = requireNonBlank(lastName, "Last name");
        String newPhoneNumber = requireNonBlank(phoneNumber, "Phone number");
        this.firstName = newFirstName;
        this.lastName = newLastName;
        this.phoneNumber = newPhoneNumber;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }
}
