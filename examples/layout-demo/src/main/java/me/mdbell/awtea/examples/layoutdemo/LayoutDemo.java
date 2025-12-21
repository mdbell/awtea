package me.mdbell.awtea.examples.layoutdemo;

import me.mdbell.awtea.util.logging.LogLevel;
import me.mdbell.awtea.util.logging.LoggerFactory;

import java.awt.*;

/**
 * Comprehensive demo showcasing all AWT layout managers in awtea:
 * - BorderLayout
 * - FlowLayout
 * - GridLayout
 * - CardLayout
 */
public class LayoutDemo {

    public static void main(String[] args) {
        LoggerFactory.setGlobalLevel(LogLevel.INFO);
        System.setProperty("me.mdbell.awtea.wasm.module_path", "awt_raster.wasm");
        System.setProperty("me.mdbell.awtea.font.subpixel", "true");
        System.setProperty("me.mdbell.awtea.font.supersample", "4");

        // Create the main window
        Frame frame = new Frame();
        frame.setTitle("Layout Manager Demo - awtea");

        // Use BorderLayout for main frame to show all demos at once
        frame.setLayout(new BorderLayout(10, 10));

        // Create title panel
        Container titlePanel = createColoredPanel("AWT Layout Managers Demo", new Color(70, 130, 180));
        titlePanel.setPreferredSize(new Dimension(0, 60));
        frame.add(titlePanel, BorderLayout.NORTH);

        // Create a container to hold all demo panels
        Container demoContainer = new Container();
        demoContainer.setLayout(new GridLayout(2, 2, 10, 10));

        // Add BorderLayout demo
        demoContainer.add(createBorderLayoutDemo());

        // Add FlowLayout demo
        demoContainer.add(createFlowLayoutDemo());

        // Add GridLayout demo
        demoContainer.add(createGridLayoutDemo());

        // Add CardLayout demo (shows first card)
        demoContainer.add(createCardLayoutInfo());

        frame.add(demoContainer, BorderLayout.CENTER);

        frame.setSize(1000, 700);
        frame.setVisible(true);
    }

    private static Container createBorderLayoutDemo() {
        Container wrapper = new Container();
        wrapper.setLayout(new BorderLayout(2, 2));

        // Add title
        Container title = createColoredPanel("BorderLayout", new Color(100, 100, 100));
        title.setPreferredSize(new Dimension(0, 25));
        wrapper.add(title, BorderLayout.NORTH);

        // Create demo
        Container panel = new Container();
        panel.setLayout(new BorderLayout(3, 3));

        Container north = createColoredPanel("NORTH", new Color(255, 200, 200));
        north.setPreferredSize(new Dimension(0, 40));
        panel.add(north, BorderLayout.NORTH);

        Container south = createColoredPanel("SOUTH", new Color(200, 255, 200));
        south.setPreferredSize(new Dimension(0, 40));
        panel.add(south, BorderLayout.SOUTH);

        Container east = createColoredPanel("EAST", new Color(200, 200, 255));
        east.setPreferredSize(new Dimension(60, 0));
        panel.add(east, BorderLayout.EAST);

        Container west = createColoredPanel("WEST", new Color(255, 255, 200));
        west.setPreferredSize(new Dimension(60, 0));
        panel.add(west, BorderLayout.WEST);

        Container center = createColoredPanel("CENTER", new Color(220, 220, 220));
        panel.add(center, BorderLayout.CENTER);

        wrapper.add(panel, BorderLayout.CENTER);
        return wrapper;
    }

    private static Container createFlowLayoutDemo() {
        Container wrapper = new Container();
        wrapper.setLayout(new BorderLayout(2, 2));

        // Add title
        Container title = createColoredPanel("FlowLayout", new Color(100, 100, 100));
        title.setPreferredSize(new Dimension(0, 25));
        wrapper.add(title, BorderLayout.NORTH);

        // Create demo
        Container panel = new Container();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

        for (int i = 1; i <= 8; i++) {
            Container box = createColoredPanel("Box " + i, 
                new Color(150 + i * 10, 180, 200 + i * 5));
            box.setPreferredSize(new Dimension(80, 40));
            panel.add(box);
        }
        
        // Set a preferred size for the panel itself so it has space
        panel.setPreferredSize(new Dimension(200, 200));

        wrapper.add(panel, BorderLayout.CENTER);
        return wrapper;
    }

    private static Container createGridLayoutDemo() {
        Container wrapper = new Container();
        wrapper.setLayout(new BorderLayout(2, 2));

        // Add title
        Container title = createColoredPanel("GridLayout (3x3)", new Color(100, 100, 100));
        title.setPreferredSize(new Dimension(0, 25));
        wrapper.add(title, BorderLayout.NORTH);

        // Create demo
        Container panel = new Container();
        panel.setLayout(new GridLayout(3, 3, 3, 3));

        for (int i = 1; i <= 9; i++) {
            Container cell = createColoredPanel("" + i, 
                new Color(150 + i * 10, 180, 200 + i * 5));
            panel.add(cell);
        }

        wrapper.add(panel, BorderLayout.CENTER);
        return wrapper;
    }

    private static Container createCardLayoutInfo() {
        Container wrapper = new Container();
        wrapper.setLayout(new BorderLayout(2, 2));

        // Add title
        Container title = createColoredPanel("CardLayout", new Color(100, 100, 100));
        title.setPreferredSize(new Dimension(100, 25));
        wrapper.add(title, BorderLayout.NORTH);

        // Create demo with CardLayout
        CardLayout cardLayout = new CardLayout();
        Container panel = new Container() {
            @Override
            public void paint(Graphics g) {
                // Paint a background to see if the panel has size
                g.setColor(new Color(180, 180, 180));
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paint(g);
            }
        };
        panel.setLayout(cardLayout);

        // Add several cards with preferred sizes
        Container card1 = createColoredPanel("Card 1", new Color(255, 200, 200));
        card1.setPreferredSize(new Dimension(100, 100));
        panel.add(card1, "card1");
        
        Container card2 = createColoredPanel("Card 2", new Color(200, 255, 200));
        card2.setPreferredSize(new Dimension(100, 100));
        panel.add(card2, "card2");
        
        Container card3 = createColoredPanel("Card 3", new Color(200, 200, 255));
        card3.setPreferredSize(new Dimension(100, 100));
        panel.add(card3, "card3");

        // Show the first card
        cardLayout.first(panel);
        
        // Set a preferred size for the panel itself so it has space
        panel.setPreferredSize(new Dimension(200, 200));

        wrapper.add(panel, BorderLayout.CENTER);
        return wrapper;
    }

    private static Container createColoredPanel(String text, Color color) {
        return new Container() {
            @Override
            public void paint(Graphics g) {
                g.setColor(color);
                g.fillRect(0, 0, getWidth(), getHeight());

                // Draw border
                g.setColor(Color.BLACK);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

                // Draw text
                boolean isTitle = text.equals("BorderLayout") || text.equals("FlowLayout") || 
                                  text.equals("GridLayout (3x3)") || text.equals("CardLayout");
                g.setColor(isTitle ? Color.WHITE : Color.BLACK);
                g.setFont(new Font("SansSerif", Font.BOLD, 14));
                
                // Simple text positioning without FontMetrics for now
                g.drawString(text, 10, getHeight() / 2);

                super.paint(g);
            }
        };
    }
}
