package persistence;

import java.util.function.Function;

/** Dito kukuha ng separate copies ng app state, di yung original agad. */
public interface AppStateRepository {
    AppState snapshot();

    <T> T transact(Function<AppState, T> operation);
}
