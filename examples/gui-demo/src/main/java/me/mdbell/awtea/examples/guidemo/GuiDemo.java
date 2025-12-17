package me.mdbell.awtea.examples.guidemo;

import me.mdbell.awtea.util.logging.LogLevel;
import me.mdbell.awtea.util.logging.LoggerFactory;

import java.awt.*;

/**
 * Comprehensive GUI demo showcasing awtea features including:
 * - Multiple containers and components
 * - Interactive drawing canvas
 * - Graphics primitives
 * - Event handling
 * - Text rendering
 */
public class GuiDemo {

    public static void main(String[] args) {

        // LoggerFactory.setGlobalLevel(LogLevel.TRACE);
        System.setProperty("me.mdbell.awtea.wasm.module_path", "/awtea-graphics/build/wasm/awt_raster.wasm");
        System.setProperty("me.mdbell.awtea.gfx.backend", "java");
        System.setProperty("me.mdbell.awtea.font.subpixel", "true");
        System.setProperty("me.mdbell.awtea.font.supersample", "4");

        // Create the main window
        Frame frame = new Frame();
        frame.setTitle("GUI Demo - awtea Example");

        // Create a container to hold everything
        Container mainPanel = new Container();

        // Add title panel at the top
        Container titlePanel = new Container() {
            @Override
            public void paint(Graphics g) {
                g.setColor(new Color(70, 130, 180)); // Steel blue
                g.fillRect(0, 0, getWidth(), getHeight());

                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, 24));
                g.drawString("awtea GUI Demo", 400, 35);

                g.setFont(new Font("SansSerif", Font.PLAIN, 14));
                g.drawString("Interactive AWT components running in your browser", 350, 55);
            }
        };
        titlePanel.setBounds(0, 0, 1050, 70);
        mainPanel.add(titlePanel);

        // Add interactive drawing canvas
        DrawingCanvas canvas = new DrawingCanvas();
        canvas.setBounds(10, 80, 500, 300);
        mainPanel.add(canvas);

        // Add graphics demo panel (increased height to show all primitives)
        GraphicsDemoPanel demoPanel = new GraphicsDemoPanel();
        demoPanel.setBounds(520, 80, 270, 660);
        mainPanel.add(demoPanel);

        // Add alpha blending demo panel
        AlphaBlendingDemoPanel alphaPanel = new AlphaBlendingDemoPanel();
        alphaPanel.setBounds(800, 80, 220, 660);
        mainPanel.add(alphaPanel);

        // Add info panel at the bottom (moved down to accommodate taller panels)
        Container infoPanel = new Container() {
            @Override
            public void paint(Graphics g) {
                g.setColor(new Color(245, 245, 245));
                g.fillRect(0, 0, getWidth(), getHeight());

                // Draw border
                g.setColor(new Color(200, 200, 200));
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

                g.setColor(Color.BLACK);
                g.setFont(new Font("SansSerif", Font.BOLD, 12));
                g.drawString("Instructions:", 10, 20);

                g.setFont(new Font("SansSerif", Font.PLAIN, 11));
                g.drawString("• Click on the left canvas to draw colored squares", 10, 40);
                g.drawString("• Move your mouse to see real-time coordinates", 10, 55);
                g.drawString("• View graphics primitives and alpha blending on right", 10, 70);

                g.setFont(new Font("SansSerif", Font.ITALIC, 10));
                g.setColor(Color.GRAY);
                g.drawString("Powered by awtea - Java AWT for the Web", 10, 95);
            }
        };
        infoPanel.setBounds(10, 630, 500, 110);
        mainPanel.add(infoPanel);

        mainPanel.setSize(1030, 760);

        // Add the main panel to the frame
        frame.add(mainPanel);

        frame.pack();

        // Show the window
        frame.setVisible(true);
    }
}
