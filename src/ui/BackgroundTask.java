package ui;
import javax.swing.*;
import java.awt.Component;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
/** Password work runs off the event dispatch thread; UI callbacks run on it. */
final class BackgroundTask {
    private BackgroundTask() { }
    static <T> void run(Component parent, JButton button, Callable<T> work, Consumer<T> success) {
        button.setEnabled(false);
        new SwingWorker<T,Void>() {
            @Override protected T doInBackground() throws Exception { return work.call(); }
            @Override protected void done() {
                button.setEnabled(true);
                try { success.accept(get()); }
                catch (Exception error) {
                    Throwable cause=error instanceof java.util.concurrent.ExecutionException ? error.getCause() : error;
                    JOptionPane.showMessageDialog(parent,cause.getMessage()==null?"The action could not be completed.":cause.getMessage(),Theme.BRAND,JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }
}
