package service;

/**
 * Resulta ng reservation check-in o walk-in seating.
 * Kapag queued, waitlist entry ID lang ang laman. Kapag seated, may session,
 * order, at table IDs; may waitlist entry ID lang kung walk-in ang pinagmulan.
 */
public record SeatingResult(
        boolean queued,
        String sessionId,
        String orderId,
        String tableId,
        String waitlistEntryId) {
}
