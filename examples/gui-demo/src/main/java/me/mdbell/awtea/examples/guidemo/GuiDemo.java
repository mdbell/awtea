package me.mdbell.awtea.examples.guidemo;

import me.mdbell.awtea.classlib.java.awt.*;

/**
 * Comprehensive GUI demo showcasing awtea features including:
 * - Multiple panels and components
 * - Interactive drawing canvas
 * - Graphics primitives
 * - Event handling
 * - Text rendering
 */
public class GuiDemo {
    
    public static void main(String[] args) {
        // Create the main window
        TFrame frame = new TFrame();
        frame.setTitle("GUI Demo - awtea Example");
        frame.setSize(800, 600);
        
        // Create a container panel to hold everything
        TPanel mainPanel = new TPanel();
        mainPanel.setLayout(null); // Using absolute positioning for simplicity
        
        // Add title label at the top
        TPanel titlePanel = new TPanel() {
            @Override
            public void paint(TGraphics g) {
                g.setColor(new TColor(70, 130, 180)); // Steel blue
                g.fillRect(0, 0, getWidth(), getHeight());
                
                g.setColor(TColor.WHITE);
                g.setFont(new TFont("SansSerif", TFont.BOLD, 24));
                String title = "awtea GUI Demo";
                TFontMetrics metrics = g.getFontMetrics();
                int x = (getWidth() - metrics.stringWidth(title)) / 2;
                g.drawString(title, x, 35);
                
                g.setFont(new TFont("SansSerif", TFont.PLAIN, 12));
                String subtitle = "Interactive AWT components running in your browser";
                metrics = g.getFontMetrics();
                x = (getWidth() - metrics.stringWidth(subtitle)) / 2;
                g.drawString(subtitle, x, 55);
            }
        };
        titlePanel.setBounds(0, 0, 800, 70);
        mainPanel.add(titlePanel);
        
        // Add interactive drawing canvas
        DrawingCanvas canvas = new DrawingCanvas();
        canvas.setBounds(10, 80, 500, 300);
        mainPanel.add(canvas);
        
        // Add graphics demo panel
        GraphicsDemoPanel demoPanel = new GraphicsDemoPanel();
        demoPanel.setBounds(520, 80, 270, 500);
        mainPanel.add(demoPanel);
        
        // Add info panel at the bottom
        TPanel infoPanel = new TPanel() {
            @Override
            public void paint(TGraphics g) {
                g.setColor(new TColor(245, 245, 245));
                g.fillRect(0, 0, getWidth(), getHeight());
                
                // Draw border
                g.setColor(new TColor(200, 200, 200));
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                
                g.setColor(TColor.BLACK);
                g.setFont(new TFont("SansSerif", TFont.BOLD, 12));
                g.drawString("Instructions:", 10, 20);
                
                g.setFont(new TFont("SansSerif", TFont.PLAIN, 11));
                g.drawString("• Click on the left canvas to draw colored circles", 10, 40);
                g.drawString("• Move your mouse to see real-time coordinates", 10, 55);
                g.drawString("• View graphics primitives on the right panel", 10, 70);
                
                g.setFont(new TFont("SansSerif", TFont.ITALIC, 10));
                g.setColor(TColor.GRAY);
                g.drawString("Powered by awtea - Java AWT for the Web", 10, 95);
            }
        };
        infoPanel.setBounds(10, 390, 500, 110);
        mainPanel.add(infoPanel);
        
        // Add the main panel to the frame
        frame.add(mainPanel);
        
        // Show the window
        frame.setVisible(true);
    }
}
