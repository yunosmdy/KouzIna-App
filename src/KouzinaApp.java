import bootstrap.ApplicationServices;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import ui.KouzinaFrame;
import ui.Photos;
import ui.Theme;

public final class KouzinaApp {
    private KouzinaApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Theme.apply();
                Path dataFile = resolveDataFile();
                // photos/ sits next to data/ (profile pictures, menu pictures, logo.png)
                Path appFolder = dataFile.getParent().getParent() != null
                        ? dataFile.getParent().getParent() : dataFile.getParent();
                Photos.useFolder(appFolder.resolve("photos"));
                ApplicationServices services = ApplicationServices.forDataFile(dataFile);
                new KouzinaFrame(services).setVisible(true);
            } catch (Exception error) {
                JOptionPane.showMessageDialog(
                        null,
                        "Kóuz 'Inà could not start: " + error.getMessage(),
                        "Startup error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /** Save file: -Dkouzina.data=... if given, otherwise data/kouzina.dat in the project folder. */
    public static Path resolveDataFile() throws URISyntaxException {
        String override = System.getProperty("kouzina.data");
        if (override != null) {
            return Path.of(override).toAbsolutePath().normalize();
        }
        Path workingFolder = Path.of("").toAbsolutePath().normalize();
        // VS Code may be opened at src/ or out/. Use the owning project, not its Java cache.
        for (Path folder = workingFolder; folder != null; folder = folder.getParent()) {
            if (Files.isRegularFile(folder.resolve("src/KouzinaApp.java"))) {
                return folder.resolve("data/kouzina.dat");
            }
        }
        Path applicationLocation = Path.of(KouzinaApp.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        return applicationLocation.getParent().resolve("data/kouzina.dat").normalize();
    }
}
