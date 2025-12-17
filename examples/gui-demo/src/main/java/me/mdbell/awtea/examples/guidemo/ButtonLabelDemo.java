package me.mdbell.awtea.examples.guidemo;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Simple demo showcasing TButton and TLabel components.
 * This demonstrates the basic UI components working in the browser.
 */
public class ButtonLabelDemo {

    private static Label statusLabel;
    private static int clickCount = 0;

    public static void main(String[] args) {
        System.setProperty("me.mdbell.awtea.wasm.module_path", "/awtea-graphics/build/wasm/awt_raster.wasm");
        System.setProperty("me.mdbell.awtea.font.subpixel", "true");
        System.setProperty("me.mdbell.awtea.font.supersample", "4");

        // Create the main window
        Frame frame = new Frame();
        frame.setTitle("Button and Label Demo - awtea");
        frame.setLayout(new BorderLayout(10, 10));

        // Title label at the top
        Panel titlePanel = new Panel();
        titlePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        titlePanel.setBackground(new Color(70, 130, 180));
        
        Label titleLabel = new Label("Button and Label Demo", Label.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setPreferredSize(new Dimension(400, 40));
        titlePanel.add(titleLabel);
        
        frame.add(titlePanel, BorderLayout.NORTH);

        // Center panel with buttons and labels
        Panel centerPanel = new Panel();
        centerPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20));
        centerPanel.setBackground(Color.WHITE);

        // Add some labels with different alignments
        Label leftLabel = new Label("Left Aligned", Label.LEFT);
        leftLabel.setPreferredSize(new Dimension(150, 25));
        leftLabel.setForeground(Color.BLUE);
        centerPanel.add(leftLabel);

        Label centerLabel = new Label("Center Aligned", Label.CENTER);
        centerLabel.setPreferredSize(new Dimension(150, 25));
        centerLabel.setForeground(new Color(0, 128, 0));
        centerPanel.add(centerLabel);

        Label rightLabel = new Label("Right Aligned", Label.RIGHT);
        rightLabel.setPreferredSize(new Dimension(150, 25));
        rightLabel.setForeground(new Color(255, 0, 0));
        centerPanel.add(rightLabel);

        // Add some buttons
        Button button1 = new Button("Click Me!");
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clickCount++;
                statusLabel.setText("Button clicked " + clickCount + " time" + (clickCount != 1 ? "s" : ""));
                System.out.println("Button 1 clicked! Count: " + clickCount);
            }
        });
        centerPanel.add(button1);

        Button button2 = new Button("Reset Counter");
        button2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clickCount = 0;
                statusLabel.setText("Counter reset!");
                System.out.println("Counter reset!");
            }
        });
        centerPanel.add(button2);

        Button button3 = new Button("Change Title");
        button3.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                titleLabel.setText("Title Changed!");
                System.out.println("Title changed!");
            }
        });
        centerPanel.add(button3);

        frame.add(centerPanel, BorderLayout.CENTER);

        // Status label at the bottom
        Panel statusPanel = new Panel();
        statusPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        statusPanel.setBackground(new Color(240, 240, 240));
        
        statusLabel = new Label("Ready - Click a button!", Label.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        statusLabel.setPreferredSize(new Dimension(400, 30));
        statusPanel.add(statusLabel);
        
        frame.add(statusPanel, BorderLayout.SOUTH);

        // Set size and show the window
        frame.setSize(600, 400);
        frame.setVisible(true);
    }
}
