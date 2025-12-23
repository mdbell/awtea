package me.mdbell.awtea.examples.focusdemo;

import me.mdbell.awtea.util.StubAppletStub;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

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
public class FocusDemo extends Applet {

    private Panel mainPanel;
    private Panel infoPanel;
    private Label statusLabel;
    private Label instructionsLabel;
    private int focusChangeCount = 0;

    @Override
    public void init() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(240, 240, 240));

        // Create title panel
        Panel titlePanel = createTitlePanel();
        add(titlePanel, BorderLayout.NORTH);

        // Create main form panel with focusable components
        mainPanel = createMainPanel();
        add(mainPanel, BorderLayout.CENTER);

        // Create info panel
        infoPanel = createInfoPanel();
        add(infoPanel, BorderLayout.SOUTH);
    }

    private Panel createTitlePanel() {
        Panel panel = new Panel() {
            @Override
            public void paint(Graphics g) {

                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, 24));
                g.drawString("Focus Traversal Demo", 10, 30);

                g.setFont(new Font("SansSerif", Font.PLAIN, 14));
                g.drawString("Press TAB to navigate forward, Shift-TAB to navigate backward", 10, 50);
            }
        };
        panel.setBackground(new Color(70, 130, 180));
        panel.setPreferredSize(new Dimension(800, 70));
        return panel;
    }

    private Panel createMainPanel() {
        Panel panel = new Panel();
        panel.setLayout(new GridLayout(5, 2, 10, 10));
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
        Button submitButton = createFocusableButton("Submit");
        submitButton.setBackground(new Color(70, 130, 180));
        submitButton.setForeground(Color.WHITE);
        panel.add(submitButton);

        return panel;
    }

    private Label createLabel(String text) {
        Label label = new Label(text);
        label.setForeground(Color.BLACK);
        label.setFont(new Font("SansSerif", Font.PLAIN, 14));
        return label;
    }

    private Button createFocusableButton(String text) {
        Button button = new Button(text);
        button.setFont(new Font("SansSerif", Font.PLAIN, 14));

        // Add focus listener to show visual feedback and update status
        button.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                button.setBackground(new Color(255, 255, 200)); // Light yellow
                focusChangeCount++;
                updateStatus("Component focused: " + button.getLabel() + " (Focus change #" + focusChangeCount + ")");
                updateInstructions(); // Update instructions to show current focus
            }

            @Override
            public void focusLost(FocusEvent e) {
                button.setBackground(new Color(240, 240, 240)); // Light gray
            }
        });

        // Add key listener to show key events
        button.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_TAB) {
                    updateStatus("Key pressed on " + button.getLabel() + ": " +
                            getKeyName(e.getKeyCode()));
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                // Not used in this demo
            }

            @Override
            public void keyTyped(KeyEvent e) {
                // Not used in this demo
            }
        });

        return button;
    }

    private String getKeyName(int keyCode) {
        if (keyCode == KeyEvent.VK_ENTER)
            return "ENTER";
        if (keyCode == KeyEvent.VK_SPACE)
            return "SPACE";
        if (keyCode == KeyEvent.VK_ESCAPE)
            return "ESC";
        if (keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z) {
            return String.valueOf((char) keyCode);
        }
        return "Key " + keyCode;
    }

    private Panel createInfoPanel() {
        Panel panel = new Panel();
        panel.setLayout(new BorderLayout());
        panel.setBackground(new Color(250, 250, 250));

        // Instructions
        instructionsLabel = new Label();
        instructionsLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        instructionsLabel.setForeground(new Color(50, 50, 50));
        panel.add(instructionsLabel, BorderLayout.NORTH);

        // Status
        statusLabel = new Label("Ready. Press TAB to start navigating.");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(70, 70, 70));
        panel.add(statusLabel, BorderLayout.CENTER);

        updateInstructions();

        panel.setPreferredSize(new Dimension(800, 50));
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
            KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            Component focusOwner = manager.getFocusOwner();
            String focusInfo = focusOwner != null ? "Current focus: " + getFocusOwnerName(focusOwner)
                    : "No component has focus";
            instructionsLabel.setText(focusInfo);
            instructionsLabel.repaint();
        }
    }

    private String getFocusOwnerName(Component component) {
        if (component instanceof Button) {
            return ((Button) component).getLabel();
        }
        return component.getClass().getSimpleName();
    }

    @Override
    public void start() {
        // Give initial focus to the first button
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        Container root = mainPanel.getFocusCycleRootAncestor();
        if (root != null) {
            FocusTraversalPolicy policy = root.getFocusTraversalPolicy();
            if (policy != null) {
                Component first = policy.getFirstComponent(root);
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

        System.setProperty("me.mdbell.awtea.log.level", "debug");

        System.setProperty("me.mdbell.awtea.classlib.java.awt.Applet.canvasId", args[0]);
        FocusDemo demo = new FocusDemo();

        demo.setStub(new StubAppletStub());

        demo.init();
        demo.setSize(800, 500);

        demo.validate();

        demo.start();
    }
}
