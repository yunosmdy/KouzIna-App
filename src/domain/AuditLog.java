package domain;

import exception.ValidationException;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** Hindi nababagong record ng mahalagang employee action. */
public final class AuditLog implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final LocalDateTime timestamp;
    private final String employeeId;
    private final String action;
    private final String detail;

    public AuditLog(String id, LocalDateTime timestamp, String employeeId,
                    String action, String detail) {
        this.id = requireNonBlank(id, "Audit log ID");
        if (timestamp == null) {
            throw new ValidationException("Audit timestamp is required.");
        }
        this.timestamp = timestamp;
        this.employeeId = requireNonBlank(employeeId, "Employee ID");
        this.action = requireNonBlank(action, "Audit action");
        this.detail = requireNonBlank(detail, "Audit detail");
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getAction() {
        return action;
    }

    public String getDetail() {
        return detail;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required.");
        }
        return value.trim();
    }
}
