package ui;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Simple line icons drawn with code (no image files needed).
 * Names: home, calendar, order, kitchen, billing, chart, logout, bell, person, shield, back, arrow, clock,
 *        plus, pencil, users, swap, refresh, check
 */
public final class Icons {
    private Icons() {
    }

    public static Icon icon(String name, int size, Color color) {
        return new Icon() {
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                paint((Graphics2D) g, name, x, y, size, color);
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    /** Draws the icon inside a size x size box at (x, y). Shapes are designed on a 24x24 grid. */
    public static void paint(Graphics2D graphics, String name, int x, int y, int size, Color color) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.translate(x, y);
        g.scale(size / 24.0, size / 24.0);
        g.setColor(color);
        g.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (name) {
            case "home" -> {
                Path2D p = new Path2D.Float();
                p.moveTo(3, 11); p.lineTo(12, 3.5); p.lineTo(21, 11);
                g.draw(p);
                Path2D body = new Path2D.Float();
                body.moveTo(5.5, 9.5); body.lineTo(5.5, 20); body.lineTo(18.5, 20); body.lineTo(18.5, 9.5);
                g.draw(body);
                g.draw(new RoundRectangle2D.Float(10, 14, 4, 6, 1, 1));
            }
            case "calendar" -> {
                g.draw(new RoundRectangle2D.Float(3.5f, 5, 17, 15.5f, 3, 3));
                g.draw(new Line2D.Float(3.5f, 10, 20.5f, 10));
                g.draw(new Line2D.Float(8, 3, 8, 7));
                g.draw(new Line2D.Float(16, 3, 16, 7));
                g.fill(new Ellipse2D.Float(7, 13, 2.4f, 2.4f));
                g.fill(new Ellipse2D.Float(11, 13, 2.4f, 2.4f));
            }
            case "order" -> {
                g.draw(new RoundRectangle2D.Float(5, 4.5f, 14, 16.5f, 3, 3));
                g.draw(new RoundRectangle2D.Float(9, 2.5f, 6, 4, 2, 2));
                g.draw(new Line2D.Float(8.5f, 11, 15.5f, 11));
                g.draw(new Line2D.Float(8.5f, 14.5f, 15.5f, 14.5f));
                g.draw(new Line2D.Float(8.5f, 18, 12.5f, 18));
            }
            case "kitchen" -> {
                Path2D pot = new Path2D.Float();
                pot.moveTo(5, 10); pot.lineTo(5, 17); pot.quadTo(5, 20, 8, 20);
                pot.lineTo(16, 20); pot.quadTo(19, 20, 19, 17); pot.lineTo(19, 10);
                g.draw(pot);
                g.draw(new Line2D.Float(3, 10, 21, 10));
                g.draw(new Line2D.Float(2, 13, 5, 13));
                g.draw(new Line2D.Float(19, 13, 22, 13));
                g.draw(new Line2D.Float(9, 7, 9.5f, 4));
                g.draw(new Line2D.Float(12, 7, 12.5f, 4));
                g.draw(new Line2D.Float(15, 7, 15.5f, 4));
            }
            case "billing" -> {
                g.draw(new RoundRectangle2D.Float(2.5f, 6, 19, 13, 3, 3));
                g.draw(new Line2D.Float(2.5f, 10, 21.5f, 10));
                g.draw(new Line2D.Float(6, 15, 10, 15));
            }
            case "chart" -> {
                g.draw(new Line2D.Float(3.5f, 20.5f, 20.5f, 20.5f));
                g.draw(new RoundRectangle2D.Float(5, 12, 3.5f, 6, 1, 1));
                g.draw(new RoundRectangle2D.Float(10.5f, 7, 3.5f, 11, 1, 1));
                g.draw(new RoundRectangle2D.Float(16, 3.5f, 3.5f, 14.5f, 1, 1));
            }
            case "logout" -> {
                Path2D door = new Path2D.Float();
                door.moveTo(13, 4); door.lineTo(6, 4); door.quadTo(4, 4, 4, 6);
                door.lineTo(4, 18); door.quadTo(4, 20, 6, 20); door.lineTo(13, 20);
                g.draw(door);
                g.draw(new Line2D.Float(10, 12, 21, 12));
                Path2D arrow = new Path2D.Float();
                arrow.moveTo(17, 8); arrow.lineTo(21, 12); arrow.lineTo(17, 16);
                g.draw(arrow);
            }
            case "bell" -> {
                Path2D bell = new Path2D.Float();
                bell.moveTo(6, 17); bell.lineTo(6, 11); bell.curveTo(6, 7, 8.5f, 4.5f, 12, 4.5f);
                bell.curveTo(15.5f, 4.5f, 18, 7, 18, 11); bell.lineTo(18, 17);
                g.draw(bell);
                g.draw(new Line2D.Float(4, 17, 20, 17));
                g.draw(new Line2D.Float(10.5f, 20.5f, 13.5f, 20.5f));
            }
            case "person" -> {
                g.draw(new Ellipse2D.Float(8, 3.5f, 8, 8));
                Path2D body = new Path2D.Float();
                body.moveTo(4, 21); body.curveTo(4, 16, 7.5f, 14, 12, 14);
                body.curveTo(16.5f, 14, 20, 16, 20, 21);
                g.draw(body);
            }
            case "shield" -> {
                Path2D shield = new Path2D.Float();
                shield.moveTo(12, 2.5f); shield.lineTo(19.5f, 5.5f); shield.lineTo(19.5f, 11);
                shield.curveTo(19.5f, 16, 16, 19.5f, 12, 21.5f);
                shield.curveTo(8, 19.5f, 4.5f, 16, 4.5f, 11); shield.lineTo(4.5f, 5.5f); shield.closePath();
                g.draw(shield);
                Path2D check = new Path2D.Float();
                check.moveTo(8.5f, 12); check.lineTo(11, 14.5f); check.lineTo(15.5f, 9.5f);
                g.draw(check);
            }
            case "back" -> {
                g.draw(new Line2D.Float(19, 12, 5, 12));
                Path2D arrow = new Path2D.Float();
                arrow.moveTo(11, 6); arrow.lineTo(5, 12); arrow.lineTo(11, 18);
                g.draw(arrow);
            }
            case "arrow" -> {
                g.draw(new Line2D.Float(5, 12, 19, 12));
                Path2D arrow = new Path2D.Float();
                arrow.moveTo(13, 6); arrow.lineTo(19, 12); arrow.lineTo(13, 18);
                g.draw(arrow);
            }
            case "clock" -> {
                g.draw(new Ellipse2D.Float(3, 3, 18, 18));
                Path2D hands = new Path2D.Float();
                hands.moveTo(12, 7); hands.lineTo(12, 12); hands.lineTo(15.5f, 14);
                g.draw(hands);
            }
            case "plus" -> {
                g.draw(new Line2D.Float(12, 5, 12, 19));
                g.draw(new Line2D.Float(5, 12, 19, 12));
            }
            case "pencil" -> {
                Path2D pencil = new Path2D.Float();
                pencil.moveTo(4, 20); pencil.lineTo(5, 15.5f); pencil.lineTo(15.5f, 5);
                pencil.lineTo(19, 8.5f); pencil.lineTo(8.5f, 19); pencil.closePath();
                g.draw(pencil);
                g.draw(new Line2D.Float(13, 7.5f, 16.5f, 11));
            }
            case "users" -> {
                g.draw(new Ellipse2D.Float(4.5f, 5, 7, 7));
                Path2D body = new Path2D.Float();
                body.moveTo(2, 20); body.curveTo(2, 16, 4.8f, 14.5f, 8, 14.5f);
                body.curveTo(11.2f, 14.5f, 14, 16, 14, 20);
                g.draw(body);
                g.draw(new Ellipse2D.Float(14, 6, 5.5f, 5.5f));
                Path2D second = new Path2D.Float();
                second.moveTo(16, 14.6f); second.curveTo(19.5f, 14.6f, 22, 16.2f, 22, 19.5f);
                g.draw(second);
            }
            case "swap" -> {
                g.draw(new Line2D.Float(4, 8, 19, 8));
                Path2D top = new Path2D.Float();
                top.moveTo(15, 4); top.lineTo(19, 8); top.lineTo(15, 12);
                g.draw(top);
                g.draw(new Line2D.Float(20, 16, 5, 16));
                Path2D bottom = new Path2D.Float();
                bottom.moveTo(9, 12); bottom.lineTo(5, 16); bottom.lineTo(9, 20);
                g.draw(bottom);
            }
            case "refresh" -> {
                g.draw(new java.awt.geom.Arc2D.Float(4, 4, 16, 16, 60, 280, java.awt.geom.Arc2D.OPEN));
                Path2D head = new Path2D.Float();
                head.moveTo(16, 3.5f); head.lineTo(16.5f, 7.6f); head.lineTo(12.5f, 8);
                g.draw(head);
            }
            case "check" -> {
                Path2D check = new Path2D.Float();
                check.moveTo(5, 12.5f); check.lineTo(10, 17.5f); check.lineTo(19, 7);
                g.draw(check);
            }
            default -> g.draw(new Ellipse2D.Float(4, 4, 16, 16));
        }
        g.dispose();
    }
}
