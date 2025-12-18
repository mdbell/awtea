package me.mdbell.awtea.examples.guidemo;

import java.awt.*;
import java.awt.event.*;

/**
 * Demo to test synthesized mouse enter/exit events.
 * Shows different colored panels that change appearance when mouse enters/exits.
 */
public class MouseEnterExitDemo {

    static class HoverPanel extends Panel {
        private String label;
        private boolean hovered = false;
        private Color normalColor;
        private Color hoverColor;

        public HoverPanel(String label, Color normalColor, Color hoverColor) {
            this.label = label;
            this.normalColor = normalColor;
            this.hoverColor = hoverColor;
            
            setBackground(normalColor);
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    System.out.println("MOUSE_ENTERED: " + label + " at (" + e.getX() + ", " + e.getY() + ")");
                    hovered = true;
                    setBackground(hoverColor);
                    repaint();
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    System.out.println("MOUSE_EXITED: " + label + " at (" + e.getX() + ", " + e.getY() + ")");
                    hovered = false;
                    setBackground(normalColor);
                    repaint();
                }
            });
        }
        
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            
            g.setColor(hovered ? Color.WHITE : Color.BLACK);
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            
            FontMetrics fm = g.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(label)) / 2;
            int y = (getHeight() + fm.getAscent()) / 2;
            
            g.drawString(label, x, y);
            
            if (hovered) {
                g.drawString("HOVERED!", x - 20, y + 25);
            }
        }
    }

    public static void main(String[] args) {
        System.setProperty("me.mdbell.awtea.wasm.module_path", "/awtea-graphics/build/wasm/awt_raster.wasm");
        
        Frame frame = new Frame();
        frame.setTitle("Mouse Enter/Exit Event Demo");
        frame.setLayout(new GridLayout(2, 3, 10, 10));
        
        // Add several panels with different colors
        frame.add(new HoverPanel("Panel 1", new Color(200, 100, 100), new Color(255, 0, 0)));
        frame.add(new HoverPanel("Panel 2", new Color(100, 200, 100), new Color(0, 255, 0)));
        frame.add(new HoverPanel("Panel 3", new Color(100, 100, 200), new Color(0, 0, 255)));
        frame.add(new HoverPanel("Panel 4", new Color(200, 200, 100), new Color(255, 255, 0)));
        frame.add(new HoverPanel("Panel 5", new Color(200, 100, 200), new Color(255, 0, 255)));
        frame.add(new HoverPanel("Panel 6", new Color(100, 200, 200), new Color(0, 255, 255)));
        
        frame.setSize(800, 600);
        frame.setVisible(true);
        
        System.out.println("Mouse Enter/Exit Demo initialized. Move your mouse over the colored panels.");
    }
}
