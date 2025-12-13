package me.mdbell.awtea.examples.guidemo;

import me.mdbell.awtea.classlib.java.awt.*;

/**
 * Panel demonstrating various graphics primitives and text rendering.
 */
public class GraphicsDemoPanel extends TPanel {
    
    @Override
    public void paint(TGraphics g) {
        // Background
        g.setColor(TColor.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());
        
        int yPos = 30;
        int spacing = 80;
        
        // Title
        g.setColor(TColor.BLACK);
        g.setFont(new TFont("SansSerif", TFont.BOLD, 18));
        g.drawString("Graphics Primitives Demo", 10, yPos);
        yPos += spacing;
        
        // Rectangles
        g.setFont(new TFont("SansSerif", TFont.PLAIN, 12));
        g.drawString("Rectangles:", 10, yPos);
        g.setColor(TColor.RED);
        g.fillRect(150, yPos - 20, 60, 40);
        g.setColor(TColor.BLUE);
        g.drawRect(230, yPos - 20, 60, 40);
        yPos += spacing;
        
        // Ovals
        g.setColor(TColor.BLACK);
        g.drawString("Ovals:", 10, yPos);
        g.setColor(TColor.GREEN);
        g.fillOval(150, yPos - 20, 60, 40);
        g.setColor(TColor.ORANGE);
        g.drawOval(230, yPos - 20, 60, 40);
        yPos += spacing;
        
        // Lines
        g.setColor(TColor.BLACK);
        g.drawString("Lines:", 10, yPos);
        g.setColor(TColor.MAGENTA);
        g.drawLine(150, yPos - 20, 210, yPos + 20);
        g.setColor(TColor.CYAN);
        g.drawLine(230, yPos + 20, 290, yPos - 20);
        yPos += spacing;
        
        // Arcs
        g.setColor(TColor.BLACK);
        g.drawString("Arcs:", 10, yPos);
        g.setColor(TColor.PINK);
        g.fillArc(150, yPos - 20, 60, 40, 0, 180);
        g.setColor(new TColor(150, 75, 0)); // Brown
        g.drawArc(230, yPos - 20, 60, 40, 45, 270);
        yPos += spacing;
        
        // Text with different fonts
        g.setColor(TColor.BLACK);
        g.drawString("Text Styles:", 10, yPos);
        g.setFont(new TFont("SansSerif", TFont.PLAIN, 14));
        g.drawString("Plain", 150, yPos);
        g.setFont(new TFont("SansSerif", TFont.BOLD, 14));
        g.drawString("Bold", 210, yPos);
        g.setFont(new TFont("SansSerif", TFont.ITALIC, 14));
        g.drawString("Italic", 260, yPos);
        yPos += spacing;
        
        // Color palette
        g.setFont(new TFont("SansSerif", TFont.PLAIN, 12));
        g.setColor(TColor.BLACK);
        g.drawString("Colors:", 10, yPos);
        
        TColor[] palette = {
            TColor.RED, TColor.GREEN, TColor.BLUE, TColor.YELLOW,
            TColor.ORANGE, TColor.PINK, TColor.CYAN, TColor.MAGENTA
        };
        
        for (int i = 0; i < palette.length; i++) {
            g.setColor(palette[i]);
            g.fillRect(150 + i * 25, yPos - 15, 20, 20);
        }
    }
}
