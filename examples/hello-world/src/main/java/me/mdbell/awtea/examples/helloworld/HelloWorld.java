package me.mdbell.awtea.examples.helloworld;

import me.mdbell.awtea.util.logging.LogLevel;
import me.mdbell.awtea.util.logging.LoggerFactory;

import java.awt.*;

/**
 * A minimal awtea example that displays "Hello, awtea!" in a window.
 * This demonstrates the basic structure of an awtea application.
 */
public class HelloWorld {

    public static void main(String[] args) {

        LoggerFactory.setGlobalLevel(LogLevel.DEBUG);
        System.setProperty("me.mdbell.awtea.gfx.backend", "java");

        // Create the main window
        Frame frame = new Frame();
        frame.setTitle("Hello World - awtea Example");
        frame.setSize(600, 400);

        // Create a canvas for custom drawing
        Canvas canvas = new Canvas() {
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

        // Add the canvas to the frame
        frame.add(canvas);

        // Show the window
        frame.setVisible(true);
    }
}
