package ui;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Pictures for profiles and menu items, read from the "photos" folder next to the "data" folder.
 *
 *   photos/Jael Castillo.png        -> profile picture for Jael Castillo
 *   photos/menu/Classic Burger.jpg  -> picture on the Classic Burger menu card
 *   photos/logo.png                 -> restaurant logo (welcome screen and sidebar)
 *
 * .png, .jpg, and .jpeg all work. Profile photos can also be picked inside the app
 * (profile screen > Manage profiles), which copies the file into this folder.
 */
public final class Photos {
    private static final String[] EXTENSIONS = {".png", ".jpg", ".jpeg"};
    private static final Map<Path, Cached> CACHE = new HashMap<>();
    private static Path folder = Path.of("photos").toAbsolutePath();

    private Photos() {
    }

    /** Called once at startup with the app folder (the one that holds "data"). */
    public static void useFolder(Path photosFolder) {
        folder = photosFolder.toAbsolutePath().normalize();
        try {
            Files.createDirectories(folder.resolve("menu"));
        } catch (IOException ignored) {
            // the app works without photos
        }
    }

    public static Path folder() {
        return folder;
    }

    /** Profile picture for this person, or null when there is none. */
    public static BufferedImage person(String name) {
        return load(find(folder, fileName(name)));
    }

    public static BufferedImage menuItem(String name) {
        return load(find(folder.resolve("menu"), fileName(name)));
    }

    public static BufferedImage logo() {
        BufferedImage logo = load(find(folder, "logo"));
        return logo != null ? logo : load(find(Path.of("").toAbsolutePath(), "logo"));
    }

    /** Copies the chosen picture to photos/<name>.<ext>, replacing an older one. */
    public static void savePerson(String name, File source) throws IOException {
        if (ImageIO.read(source) == null) {
            throw new IOException("That file is not a picture. Choose a .png or .jpg file.");
        }
        String lower = source.getName().toLowerCase(Locale.ROOT);
        String extension = lower.endsWith(".png") ? ".png" : lower.endsWith(".jpeg") ? ".jpeg" : ".jpg";
        removePerson(name);
        Files.createDirectories(folder);
        Files.copy(source.toPath(), folder.resolve(fileName(name) + extension),
                StandardCopyOption.REPLACE_EXISTING);
    }

    public static void removePerson(String name) throws IOException {
        Path existing;
        while ((existing = find(folder, fileName(name))) != null) {
            Files.deleteIfExists(existing);
            CACHE.remove(existing);
        }
    }

    /** Characters Windows does not allow in file names are left out. */
    static String fileName(String name) {
        return name == null ? "" : name.replaceAll("[\\\\/:*?\"<>|]", "").trim();
    }

    /** Finds base.png / base.jpg / base.jpeg in the folder, ignoring upper/lower case. */
    private static Path find(Path directory, String base) {
        if (base.isEmpty() || !Files.isDirectory(directory)) {
            return null;
        }
        File[] files = directory.toFile().listFiles();
        if (files == null) {
            return null;
        }
        for (String extension : EXTENSIONS) {
            for (File file : files) {
                if (file.isFile() && file.getName().equalsIgnoreCase(base + extension)) {
                    return file.toPath();
                }
            }
        }
        return null;
    }

    private static BufferedImage load(Path file) {
        if (file == null) {
            return null;
        }
        try {
            long modified = Files.getLastModifiedTime(file).toMillis();
            Cached cached = CACHE.get(file);
            if (cached != null && cached.modified == modified) {
                return cached.image;
            }
            BufferedImage image = ImageIO.read(file.toFile());
            CACHE.put(file, new Cached(image, modified));
            return image;
        } catch (IOException | RuntimeException error) {
            return null;
        }
    }

    private record Cached(BufferedImage image, long modified) {
    }
}
