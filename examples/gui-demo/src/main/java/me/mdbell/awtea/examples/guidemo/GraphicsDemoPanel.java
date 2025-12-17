package me.mdbell.awtea.examples.guidemo;

import java.awt.*;

/**
 * Panel demonstrating various graphics primitives and text rendering.
 * Updated to showcase newly implemented primitives: drawOval, drawArc,
 * drawRoundRect, drawPolyline, and copyArea.
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
        g.drawString("Graphics Primitives Demo", 10, yPos);
        yPos += spacing;

        // Rectangles
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("Rectangles:", 10, yPos);
        g.setColor(Color.RED);
        g.fillRect(100, yPos - 15, 50, 30);
        g.setColor(Color.BLUE);
        g.drawRect(180, yPos - 15, 50, 30);
        yPos += spacing;

        // Rounded Rectangles (NEW!)
        g.setColor(Color.BLACK);
        g.drawString("Rounded Rects:", 10, yPos);
        g.setColor(Color.ORANGE);
        g.fillRoundRect(100, yPos - 15, 50, 30, 15, 15);
        g.setColor(new Color(128, 0, 128)); // Purple
        g.drawRoundRect(180, yPos - 15, 50, 30, 10, 10);
        yPos += spacing;

        // Ovals (NEW!)
        g.setColor(Color.BLACK);
        g.drawString("Ovals:", 10, yPos);
        g.setColor(Color.CYAN);
        g.fillOval(100, yPos - 15, 50, 30);
        g.setColor(Color.MAGENTA);
        g.drawOval(180, yPos - 15, 50, 30);
        yPos += spacing;

        // Lines
        g.setColor(Color.BLACK);
        g.drawString("Lines:", 10, yPos);
        g.setColor(Color.MAGENTA);
        g.drawLine(100, yPos - 15, 150, yPos + 15);
        g.setColor(Color.CYAN);
        g.drawLine(180, yPos + 15, 220, yPos - 15);
        yPos += spacing;

        // Arcs (UPDATED!)
        g.setColor(Color.BLACK);
        g.drawString("Arcs:", 10, yPos);
        g.setColor(new Color(255, 150, 0)); // Orange
        g.fillArc(100, yPos - 15, 50, 30, 0, 180);
        g.setColor(new Color(200, 0, 0)); // Darker red for better visibility
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
        yPos += spacing;

        // Polyline (NEW!)
        g.setColor(Color.BLACK);
        g.drawString("Polyline:", 10, yPos);
        int[] xPolyline = { 100, 120, 140, 160, 180, 200, 220 };
        int[] yPolyline = { yPos, yPos - 10, yPos - 5, yPos - 15, yPos, yPos + 5, yPos };
        g.setColor(Color.RED);
        g.drawPolyline(xPolyline, yPolyline, xPolyline.length);
        yPos += spacing;

        // Text styles
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

        // CopyArea demonstration (NEW!)
        yPos += spacing;
        g.setColor(Color.BLACK);
        g.drawString("CopyArea:", 10, yPos);
        // Draw a small pattern
        g.setColor(Color.BLUE);
        g.fillRect(100, yPos - 15, 20, 20);
        g.setColor(Color.RED);
        g.drawOval(105, yPos - 10, 10, 10);
        // Copy it to the right
        g.copyArea(100, yPos - 15, 20, 20, 50, 0);
        // And down
        g.copyArea(100, yPos - 15, 20, 20, 0, 30);
    }
}
