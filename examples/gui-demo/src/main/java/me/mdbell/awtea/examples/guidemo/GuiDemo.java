package me.mdbell.awtea.examples.guidemo;

import me.mdbell.awtea.util.logging.LogLevel;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Comprehensive GUI demo showcasing awtea features including:
 * - Multiple containers and components
 * - Interactive drawing canvas
 * - Graphics primitives
 * - Event handling
 * - Text rendering
 */
public class GuiDemo {

    private static final Logger log = LoggerFactory.getLogger(GuiDemo.class);

    public static void main(String[] args) {

        // LoggerFactory.setGlobalLevel(LogLevel.TRACE);
        System.setProperty("me.mdbell.awtea.wasm.module_path", "/awtea-graphics/build/wasm/awt_raster.wasm");
        // System.setProperty("me.mdbell.awtea.gfx.backend", "java");
        System.setProperty("me.mdbell.awtea.font.subpixel", "false");
        System.setProperty("me.mdbell.awtea.font.supersample", "4");

        // Create the main window
        Frame frame = new Frame();
        frame.setTitle("GUI Demo - awtea Example");
        frame.setLayout(new BorderLayout(10, 10));

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
        titlePanel.setPreferredSize(new Dimension(1050, 70));
        frame.add(titlePanel, BorderLayout.NORTH);

        // Create center container with left and right sections
        Container centerPanel = new Container();
        centerPanel.setLayout(new BorderLayout(10, 10));

        // Left side: canvas and info panel
        Container leftPanel = new Container();
        leftPanel.setLayout(new BorderLayout(0, 10));

        DrawingCanvas canvas = new DrawingCanvas();
        canvas.setPreferredSize(new Dimension(500, 540));

        // Add button panel to demonstrate TButton and TLabel
        Panel buttonPanel = new Panel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(new Color(250, 250, 250));

        Label buttonDemoLabel = new Label("Button Demo:", Label.CENTER);
        buttonDemoLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        buttonPanel.add(buttonDemoLabel);

        final Label statusLabel = new Label("Click a button to see action!", Label.CENTER);
        statusLabel.setForeground(new Color(0, 100, 0));

        Button button1 = new Button("Hello World");
        button1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                log.info("Mouse entered button: Hello World");
            }
            @Override
            public void mouseExited(MouseEvent e) {
                log.info("Mouse exited button: Hello World");
            }
        });
        button1.addActionListener(e -> {
            statusLabel.setText("Hello World button clicked!");
            System.out.println("Hello World button action!");
        });
        buttonPanel.add(button1);

        Button button2 = new Button("Clear Canvas");
        button2.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                log.info("Mouse entered button: Clear Canvas");
            }
            @Override
            public void mouseExited(MouseEvent e) {
                log.info("Mouse exited button: Clear Canvas");
            }
        });
        button2.addActionListener(e -> {
            canvas.clear();
            statusLabel.setText("Canvas cleared!");
            System.out.println("Clear canvas action!");
        });
        buttonPanel.add(button2);

        Button button3 = new Button("Change Color");
        button3.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                log.info("Mouse entered button: Change Color");
            }
            @Override
            public void mouseExited(MouseEvent e) {
                log.info("Mouse exited button: Change Color");
            }
        });
        button3.addActionListener(e -> {
            canvas.randomizeColor();
            statusLabel.setText("Color changed!");
            System.out.println("Color changed action!");
        });
        buttonPanel.add(button3);

        buttonPanel.add(statusLabel);

        leftPanel.add(buttonPanel, BorderLayout.NORTH);
        leftPanel.add(canvas, BorderLayout.CENTER);

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
                g.drawString("• Hover over panels below to see enter/exit events", 10, 70);
                g.drawString("• View graphics primitives and alpha blending on right", 10, 85);

                g.setFont(new Font("SansSerif", Font.ITALIC, 10));
                g.setColor(Color.GRAY);
                g.drawString("Powered by awtea - Java AWT for the Web", 10, 95);
            }
        };
        infoPanel.setPreferredSize(new Dimension(500, 110));
        
        // Add hover demo panel
        Panel hoverDemoPanel = new Panel();
        hoverDemoPanel.setLayout(new GridLayout(1, 3, 5, 0));
        hoverDemoPanel.setPreferredSize(new Dimension(500, 60));
        
        // Add hover panels to demonstrate mouse enter/exit events
        hoverDemoPanel.add(createHoverPanel("Hover 1", new Color(200, 100, 100), new Color(255, 0, 0)));
        hoverDemoPanel.add(createHoverPanel("Hover 2", new Color(100, 200, 100), new Color(0, 255, 0)));
        hoverDemoPanel.add(createHoverPanel("Hover 3", new Color(100, 100, 200), new Color(0, 0, 255)));
        
        // Create bottom panel to hold both info and hover panels
        Container bottomPanel = new Container();
        bottomPanel.setLayout(new BorderLayout(0, 5));
        bottomPanel.add(infoPanel, BorderLayout.NORTH);
        bottomPanel.add(hoverDemoPanel, BorderLayout.CENTER);
        
        leftPanel.add(bottomPanel, BorderLayout.SOUTH);

        centerPanel.add(leftPanel, BorderLayout.WEST);

        // Right side: demo panels
        Container rightPanel = new Container();
        rightPanel.setLayout(new GridLayout(1, 2, 10, 0));

        GraphicsDemoPanel demoPanel = new GraphicsDemoPanel();
        demoPanel.setPreferredSize(new Dimension(270, 660));
        rightPanel.add(demoPanel);

        AlphaBlendingDemoPanel alphaPanel = new AlphaBlendingDemoPanel();
        alphaPanel.setPreferredSize(new Dimension(220, 660));
        rightPanel.add(alphaPanel);

        centerPanel.add(rightPanel, BorderLayout.CENTER);

        frame.add(centerPanel, BorderLayout.CENTER);

        // Set size before making window visible
        frame.setSize(1050, 780);

        // Show the window
        frame.setVisible(true);
    }

    /**
     * Creates a hover panel that changes color when mouse enters/exits.
     */
    private static Panel createHoverPanel(String label, Color normalColor, Color hoverColor) {
        // Use an array to hold mutable state that can be accessed from both inner classes
        final boolean[] hoveredState = {false};
        
        Panel panel = new Panel() {
            @Override
            public void paint(Graphics g) {
                // Draw background
                g.setColor(hoveredState[0] ? hoverColor : normalColor);
                g.fillRect(0, 0, getWidth(), getHeight());
                
                // Draw label
                g.setColor(hoveredState[0] ? Color.WHITE : Color.BLACK);
                g.setFont(new Font("SansSerif", Font.BOLD, 12));
                
                FontMetrics fm = g.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(label)) / 2;
                int y = (getHeight() + fm.getAscent()) / 2 - 5;
                
                g.drawString(label, x, y);
                
                if (hoveredState[0]) {
                    g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                    String hoverText = "HOVER!";
                    int hx = (getWidth() - g.getFontMetrics().stringWidth(hoverText)) / 2;
                    g.drawString(hoverText, hx, y + 15);
                }
                
                // Draw border
                g.setColor(Color.DARK_GRAY);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
        };
        
        panel.setBackground(normalColor);
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                log.info("Mouse entered: {}", label);
                hoveredState[0] = true;
                panel.repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                log.info("Mouse exited: {}", label);
                hoveredState[0] = false;
                panel.repaint();
            }
        });
        
        return panel;
    }
}
