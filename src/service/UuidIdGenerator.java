package service;

import java.util.UUID;

/** Gumagawa ng IDs gamit ang random UUIDs. */
public final class UuidIdGenerator implements IdGenerator {
    @Override
    public String nextId() {
        return UUID.randomUUID().toString();
    }
}
