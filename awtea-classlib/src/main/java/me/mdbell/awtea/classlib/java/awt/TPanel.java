package me.mdbell.awtea.classlib.java.awt;

import java.awt.Color;

/**
 * {@code TPanel} is the simplest container class. A panel
 * provides space in which an application can attach any other
 * component, including other panels.
 * <p>
 * The default layout manager for a panel is the {@code TFlowLayout}
 * layout manager.
 * This is the awtea implementation of java.awt.Panel.
 *
 * @see TFlowLayout
 * @see java.awt.Panel
 */
public class TPanel extends TContainer {

    /**
     * Creates a new panel using the default layout manager.
     * The default layout manager for all panels is the {@code TFlowLayout} class.
     */
    public TPanel() {
        this(new TFlowLayout());
    }

    /**
     * Creates a new panel with the specified layout manager.
     *
     * @param layout the layout manager for this panel
     */
    public TPanel(TLayoutManager layout) {
        setLayout(layout);
    }

    /**
     * Paints the panel. First paints the background if set, then paints children.
     *
     * @param g the graphics context
     */
    @Override
    public void paint(TGraphics g) {
        if (g == null) {
            return;
        }
        
        // Paint background if set
        Color bg = getBackground();
        if (bg != null) {
            g.setColor(bg);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        
        // Paint children
        super.paint(g);
    }
}
