package ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Profile picture. Shows photos/<name>.png when it exists,
 * otherwise a colored tile with the person's initials (like a Netflix profile).
 */
public final class Avatar extends JComponent {
    private final int size;
    private final boolean round;
    private String name = "";
    private String colorKey = "";

    /** round = circle (sidebar), otherwise a rounded square (profile screen). */
    public Avatar(int size, boolean round) {
        this.size = size;
        this.round = round;
        Dimension dimension = new Dimension(size, size);
        setPreferredSize(dimension);
        setMinimumSize(dimension);
        setMaximumSize(dimension);
    }

    public void setPerson(String name, String colorKey) {
        this.name = name == null ? "" : name;
        this.colorKey = colorKey == null ? this.name : colorKey;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        paintAvatar((Graphics2D) g, name, colorKey, (getWidth() - size) / 2, (getHeight() - size) / 2, size, round);
    }

    /** Draws an avatar at (x, y). Also used by the profile tiles. */
    static void paintAvatar(Graphics2D graphics, String name, String colorKey, int x, int y, int size, boolean round) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        Shape shape = round
                ? new Ellipse2D.Float(x, y, size, size)
                : new RoundRectangle2D.Float(x, y, size, size, size * 0.18f, size * 0.18f);
        BufferedImage photo = Photos.person(name);
        if (photo != null) {
            g.clip(shape);
            // fill the square, cropping the longer side (like "cover")
            double scale = Math.max(size / (double) photo.getWidth(), size / (double) photo.getHeight());
            int width = (int) Math.ceil(photo.getWidth() * scale);
            int height = (int) Math.ceil(photo.getHeight() * scale);
            g.drawImage(photo, x + (size - width) / 2, y + (size - height) / 2, width, height, null);
        } else {
            Color color = Theme.avatarColor(colorKey);
            g.setColor(color);
            g.fill(shape);
            g.setColor(Color.WHITE);
            g.setFont(Theme.font(Font.BOLD, size * 0.36f));
            FontMetrics fm = g.getFontMetrics();
            String letters = initials(name);
            g.drawString(letters, x + (size - fm.stringWidth(letters)) / 2,
                    y + (size - fm.getHeight()) / 2 + fm.getAscent());
        }
        g.dispose();
    }

    /** "Jael Castillo" -> "JC", "Vera" -> "V". */
    static String initials(String name) {
        StringBuilder letters = new StringBuilder();
        String[] words = name.trim().split("\\s+");
        if (words.length > 0 && !words[0].isEmpty()) {
            letters.append(Character.toUpperCase(words[0].charAt(0)));
        }
        if (words.length > 1 && !words[words.length - 1].isEmpty()) {
            letters.append(Character.toUpperCase(words[words.length - 1].charAt(0)));
        }
        return letters.toString();
    }
}
