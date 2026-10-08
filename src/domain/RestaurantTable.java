package domain;

import domain.enums.TableStatus;

import java.io.Serial;
import java.io.Serializable;

/** Physical table, current occupancy lang ibig sabihin ng status dito. */
public final class RestaurantTable implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final int tableNumber;
    private final int capacity;
    private TableStatus status;

    public RestaurantTable(String id, int tableNumber, int capacity) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Table ID is required.");
        }
        if (tableNumber <= 0) {
            throw new IllegalArgumentException("Table number must be positive.");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Table capacity must be positive.");
        }
        this.id = id.trim();
        this.tableNumber = tableNumber;
        this.capacity = capacity;
        this.status = TableStatus.AVAILABLE;
    }

    public String getId() {
        return id;
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public int getCapacity() {
        return capacity;
    }

    public TableStatus getStatus() {
        return status;
    }

    public boolean canSeat(int partySize) {
        return partySize > 0 && partySize <= capacity;
    }

    public boolean isAvailable() {
        return status == TableStatus.AVAILABLE;
    }

    public void occupy() {
        if (!isAvailable()) {
            throw new IllegalStateException("Table " + tableNumber + " is already occupied.");
        }
        status = TableStatus.OCCUPIED;
    }

    public void release() {
        if (isAvailable()) {
            throw new IllegalStateException("Table " + tableNumber + " is already available.");
        }
        status = TableStatus.AVAILABLE;
    }
}
