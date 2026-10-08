package persistence;

import java.util.Objects;
import java.util.function.Function;

/** Sa memory lang to, sabay apply ng changes pag successful yung transaction. */
public final class InMemoryAppStateRepository implements AppStateRepository {
    private AppState activeState;

    public InMemoryAppStateRepository(AppState initialState) {
        activeState = Objects.requireNonNull(initialState, "Initial state is required.").deepCopy();
    }

    @Override
    public synchronized AppState snapshot() {
        return activeState.deepCopy();
    }

    @Override
    public synchronized <T> T transact(Function<AppState, T> operation) {
        Function<AppState, T> requiredOperation =
                Objects.requireNonNull(operation, "Transaction operation is required.");
        AppState workingState = activeState.deepCopy();
        T result = requiredOperation.apply(workingState);
        activeState = workingState;
        return result;
    }
}
