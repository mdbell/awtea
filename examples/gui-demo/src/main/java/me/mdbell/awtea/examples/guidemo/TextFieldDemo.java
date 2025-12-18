package me.mdbell.awtea.examples.guidemo;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Demo showcasing TTextField component functionality.
 * Demonstrates text input, editing, selection, keyboard shortcuts, and events.
 */
public class TextFieldDemo {

    private static Label outputLabel;
    private static TextField nameField;
    private static TextField emailField;
    private static TextField passwordField;
    private static TextField columnsField;

    public static void main(String[] args) {
        System.setProperty("me.mdbell.awtea.wasm.module_path", "/awtea-graphics/build/wasm/awt_raster.wasm");
        System.setProperty("me.mdbell.awtea.font.subpixel", "true");
        System.setProperty("me.mdbell.awtea.font.supersample", "4");

        // Create the main window
        Frame frame = new Frame();
        frame.setTitle("TextField Demo - awtea");
        frame.setLayout(new BorderLayout(10, 10));

        // Title panel
        Panel titlePanel = new Panel();
        titlePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        titlePanel.setBackground(new Color(70, 130, 180));
        
        Label titleLabel = new Label("TextField Demo", Label.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setPreferredSize(new Dimension(500, 40));
        titlePanel.add(titleLabel);
        
        frame.add(titlePanel, BorderLayout.NORTH);

        // Center panel with text fields
        Panel centerPanel = new Panel();
        centerPanel.setLayout(new GridLayout(5, 2, 10, 10));
        centerPanel.setBackground(Color.WHITE);

        // Name field
        Label nameLabel = new Label("Name:", Label.RIGHT);
        nameField = new TextField("John Doe", 20);
        nameField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                outputLabel.setText("Name entered: " + nameField.getText());
                System.out.println("Name field action: " + nameField.getText());
            }
        });
        centerPanel.add(nameLabel);
        centerPanel.add(nameField);

        // Email field
        Label emailLabel = new Label("Email:", Label.RIGHT);
        emailField = new TextField("user@example.com", 25);
        emailField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                outputLabel.setText("Email entered: " + emailField.getText());
                System.out.println("Email field action: " + emailField.getText());
            }
        });
        centerPanel.add(emailLabel);
        centerPanel.add(emailField);

        // Password field (TODO: echo char not yet implemented)
        Label passwordLabel = new Label("Password:", Label.RIGHT);
        passwordField = new TextField("", 20);
        passwordField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                outputLabel.setText("Password entered (length: " + passwordField.getText().length() + ")");
                System.out.println("Password field action");
            }
        });
        centerPanel.add(passwordLabel);
        centerPanel.add(passwordField);

        // Columns demo field
        Label columnsLabel = new Label("Custom Columns:", Label.RIGHT);
        columnsField = new TextField("", 15);
        columnsField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                outputLabel.setText("Columns field: " + columnsField.getText());
            }
        });
        centerPanel.add(columnsLabel);
        centerPanel.add(columnsField);

        // Read-only field
        Label readOnlyLabel = new Label("Read-only:", Label.RIGHT);
        TextField readOnlyField = new TextField("This is read-only text", 25);
        readOnlyField.setEditable(false);
        readOnlyField.setBackground(new Color(240, 240, 240));
        centerPanel.add(readOnlyLabel);
        centerPanel.add(readOnlyField);

        frame.add(centerPanel, BorderLayout.CENTER);

        // Button panel
        Panel buttonPanel = new Panel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(Color.WHITE);

        Button submitButton = new Button("Submit");
        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String name = nameField.getText();
                String email = emailField.getText();
                String output = "Submitted: Name='" + name + "', Email='" + email + "'";
                outputLabel.setText(output);
                System.out.println(output);
            }
        });
        buttonPanel.add(submitButton);

        Button clearButton = new Button("Clear All");
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nameField.setText("");
                emailField.setText("");
                passwordField.setText("");
                columnsField.setText("");
                outputLabel.setText("All fields cleared");
                System.out.println("All fields cleared");
            }
        });
        buttonPanel.add(clearButton);

        Button selectAllButton = new Button("Select Name");
        selectAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nameField.selectAll();
                nameField.requestFocus();
                outputLabel.setText("Name field text selected");
            }
        });
        buttonPanel.add(selectAllButton);

        Button fillButton = new Button("Fill Demo Data");
        fillButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nameField.setText("Alice Smith");
                emailField.setText("alice@example.org");
                passwordField.setText("secret123");
                columnsField.setText("15 columns wide");
                outputLabel.setText("Demo data filled");
            }
        });
        buttonPanel.add(fillButton);

        frame.add(buttonPanel, BorderLayout.CENTER);

        // Output/status panel at the bottom
        Panel statusPanel = new Panel();
        statusPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        statusPanel.setBackground(new Color(240, 240, 240));
        
        outputLabel = new Label("Ready - Type in fields or press Enter to trigger action", Label.CENTER);
        outputLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        outputLabel.setPreferredSize(new Dimension(600, 30));
        statusPanel.add(outputLabel);
        
        frame.add(statusPanel, BorderLayout.SOUTH);

        // Instructions panel
        Panel instructionsPanel = new Panel();
        instructionsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        instructionsPanel.setBackground(new Color(255, 255, 220));
        
        Label instructionsLabel = new Label(
            "   Keyboard shortcuts: Ctrl+A (select all), Ctrl+C/V/X (copy/paste/cut), Arrow keys, Home/End   ");
        instructionsLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        instructionsPanel.add(instructionsLabel);
        
        frame.add(instructionsPanel, BorderLayout.SOUTH);

        // Set size and show the window
        frame.setSize(700, 400);
        frame.setVisible(true);

        // Set initial focus to name field
        nameField.requestFocus();
    }
}
