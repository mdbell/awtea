package me.mdbell.awtea.examples.guidemo;

import me.mdbell.awtea.classlib.java.awt.*;
import me.mdbell.awtea.classlib.java.awt.event.*;
import java.awt.Color;

/**
 * Interactive drawing canvas that responds to mouse events.
 * Click to draw circles at the mouse position.
 */
public class DrawingCanvas extends TCanvas {
    
    private static class Circle {
        int x, y, radius;
        Color color;
        
        Circle(int x, int y, int radius, Color color) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.color = color;
        }
    }
    
    private java.util.List<Circle> circles = new java.util.ArrayList<>();
    private int mouseX = -1;
    private int mouseY = -1;
    private Color[] colors = {
        Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE,
        Color.MAGENTA, Color.CYAN, Color.PINK, Color.YELLOW
    };
    private int colorIndex = 0;
    
    public DrawingCanvas() {
        // Add mouse listener for clicks
        addMouseListener(new TMouseAdapter() {
            @Override
            public void mousePressed(TMouseEvent e) {
                // Add a circle at the click position
                circles.add(new Circle(e.getX(), e.getY(), 20, colors[colorIndex]));
                colorIndex = (colorIndex + 1) % colors.length;
                repaint();
            }
        });
        
        // Add mouse motion listener to track position
        addMouseMotionListener(new TMouseMotionAdapter() {
            @Override
            public void mouseMoved(TMouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                repaint();
            }
        });
    }
    
    @Override
    public void paint(TGraphics g) {
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
        
        // Draw all circles
        for (Circle circle : circles) {
            g.setColor(circle.color);
            g.fillOval(circle.x - circle.radius, circle.y - circle.radius, 
                      circle.radius * 2, circle.radius * 2);
            g.setColor(Color.BLACK);
            g.drawOval(circle.x - circle.radius, circle.y - circle.radius, 
                      circle.radius * 2, circle.radius * 2);
        }
        
        // Draw instructions and mouse position
        g.setColor(Color.BLACK);
        g.setFont(new TFont("SansSerif", TFont.PLAIN, 12));
        g.drawString("Click anywhere to draw circles!", 10, 20);
        
        if (mouseX >= 0 && mouseY >= 0) {
            g.drawString("Mouse: (" + mouseX + ", " + mouseY + ")", 10, 40);
        }
        
        g.drawString("Circles drawn: " + circles.size(), 10, 60);
    }
}
