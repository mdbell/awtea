package me.mdbell.awtea.examples.guidemo;

import java.awt.*;

/**
 * Panel demonstrating various graphics primitives and text rendering.
 */
public class GraphicsDemoPanel extends Container {

    @Override
    public void paint(Graphics g) {
        // Background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());

        int yPos = 30;
        int spacing = 60;

        // Title
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString("Graphics Primitives", 10, yPos);
        yPos += spacing;

        // Rectangles
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("Rectangles:", 10, yPos);
        g.setColor(Color.RED);
        g.fillRect(100, yPos - 15, 50, 30);
        g.setColor(Color.BLUE);
        g.drawRect(180, yPos - 15, 50, 30);
        yPos += spacing;

        // Lines
        g.setColor(Color.BLACK);
        g.drawString("Lines:", 10, yPos);
        g.setColor(Color.MAGENTA);
        g.drawLine(100, yPos - 15, 150, yPos + 15);
        g.setColor(Color.CYAN);
        g.drawLine(180, yPos + 15, 220, yPos - 15);
        yPos += spacing;

        // Arcs
        g.setColor(Color.BLACK);
        g.drawString("Arcs:", 10, yPos);
        g.setColor(new Color(150, 75, 0));
        g.drawArc(100, yPos - 15, 50, 30, 0, 180);
        g.drawArc(180, yPos - 15, 50, 30, 45, 270);

        yPos += spacing;

        // Polygons
        g.setColor(Color.BLACK);
        g.drawString("Polygons:", 10, yPos);
        int[] xPoints = { 95, 125, 145, 115 };
        int[] yPoints = { yPos - 10, yPos - 20, yPos, yPos + 10 };
        g.setColor(Color.GREEN);
        g.drawPolygon(xPoints, yPoints, 4);

        for (int i = 0; i < xPoints.length; i++) {
            xPoints[i] += 75;
        }

        g.fillPolygon(xPoints, yPoints, 4);

        // Text styles
        yPos += spacing;
        g.setColor(Color.BLACK);
        g.drawString("Fonts:", 10, yPos);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("Plain", 75, yPos);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString("Bold", 120, yPos);
        g.setFont(new Font("SansSerif", Font.ITALIC, 12));
        g.drawString("Italic", 160, yPos);

        // Color palette at the bottom
        yPos += spacing;
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("Colors:", 10, yPos);

        Color[] colors = {
                Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
                Color.ORANGE, Color.PINK, Color.CYAN, Color.MAGENTA
        };

        for (int i = 0; i < colors.length; i++) {
            g.setColor(colors[i]);
            g.fillRect(75 + i * 22, yPos - 12, 18, 18);
        }
    }
}
