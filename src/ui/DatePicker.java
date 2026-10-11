package ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Date field that opens a calendar when clicked. No typing needed.
 * Use getDate() / setDate(...); addChangeListener(...) runs when the user picks a day.
 */
public final class DatePicker extends JPanel {
    private static final DateTimeFormatter BUTTON_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy");

    private final JButton field = new JButton();
    private final List<Runnable> listeners = new ArrayList<>();
    private LocalDate date;
    private YearMonth shownMonth;

    public DatePicker(LocalDate initial) {
        super(new BorderLayout());
        setOpaque(false);
        this.date = Objects.requireNonNull(initial);
        Theme.outline(field);
        field.setHorizontalAlignment(SwingConstants.LEFT);
        field.setIcon(Icons.icon("calendar", 18, Theme.ORANGE_DARK));
        field.setIconTextGap(10);
        field.setFont(Theme.font(Font.PLAIN, 14f));
        field.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        field.setToolTipText("Click to pick a date");
        field.addActionListener(event -> openCalendar());
        add(field, BorderLayout.CENTER);
        updateText();
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate newDate) {
        this.date = Objects.requireNonNull(newDate);
        updateText();
    }

    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    private void updateText() {
        field.setText(date.format(BUTTON_FORMAT));
    }

    private void openCalendar() {
        shownMonth = YearMonth.from(date);
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.LINE),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        popup.setBackground(Theme.WHITE);
        JPanel calendar = new JPanel(new BorderLayout(0, 8));
        calendar.setBackground(Theme.WHITE);
        popup.add(calendar);
        fillCalendar(calendar, popup);
        popup.show(field, 0, field.getHeight() + 4);
    }

    private void fillCalendar(JPanel calendar, JPopupMenu popup) {
        calendar.removeAll();

        // Header: < October 2026 >
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JButton previous = navButton("‹");
        JButton next = navButton("›");
        JLabel month = new JLabel(shownMonth.format(MONTH_FORMAT), SwingConstants.CENTER);
        month.setFont(Theme.font(Font.BOLD, 15f));
        previous.addActionListener(event -> {
            shownMonth = shownMonth.minusMonths(1);
            fillCalendar(calendar, popup);
        });
        next.addActionListener(event -> {
            shownMonth = shownMonth.plusMonths(1);
            fillCalendar(calendar, popup);
        });
        header.add(previous, BorderLayout.WEST);
        header.add(month, BorderLayout.CENTER);
        header.add(next, BorderLayout.EAST);
        calendar.add(header, BorderLayout.NORTH);

        // Day grid
        JPanel grid = new JPanel(new GridLayout(0, 7, 4, 4));
        grid.setOpaque(false);
        for (String day : new String[]{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"}) {
            JLabel label = new JLabel(day, SwingConstants.CENTER);
            label.setFont(Theme.font(Font.BOLD, 11.5f));
            label.setForeground(Theme.MUTED);
            grid.add(label);
        }
        LocalDate first = shownMonth.atDay(1);
        int blanks = first.getDayOfWeek() == DayOfWeek.SUNDAY ? 0 : first.getDayOfWeek().getValue();
        for (int i = 0; i < blanks; i++) {
            grid.add(new JLabel());
        }
        LocalDate today = LocalDate.now();
        for (int day = 1; day <= shownMonth.lengthOfMonth(); day++) {
            LocalDate value = shownMonth.atDay(day);
            JButton button = new JButton(String.valueOf(day));
            boolean selected = value.equals(date);
            Theme.chip(button, selected);
            button.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
            button.setPreferredSize(new Dimension(38, 32));
            button.setFont(Theme.font(selected ? Font.BOLD : Font.PLAIN, 13f));
            button.putClientProperty(Theme.OUTLINE, value.equals(today) && !selected ? Theme.ORANGE : null);
            if (value.isBefore(today)) {
                button.setForeground(Theme.MUTED);
            }
            button.addActionListener(event -> {
                setDate(value);
                popup.setVisible(false);
                listeners.forEach(Runnable::run);
            });
            grid.add(button);
        }
        calendar.add(grid, BorderLayout.CENTER);

        JButton todayButton = Theme.subtle(new JButton("Today"));
        todayButton.addActionListener(event -> {
            setDate(today);
            popup.setVisible(false);
            listeners.forEach(Runnable::run);
        });
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(todayButton, BorderLayout.EAST);
        calendar.add(footer, BorderLayout.SOUTH);

        calendar.revalidate();
        calendar.repaint();
        popup.pack();
    }

    private static JButton navButton(String text) {
        JButton button = Theme.outline(new JButton(text));
        button.setFont(Theme.font(Font.BOLD, 18f));
        button.putClientProperty(Theme.ARC, 999);
        button.setBorder(BorderFactory.createEmptyBorder(0, 12, 2, 12));
        return button;
    }
}
