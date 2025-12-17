package me.mdbell.awtea.examples.guidemo;

import java.awt.*;
import java.awt.event.*;

/**
 * Interactive drawing canvas that responds to mouse events.
 * Click to draw colored squares at the mouse position.
 */
public class DrawingCanvas extends Canvas {

    private static class DrawRect {
        int x, y, size;
        Color color;

        DrawRect(int x, int y, int size, Color color) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.color = color;
        }
    }

    private java.util.List<DrawRect> rects = new java.util.ArrayList<>();
    private int mouseX = -1;
    private int mouseY = -1;
    private Color[] colors = {
            Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE,
            Color.MAGENTA, Color.CYAN, Color.PINK, Color.YELLOW
    };
    private int colorIndex = 0;

    public DrawingCanvas() {
        // Add mouse listener for clicks
        addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
            }

            public void mousePressed(MouseEvent e) {
                // Add a square at the click position
                rects.add(new DrawRect(e.getX(), e.getY(), 30, colors[colorIndex]));
                colorIndex = (colorIndex + 1) % colors.length;
                repaint();
            }

            public void mouseReleased(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }
        });

        // Add mouse motion listener to track position
        addMouseMotionListener(new MouseMotionListener() {
            public void mouseDragged(MouseEvent e) {
            }

            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                repaint();
            }
        });
    }

    @Override
    public void paint(Graphics g) {
        // Background
        g.setColor(new Color(240, 240, 255));
        g.fillRect(0, 0, getWidth(), getHeight());

        // Draw grid
        g.setColor(new Color(220, 220, 230));
        for (int i = 0; i < getWidth(); i += 50) {
            g.drawLine(i, 0, i, getHeight());
        }
        for (int i = 0; i < getHeight(); i += 50) {
            g.drawLine(0, i, getWidth(), i);
        }

        // Draw all squares
        for (DrawRect rect : rects) {
            g.setColor(rect.color);
            g.fillRect(rect.x - rect.size / 2, rect.y - rect.size / 2, rect.size, rect.size);
            g.setColor(Color.BLACK);
            g.drawRect(rect.x - rect.size / 2, rect.y - rect.size / 2, rect.size, rect.size);
        }

        // Draw instructions and mouse position
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("Click anywhere to draw squares!", 10, 20);

        if (mouseX >= 0 && mouseY >= 0) {
            g.drawString("Mouse: (" + mouseX + ", " + mouseY + ")", 10, 40);
        }

        g.drawString("Shapes drawn: " + rects.size(), 10, 60);
    }

    /**
     * Clears all drawn shapes from the canvas.
     */
    public void clear() {
        rects.clear();
        repaint();
    }

    /**
     * Randomizes the color used for the next shape.
     */
    public void randomizeColor() {
        colorIndex = (int) (Math.random() * colors.length);
    }
}
