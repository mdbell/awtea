package me.mdbell.awtea.examples.helloworld;

import me.mdbell.awtea.classlib.java.applet.AppletRegistry;
import me.mdbell.awtea.classlib.java.applet.TApplet;
import me.mdbell.awtea.classlib.java.awt.TCanvas;
import me.mdbell.awtea.classlib.java.awt.TFont;
import me.mdbell.awtea.classlib.java.awt.TGraphics;
import org.teavm.classlib.java.awt.TColor;

/**
 * A minimal awtea applet that displays "Hello, awtea!" in a canvas.
 * This demonstrates the basic structure of an awtea applet using the factory pattern.
 */
public class HelloWorldApplet extends TApplet {
    
    // Register this applet with the registry at class load time
    static {
        AppletRegistry.register("hello-world", HelloWorldApplet::new);
    }
    
    private TCanvas canvas;
    
    @Override
    public void init() {
        // Create a canvas for custom drawing
        canvas = new TCanvas() {
            @Override
            public void paint(TGraphics g) {
                // Set background color
                g.setColor(TColor.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());
                
                // Draw "Hello, awtea!" text
                g.setColor(TColor.BLACK);
                g.setFont(new TFont("SansSerif", TFont.BOLD, 32));
                
                String message = "Hello, awtea!";
                g.drawString(message, 150, 150);
                
                // Draw a subtitle
                g.setFont(new TFont("SansSerif", TFont.PLAIN, 16));
                g.setColor(new TColor(100, 100, 100));
                String subtitle = "Java AWT running in your browser!";
                g.drawString(subtitle, 120, 200);
                
                // Draw a simple box around the text
                g.setColor(new TColor(70, 130, 180));
                g.drawRect(100, 100, 400, 150);
            }
        };
        
        // Set canvas size to fill the applet
        canvas.setSize(getWidth(), getHeight());
        
        // Add the canvas to the applet
        add(canvas);
    }
    
    @Override
    public void start() {
        // Request repaint when applet starts
        if (canvas != null) {
            canvas.repaint();
        }
    }
}
