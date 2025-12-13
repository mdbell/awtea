package me.mdbell.awtea.examples.guidemo;

import me.mdbell.awtea.classlib.java.awt.*;
import me.mdbell.awtea.classlib.java.awt.event.*;
import java.awt.Color;

/**
 * Interactive drawing canvas that responds to mouse events.
 * Click to draw colored rectangles at the mouse position.
 */
public class DrawingCanvas extends TCanvas {
    
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
        addMouseListener(new TMouseListener() {
            public void mouseClicked(TMouseEvent e) {}
            
            public void mousePressed(TMouseEvent e) {
                // Add a rectangle at the click position
                rects.add(new DrawRect(e.getX(), e.getY(), 30, colors[colorIndex]));
                colorIndex = (colorIndex + 1) % colors.length;
                repaint();
            }
            
            public void mouseReleased(TMouseEvent e) {}
            public void mouseEntered(TMouseEvent e) {}
            public void mouseExited(TMouseEvent e) {}
        });
        
        // Add mouse motion listener to track position
        addMouseMotionListener(new TMouseMotionListener() {
            public void mouseDragged(TMouseEvent e) {}
            
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
        
        // Draw all rectangles
        for (DrawRect rect : rects) {
            g.setColor(rect.color);
            g.fillRect(rect.x - rect.size/2, rect.y - rect.size/2, rect.size, rect.size);
            g.setColor(Color.BLACK);
            g.drawRect(rect.x - rect.size/2, rect.y - rect.size/2, rect.size, rect.size);
        }
        
        // Draw instructions and mouse position
        g.setColor(Color.BLACK);
        g.setFont(new TFont("SansSerif", TFont.PLAIN, 12));
        g.drawString("Click anywhere to draw squares!", 10, 20);
        
        if (mouseX >= 0 && mouseY >= 0) {
            g.drawString("Mouse: (" + mouseX + ", " + mouseY + ")", 10, 40);
        }
        
        g.drawString("Shapes drawn: " + rects.size(), 10, 60);
    }
}
