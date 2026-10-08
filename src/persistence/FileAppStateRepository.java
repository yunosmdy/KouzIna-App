package persistence;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Buong app state yung naka-save sa local file.
 * Sa copy muna yung changes, tapos save sa temporary file.
 * Lipat muna sa data file bago gamitin as active state, para sabay yung update.
 */
public final class FileAppStateRepository implements AppStateRepository {
    private final Path dataFile;
    private final Path temporaryFile;
    private AppState activeState;

    public FileAppStateRepository(Path dataFile, Supplier<AppState> initialStateSupplier) {
        this.dataFile = Objects.requireNonNull(dataFile, "Data file is required.")
                .toAbsolutePath()
                .normalize();
        this.temporaryFile = this.dataFile.resolveSibling(this.dataFile.getFileName() + ".tmp");
        Supplier<AppState> requiredSupplier = Objects.requireNonNull(
                initialStateSupplier, "Initial-state supplier is required.");

        if (Files.exists(this.dataFile)) {
            activeState = loadState();
        } else {
            activeState = Objects.requireNonNull(
                    requiredSupplier.get(), "Initial state is required.").deepCopy();
            saveState(activeState);
        }
    }

    @Override
    public synchronized AppState snapshot() {
        return activeState.deepCopy();
    }

    @Override
    public synchronized <T> T transact(Function<AppState, T> operation) {
        Function<AppState, T> requiredOperation = Objects.requireNonNull(
                operation, "Transaction operation is required.");
        AppState workingState = activeState.deepCopy();
        T result = requiredOperation.apply(workingState);
        saveState(workingState);
        activeState = workingState;
        return result;
    }

    private AppState loadState() {
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(dataFile)) {
            @Override
            protected ObjectStreamClass readClassDescriptor()
                    throws IOException, ClassNotFoundException {
                return matchCurrentPackage(super.readClassDescriptor());
            }
        }) {
            Object value = input.readObject();
            if (!(value instanceof AppState state)) {
                throw new IllegalStateException("The Kóuz 'inà data file has an invalid format.");
            }
            if (state.getSchemaVersion() != AppState.CURRENT_SCHEMA_VERSION) {
                throw new IllegalStateException(
                        "Unsupported Kóuz 'inà data version " + state.getSchemaVersion() + ".");
            }
            return state;
        } catch (IOException | ClassNotFoundException error) {
            throw new IllegalStateException(
                    "Unable to load Kóuz 'inà data from " + dataFile + ".", error);
        }
    }

    // para mabasa pa rin yung old save kahit may com.kouzina pa yung package name
    private static ObjectStreamClass matchCurrentPackage(ObjectStreamClass saved)
            throws ClassNotFoundException, InvalidClassException {
        String currentName = saved.getName().replace("com.kouzina.", "");
        if (currentName.equals(saved.getName())) {
            return saved;
        }
        Class<?> currentType = Class.forName(
                currentName, false, FileAppStateRepository.class.getClassLoader());
        ObjectStreamClass current = ObjectStreamClass.lookup(currentType);
        if (current == null || (!currentType.isArray()
                && saved.getSerialVersionUID() != current.getSerialVersionUID())) {
            throw new InvalidClassException(saved.getName(), "Saved class version does not match.");
        }
        ObjectStreamField[] savedFields = saved.getFields();
        ObjectStreamField[] currentFields = current.getFields();
        if (savedFields.length != currentFields.length) {
            throw new InvalidClassException(saved.getName(), "Saved fields do not match.");
        }
        // package name lang nagbago dapat, di pwede tanggapin pag iba na yung fields
        for (int index = 0; index < savedFields.length; index++) {
            String savedType = savedFields[index].getTypeString();
            if (savedType != null) {
                savedType = savedType.replace("com/kouzina/", "");
            }
            if (!savedFields[index].getName().equals(currentFields[index].getName())
                    || savedFields[index].getTypeCode() != currentFields[index].getTypeCode()
                    || !Objects.equals(savedType, currentFields[index].getTypeString())) {
                throw new InvalidClassException(saved.getName(), "Saved fields do not match.");
            }
        }
        return current;
    }

    private void saveState(AppState state) {
        try {
            Path parent = dataFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (ObjectOutputStream output = new ObjectOutputStream(
                    Files.newOutputStream(temporaryFile))) {
                output.writeObject(state);
            }
            replaceDataFile();
        } catch (IOException error) {
            deleteTemporaryFile();
            throw new IllegalStateException(
                    "Unable to save Kóuz 'inà data to " + dataFile + ".", error);
        }
    }

    private void replaceDataFile() throws IOException {
        try {
            Files.move(
                    temporaryFile,
                    dataFile,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException error) {
            Files.move(temporaryFile, dataFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteTemporaryFile() {
        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException ignored) {
            // original data pa rin gamit kahit di matanggal yung temporary file
        }
    }
}
