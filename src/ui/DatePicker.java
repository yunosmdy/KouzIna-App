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
 * Click the month name to jump: years grid -> months grid -> days.
 */
public final class DatePicker extends JPanel {
    private static final DateTimeFormatter BUTTON_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy");

    private final JButton field = new JButton();
    private final List<Runnable> listeners = new ArrayList<>();
    private LocalDate date;
    private YearMonth shownMonth;
    /** What the pop-up shows: the days of a month, the 12 months of a year, or a page of 12 years. */
    private enum View { DAYS, MONTHS, YEARS }
    private View view = View.DAYS;
    private static final int YEARS_PER_PAGE = 12;
    /** First year on the years page. */
    private int firstShownYear;

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
        view = View.DAYS;
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
        if (view == View.YEARS) {
            fillYears(calendar, popup);
        } else if (view == View.MONTHS) {
            fillMonths(calendar, popup);
        } else {
            fillDays(calendar, popup);
        }
        calendar.add(footer(popup), BorderLayout.SOUTH);
        calendar.revalidate();
        calendar.repaint();
        popup.pack();
    }

    /** Header: < [title] >. Clicking the title zooms out (days -> years, months -> years); null = plain title. */
    private JPanel header(String title, Runnable onTitle, Runnable onPrevious, Runnable onNext,
                          JPanel calendar, JPopupMenu popup) {
        JPanel header = new JPanel(new BorderLayout(6, 0));
        header.setOpaque(false);
        JButton previous = navButton("‹");
        JButton next = navButton("›");
        javax.swing.JComponent titleButton;
        if (onTitle == null) {
            JLabel label = new JLabel(title, SwingConstants.CENTER);
            label.setFont(Theme.font(Font.BOLD, 15f));
            titleButton = label;
        } else {
            JButton button = Theme.subtle(new JButton(title + "  ▾"));
            button.setFont(Theme.font(Font.BOLD, 15f));
            button.setToolTipText("Choose the year");
            button.addActionListener(event -> {
                onTitle.run();
                fillCalendar(calendar, popup);
            });
            titleButton = button;
        }
        previous.addActionListener(event -> {
            onPrevious.run();
            fillCalendar(calendar, popup);
        });
        next.addActionListener(event -> {
            onNext.run();
            fillCalendar(calendar, popup);
        });
        header.add(previous, BorderLayout.WEST);
        header.add(titleButton, BorderLayout.CENTER);
        header.add(next, BorderLayout.EAST);
        return header;
    }

    /** Page of 12 years, e.g. 2016-2027. Picking one shows that year's months. */
    private void fillYears(JPanel calendar, JPopupMenu popup) {
        int last = firstShownYear + YEARS_PER_PAGE - 1;
        calendar.add(header(firstShownYear + " – " + last, null,
                () -> firstShownYear -= YEARS_PER_PAGE, () -> firstShownYear += YEARS_PER_PAGE,
                calendar, popup), BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridLayout(0, 3, 6, 6));
        grid.setOpaque(false);
        for (int year = firstShownYear; year <= last; year++) {
            int value = year;
            JButton button = pickButton(String.valueOf(year), year == shownMonth.getYear(),
                    year == LocalDate.now().getYear());
            button.addActionListener(event -> {
                shownMonth = YearMonth.of(value, shownMonth.getMonthValue());
                view = View.MONTHS;
                fillCalendar(calendar, popup);
            });
            grid.add(button);
        }
        calendar.add(grid, BorderLayout.CENTER);
    }

    /** The 12 months of the shown year. Picking one shows its days. */
    private void fillMonths(JPanel calendar, JPopupMenu popup) {
        calendar.add(header(String.valueOf(shownMonth.getYear()), this::showYearsPage,
                () -> shownMonth = shownMonth.minusYears(1), () -> shownMonth = shownMonth.plusYears(1),
                calendar, popup), BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridLayout(0, 3, 6, 6));
        grid.setOpaque(false);
        YearMonth thisMonth = YearMonth.now();
        for (int month = 1; month <= 12; month++) {
            YearMonth value = YearMonth.of(shownMonth.getYear(), month);
            JButton button = pickButton(value.format(DateTimeFormatter.ofPattern("MMM")),
                    value.equals(YearMonth.from(date)), value.equals(thisMonth));
            button.addActionListener(event -> {
                shownMonth = value;
                view = View.DAYS;
                fillCalendar(calendar, popup);
            });
            grid.add(button);
        }
        calendar.add(grid, BorderLayout.CENTER);
    }

    /** Opens the page of years that contains the shown year. */
    private void showYearsPage() {
        int year = shownMonth.getYear();
        firstShownYear = year - Math.floorMod(year, YEARS_PER_PAGE);
        view = View.YEARS;
    }

    private static JButton pickButton(String text, boolean selected, boolean current) {
        JButton button = new JButton(text);
        Theme.chip(button, selected);
        button.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        button.setPreferredSize(new Dimension(84, 38));
        button.setFont(Theme.font(selected ? Font.BOLD : Font.PLAIN, 13.5f));
        button.putClientProperty(Theme.OUTLINE, current && !selected ? Theme.ORANGE : null);
        return button;
    }

    private void fillDays(JPanel calendar, JPopupMenu popup) {
        // Header: < October 2026 ▾ >   (click the month name to pick another year)
        calendar.add(header(shownMonth.format(MONTH_FORMAT), this::showYearsPage,
                () -> shownMonth = shownMonth.minusMonths(1), () -> shownMonth = shownMonth.plusMonths(1),
                calendar, popup), BorderLayout.NORTH);

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
    }

    private JPanel footer(JPopupMenu popup) {
        JButton todayButton = Theme.subtle(new JButton("Today"));
        todayButton.addActionListener(event -> {
            setDate(LocalDate.now());
            popup.setVisible(false);
            listeners.forEach(Runnable::run);
        });
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(todayButton, BorderLayout.EAST);
        return footer;
    }

    private static JButton navButton(String text) {
        JButton button = Theme.outline(new JButton(text));
        button.setFont(Theme.font(Font.BOLD, 18f));
        button.putClientProperty(Theme.ARC, 999);
        button.setBorder(BorderFactory.createEmptyBorder(0, 12, 2, 12));
        return button;
    }
}
