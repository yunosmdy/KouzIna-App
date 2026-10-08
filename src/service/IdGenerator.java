package service;

/** Nagbibigay ng IDs nang hindi nakatali ang services sa isang paraan ng paggawa nito. */
@FunctionalInterface
public interface IdGenerator {
    String nextId();
}
