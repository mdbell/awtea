package me.mdbell.awtea.examples.helloworld;

import me.mdbell.awtea.classlib.java.awt.*;
import java.awt.Color;

/**
 * A minimal awtea example that displays "Hello, awtea!" in a window.
 * This demonstrates the basic structure of an awtea application.
 */
public class HelloWorld {
    
    public static void main(String[] args) {
        // Create the main window
        TFrame frame = new TFrame();
        frame.setTitle("Hello World - awtea Example");
        frame.setSize(400, 300);
        
        // Create a canvas for custom drawing
        TCanvas canvas = new TCanvas() {
            @Override
            public void paint(TGraphics g) {
                // Set background color
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());
                
                // Draw "Hello, awtea!" text
                g.setColor(Color.BLACK);
                g.setFont(new TFont("SansSerif", TFont.BOLD, 32));
                
                String message = "Hello, awtea!";
                TFontMetrics metrics = g.getFontMetrics();
                int x = (getWidth() - metrics.stringWidth(message)) / 2;
                int y = (getHeight() + metrics.getAscent()) / 2;
                
                g.drawString(message, x, y);
                
                // Draw a subtitle
                g.setFont(new TFont("SansSerif", TFont.PLAIN, 14));
                g.setColor(Color.GRAY);
                String subtitle = "Java AWT running in your browser!";
                metrics = g.getFontMetrics();
                int subX = (getWidth() - metrics.stringWidth(subtitle)) / 2;
                int subY = y + 40;
                g.drawString(subtitle, subX, subY);
            }
        };
        
        // Add the canvas to the frame
        frame.add(canvas);
        
        // Show the window
        frame.setVisible(true);
    }
}
