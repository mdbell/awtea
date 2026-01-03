package me.mdbell.awtea.examples.guidemo;

import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * Demonstration panel for mouse wheel normalization.
 * Shows mouse wheel event details and scroll position.
 */
public class MouseWheelDemoPanel extends Panel implements MouseWheelListener {
    
    private int scrollPosition = 0;
    private int lastWheelRotation = 0;
    private double lastPreciseRotation = 0.0;
    private int lastDeltaMode = 0;
    private int eventCount = 0;
    
    public MouseWheelDemoPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        addMouseWheelListener(this);
        
        // Instructions label
        Label instructions = new Label("Scroll with your mouse wheel to test normalization");
        instructions.setAlignment(Label.CENTER);
        add(instructions, BorderLayout.NORTH);
    }
    
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        eventCount++;
        lastWheelRotation = e.getWheelRotation();
        lastPreciseRotation = e.getPreciseWheelRotation();
        // scrollType field contains the browser's deltaMode (0=PIXEL, 1=LINE, 2=PAGE)
        lastDeltaMode = e.getScrollType();
        
        // Update scroll position
        scrollPosition += e.getUnitsToScroll();
        
        // Repaint to show updated values
        repaint();
    }
    
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        
        int width = getWidth();
        int height = getHeight();
        
        // Draw background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        
        // Draw info text
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        int y = 60;
        int lineHeight = 25;
        
        g.drawString("Mouse Wheel Normalization Test", 20, y);
        y += lineHeight * 2;
        
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.drawString("Event Count: " + eventCount, 20, y);
        y += lineHeight;
        
        g.drawString("Last Wheel Rotation: " + lastWheelRotation, 20, y);
        y += lineHeight;
        
        g.drawString(String.format("Last Precise Rotation: %.2f", lastPreciseRotation), 20, y);
        y += lineHeight;
        
        g.drawString("Delta Mode: " + getDeltaModeString(lastDeltaMode), 20, y);
        y += lineHeight;
        
        g.drawString("Current Scroll Position: " + scrollPosition, 20, y);
        y += lineHeight * 2;
        
        // Draw current system property values
        g.setColor(new Color(0, 100, 0));
        g.drawString("Current Configuration:", 20, y);
        y += lineHeight;
        
        int pixelDivisor = Integer.getInteger("me.mdbell.awtea.mouseWheel.pixelDivisor", 100);
        g.drawString("  Pixel Divisor: " + pixelDivisor, 20, y);
        y += lineHeight;
        
        int lineDivisor = Integer.getInteger("me.mdbell.awtea.mouseWheel.lineDivisor", 3);
        g.drawString("  Line Divisor: " + lineDivisor, 20, y);
        y += lineHeight;
        
        int pageMultiplier = Integer.getInteger("me.mdbell.awtea.mouseWheel.pageMultiplier", 1);
        g.drawString("  Page Multiplier: " + pageMultiplier, 20, y);
        y += lineHeight * 2;
        
        // Visual scroll position indicator
        g.setColor(Color.BLUE);
        int barX = 20;
        int barY = y;
        int barWidth = width - 40;
        int barHeight = 40;
        
        // Background bar
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(barX, barY, barWidth, barHeight);
        
        // Position indicator (constrained to bar)
        g.setColor(Color.BLUE);
        int indicatorWidth = 10;
        int normalizedPos = scrollPosition % barWidth;
        if (normalizedPos < 0) normalizedPos += barWidth;
        g.fillRect(barX + normalizedPos, barY, indicatorWidth, barHeight);
        
        // Draw center line
        g.setColor(Color.RED);
        int centerX = barX + barWidth / 2;
        g.drawLine(centerX, barY, centerX, barY + barHeight);
        
        g.setColor(Color.BLACK);
        y += barHeight + 20;
        g.drawString("Blue indicator shows scroll position (red line = center)", 20, y);
    }
    
    private String getDeltaModeString(int deltaMode) {
        switch (deltaMode) {
            case 0: return "PIXEL (0)";
            case 1: return "LINE (1)";
            case 2: return "PAGE (2)";
            default: return "UNKNOWN (" + deltaMode + ")";
        }
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(600, 400);
    }
}
