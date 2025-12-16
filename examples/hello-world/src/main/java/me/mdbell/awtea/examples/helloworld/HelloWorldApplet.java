package me.mdbell.awtea.examples.helloworld;

import me.mdbell.awtea.classlib.java.applet.AppletRegistry;

import java.applet.Applet;
import java.awt.*;

/**
 * A minimal awtea applet that displays "Hello, awtea!" in a canvas.
 * This demonstrates the basic structure of an awtea applet using the factory pattern.
 */
public class HelloWorldApplet extends Applet {
    
    // Register this applet with the registry at class load time
    static {
        AppletRegistry.register("hello-world", HelloWorldApplet::new);
    }
    
    private Canvas canvas;
    
    @Override
    public void init() {
        // Create a canvas for custom drawing
        canvas = new Canvas() {
            @Override
            public void paint(Graphics g) {
                // Set background color
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());
                
                // Draw "Hello, awtea!" text
                g.setColor(Color.BLACK);
                g.setFont(new Font("SansSerif", Font.BOLD, 32));
                
                String message = "Hello, awtea!";
                g.drawString(message, 150, 150);
                
                // Draw a subtitle
                g.setFont(new Font("SansSerif", Font.PLAIN, 16));
                g.setColor(new Color(100, 100, 100));
                String subtitle = "Java AWT running in your browser!";
                g.drawString(subtitle, 120, 200);
                
                // Draw a simple box around the text
                g.setColor(new Color(70, 130, 180));
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
