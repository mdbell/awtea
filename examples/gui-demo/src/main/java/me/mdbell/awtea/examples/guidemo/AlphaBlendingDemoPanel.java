package me.mdbell.awtea.examples.guidemo;

import java.awt.*;

/**
 * Panel demonstrating alpha blending and transparency effects.
 * 
 * Note: Alpha blending is now supported in the software rasterizer through the
 * setComposite() method. Use TAlphaComposite to control blending modes and alpha values.
 * 
 * Example usage in your AWT code:
 * <pre>
 * Graphics2D g2d = (Graphics2D) g;
 * // Set semi-transparent red
 * g2d.setComposite(TAlphaComposite.getInstance(TAlphaComposite.SRC_OVER, 0.5f));
 * g2d.setColor(Color.RED);
 * g2d.fillRect(10, 10, 100, 100);
 * </pre>
 */
public class AlphaBlendingDemoPanel extends Container {
    
    @Override
    public void paint(Graphics g) {
        // Background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());
        
        int yPos = 30;
        
        // Title
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString("Alpha Blending Support", 10, yPos);
        yPos += 40;
        
        // Description
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(Color.BLACK);
        g.drawString("Alpha blending is now available!", 10, yPos);
        yPos += 25;
        
        // Demo 1: Semi-transparent rectangles using alpha in Color
        g.drawString("Semi-transparent colors:", 10, yPos);
        yPos += 20;
        
        // Draw overlapping rectangles with alpha in the color
        g.setColor(new Color(255, 0, 0, 180)); // Semi-transparent red
        g.fillRect(30, yPos, 60, 60);
        
        g.setColor(new Color(0, 255, 0, 180)); // Semi-transparent green
        g.fillRect(60, yPos, 60, 60);
        
        g.setColor(new Color(0, 0, 255, 180)); // Semi-transparent blue
        g.fillRect(45, yPos + 30, 60, 60);
        
        yPos += 90;
        
        // Demo 2: Gradient effect
        g.setColor(Color.BLACK);
        g.drawString("Fading gradient effect:", 10, yPos);
        yPos += 20;
        
        // Create a gradient-like effect using decreasing alpha
        for (int i = 0; i < 10; i++) {
            int alpha = 255 - (i * 25);
            g.setColor(new Color(255, 150, 50, alpha));
            g.fillRect(20 + i * 20, yPos, 18, 40);
        }
        
        yPos += 60;
        
        // API Information
        g.setColor(Color.BLACK);
        g.drawString("API Usage:", 10, yPos);
        yPos += 20;
        
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setColor(new Color(60, 60, 60));
        g.drawString("Use setComposite() with TAlphaComposite", 10, yPos);
        yPos += 15;
        g.drawString("for advanced blending modes:", 10, yPos);
        yPos += 15;
        g.drawString("- SRC_OVER (default)", 10, yPos);
        yPos += 15;
        g.drawString("- SRC, DST, SRC_IN, DST_IN", 10, yPos);
        yPos += 15;
        g.drawString("- SRC_OUT, DST_OUT, XOR", 10, yPos);
        yPos += 15;
        g.drawString("- SRC_ATOP, DST_ATOP", 10, yPos);
        
        yPos += 25;
        
        // Info text
        g.setColor(Color.GRAY);
        g.setFont(new Font("SansSerif", Font.ITALIC, 10));
        g.drawString("Enables transparency and", 10, yPos);
        g.drawString("semi-transparent rendering.", 10, yPos + 15);
    }
}
