package me.mdbell.awtea.examples.focusdemo;

import me.mdbell.awtea.classlib.java.applet.TApplet;
import me.mdbell.awtea.classlib.java.awt.*;
import me.mdbell.awtea.classlib.java.awt.event.TFocusEvent;
import me.mdbell.awtea.classlib.java.awt.event.TFocusListener;
import me.mdbell.awtea.classlib.java.awt.event.TKeyEvent;
import me.mdbell.awtea.classlib.java.awt.event.TKeyListener;

import java.awt.Color;
import org.teavm.classlib.java.awt.TDimension;

/**
 * Demo application showcasing the Focus Traversal System.
 * 
 * This demo demonstrates:
 * - TAB key navigation between focusable components
 * - Shift-TAB for backward traversal
 * - Visual focus indicators
 * - Focus event handling
 * - Custom focus traversal policies
 */
public class FocusDemo extends TApplet {

    private TPanel mainPanel;
    private TPanel infoPanel;
    private TLabel statusLabel;
    private TLabel instructionsLabel;
    private int focusChangeCount = 0;

    @Override
    public void init() {
        setLayout(new TBorderLayout(10, 10));
        setBackground(new Color(240, 240, 240));

        // Create title panel
        TPanel titlePanel = createTitlePanel();
        add(titlePanel, TBorderLayout.NORTH);

        // Create main form panel with focusable components
        mainPanel = createMainPanel();
        add(mainPanel, TBorderLayout.CENTER);

        // Create info panel
        infoPanel = createInfoPanel();
        add(infoPanel, TBorderLayout.SOUTH);
    }

    private TPanel createTitlePanel() {
        TPanel panel = new TPanel() {
            @Override
            public void paint(TGraphics g) {
                g.setColor(new Color(70, 130, 180)); // Steel blue
                g.fillRect(0, 0, getWidth(), getHeight());

                g.setColor(Color.WHITE);
                g.setFont(new TFont("SansSerif", TFont.BOLD, 24));
                g.drawString("Focus Traversal Demo", 10, 30);

                g.setFont(new TFont("SansSerif", TFont.PLAIN, 14));
                g.drawString("Press TAB to navigate forward, Shift-TAB to navigate backward", 10, 50);
            }
        };
        panel.setPreferredSize(new TDimension(800, 70));
        return panel;
    }

    private TPanel createMainPanel() {
        TPanel panel = new TPanel();
        panel.setLayout(new TGridLayout(5, 2, 10, 10));
        panel.setBackground(Color.WHITE);

        // Create a form with multiple focusable components
        panel.add(createLabel("Name:"));
        panel.add(createFocusableButton("Name Input"));

        panel.add(createLabel("Email:"));
        panel.add(createFocusableButton("Email Input"));

        panel.add(createLabel("Age:"));
        panel.add(createFocusableButton("Age Input"));

        panel.add(createLabel("Country:"));
        panel.add(createFocusableButton("Country Input"));

        panel.add(createLabel(""));
        TButton submitButton = createFocusableButton("Submit");
        submitButton.setBackground(new Color(70, 130, 180));
        submitButton.setForeground(Color.WHITE);
        panel.add(submitButton);

        return panel;
    }

    private TLabel createLabel(String text) {
        TLabel label = new TLabel(text);
        label.setForeground(Color.BLACK);
        label.setFont(new TFont("SansSerif", TFont.PLAIN, 14));
        return label;
    }

    private TButton createFocusableButton(String text) {
        TButton button = new TButton(text);
        button.setFont(new TFont("SansSerif", TFont.PLAIN, 14));

        // Add focus listener to show visual feedback and update status
        button.addFocusListener(new TFocusListener() {
            @Override
            public void focusGained(TFocusEvent e) {
                button.setBackground(new Color(255, 255, 200)); // Light yellow
                focusChangeCount++;
                updateStatus("Component focused: " + button.getLabel() + " (Focus change #" + focusChangeCount + ")");
            }

            @Override
            public void focusLost(TFocusEvent e) {
                button.setBackground(new Color(240, 240, 240)); // Light gray
            }
        });

        // Add key listener to show key events
        button.addKeyListener(new TKeyListener() {
            @Override
            public void keyPressed(TKeyEvent e) {
                if (e.getKeyCode() != TKeyEvent.VK_TAB) {
                    updateStatus("Key pressed on " + button.getLabel() + ": " + 
                               getKeyName(e.getKeyCode()));
                }
            }

            @Override
            public void keyReleased(TKeyEvent e) {
                // Not used in this demo
            }

            @Override
            public void keyTyped(TKeyEvent e) {
                // Not used in this demo
            }
        });

        return button;
    }

    private String getKeyName(int keyCode) {
        if (keyCode == TKeyEvent.VK_ENTER) return "ENTER";
        if (keyCode == TKeyEvent.VK_SPACE) return "SPACE";
        if (keyCode == TKeyEvent.VK_ESCAPE) return "ESC";
        if (keyCode >= TKeyEvent.VK_A && keyCode <= TKeyEvent.VK_Z) {
            return String.valueOf((char) keyCode);
        }
        return "Key " + keyCode;
    }

    private TPanel createInfoPanel() {
        TPanel panel = new TPanel();
        panel.setLayout(new TBorderLayout());
        panel.setBackground(new Color(250, 250, 250));

        // Instructions
        instructionsLabel = new TLabel();
        instructionsLabel.setFont(new TFont("SansSerif", TFont.BOLD, 12));
        instructionsLabel.setForeground(new Color(50, 50, 50));
        panel.add(instructionsLabel, TBorderLayout.NORTH);

        // Status
        statusLabel = new TLabel("Ready. Press TAB to start navigating.");
        statusLabel.setFont(new TFont("SansSerif", TFont.PLAIN, 12));
        statusLabel.setForeground(new Color(70, 70, 70));
        panel.add(statusLabel, TBorderLayout.CENTER);

        updateInstructions();

        panel.setPreferredSize(new TDimension(800, 50));
        return panel;
    }

    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.repaint();
        }
    }

    private void updateInstructions() {
        if (instructionsLabel != null) {
            TKeyboardFocusManager manager = TKeyboardFocusManager.getCurrentKeyboardFocusManager();
            TComponent focusOwner = manager.getFocusOwner();
            String focusInfo = focusOwner != null ? 
                "Current focus: " + getFocusOwnerName(focusOwner) : 
                "No component has focus";
            instructionsLabel.setText(focusInfo);
            instructionsLabel.repaint();
        }
    }

    private String getFocusOwnerName(TComponent component) {
        if (component instanceof TButton) {
            return ((TButton) component).getLabel();
        }
        return component.getClass().getSimpleName();
    }

    @Override
    public void start() {
        // Give initial focus to the first button
        TKeyboardFocusManager manager = TKeyboardFocusManager.getCurrentKeyboardFocusManager();
        TContainer root = mainPanel.getFocusCycleRootAncestor();
        if (root != null) {
            TFocusTraversalPolicy policy = root.getFocusTraversalPolicy();
            if (policy != null) {
                TComponent first = policy.getFirstComponent(root);
                if (first != null) {
                    first.requestFocus();
                }
            }
        }
    }

    public static void main(String[] args) {
        // Set system properties for optimal rendering
        System.setProperty("me.mdbell.awtea.wasm.module_path", "/awtea-graphics/build/wasm/awt_raster.wasm");
        System.setProperty("me.mdbell.awtea.font.subpixel", "true");
        System.setProperty("me.mdbell.awtea.font.supersample", "4");

        // Create and show the demo
        TFrame frame = new TFrame();
        frame.setTitle("Focus Traversal Demo - awtea");
        frame.setSize(800, 500);

        FocusDemo demo = new FocusDemo();
        demo.init();
        demo.setSize(800, 500);
        
        frame.add(demo);
        frame.setVisible(true);
        
        demo.start();
    }
}
