package service;

/** Resulta ng order cancellation at pag-void ng dining session nito. */
public record CancellationResult(
        String orderId,
        String sessionId,
        String tableId,
        boolean stockRestored) {
}
