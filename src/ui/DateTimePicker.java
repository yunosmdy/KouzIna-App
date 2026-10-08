package ui;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Calendar date + time dropdown (every 30 minutes). Used for reservation start/end.
 * getDateTime() returns the combined value.
 */
public final class DateTimePicker extends JPanel {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");
    private static final int STEP_MINUTES = 30;

    private final DatePicker datePicker;
    private final JComboBox<LocalTime> timeBox = new JComboBox<>();

    public DateTimePicker(LocalDateTime initial) {
        super(new BorderLayout(8, 0));
        setOpaque(false);
        datePicker = new DatePicker(initial.toLocalDate());
        for (LocalTime time = LocalTime.MIDNIGHT; ; time = time.plusMinutes(STEP_MINUTES)) {
            timeBox.addItem(time);
            if (time.equals(LocalTime.of(23, 60 - STEP_MINUTES))) {
                break;
            }
        }
        timeBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, false);
                setText(value instanceof LocalTime time ? time.format(TIME_FORMAT) : "");
                setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 8, 5, 8));
                setOpaque(index >= 0);
                setBackground(selected ? Theme.BLUE_TINT : Theme.WHITE);
                setForeground(Theme.TEXT);
                return this;
            }
        });
        timeBox.setPrototypeDisplayValue(LocalTime.of(12, 30));
        timeBox.setToolTipText("Pick a time");
        setDateTime(initial);
        add(datePicker, BorderLayout.CENTER);
        add(timeBox, BorderLayout.EAST);
    }

    public LocalDateTime getDateTime() {
        return LocalDateTime.of(datePicker.getDate(), (LocalTime) timeBox.getSelectedItem());
    }

    /** Sets the value, rounding the time to the nearest 30 minutes. */
    public void setDateTime(LocalDateTime value) {
        LocalDate date = value.toLocalDate();
        int minutes = value.getHour() * 60 + value.getMinute();
        int rounded = Math.round(minutes / (float) STEP_MINUTES) * STEP_MINUTES;
        if (rounded >= 24 * 60) {
            rounded = 24 * 60 - STEP_MINUTES;
        }
        datePicker.setDate(date);
        timeBox.setSelectedItem(LocalTime.of(rounded / 60, rounded % 60));
    }

    /** Runs when either the date or the time changes. */
    public void addChangeListener(Runnable listener) {
        datePicker.addChangeListener(listener);
        timeBox.addActionListener(event -> listener.run());
    }
}
