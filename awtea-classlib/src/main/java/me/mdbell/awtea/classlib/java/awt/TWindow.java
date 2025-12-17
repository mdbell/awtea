package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.event.TWindowListener;

import java.awt.*;

/**
 * A {@code TWindow} object is a top-level window with no borders and no menubar.
 * The default layout for a window is {@code BorderLayout}.
 * <p>
 * A window must have either a frame, dialog, or another window defined as its
 * owner when it's constructed.
 * <p>
 * In awtea, TWindow serves as the base class for all top-level heavyweight windows
 * (TFrame, and future TDialog implementations).
 * <p>
 * This class is abstract because it requires subclasses to provide the surface graphics
 * implementation through their specific peer implementations.
 *
 * @see java.awt.Window
 * @see TFrame
 */
public abstract class TWindow extends TSurface {

    private static final Color WINDOW_BACKGROUND_COLOR = new Color(223, 223, 223);

    /**
     * Constructs a new, initially invisible window.
     * The window is created with BorderLayout as the default layout manager.
     */
    public TWindow() {
        this.setBackground(WINDOW_BACKGROUND_COLOR);
        // Windows use BorderLayout by default in AWT
        setLayout(new TBorderLayout());
    }

    /**
     * Causes this Window to be sized to fit the preferred size and layouts of its subcomponents.
     * The resulting width and height of the window are automatically enlarged if either of dimensions
     * is less than the minimum size as specified by previous call to {@code setMinimumSize}.
     * <p>
     * If the window and/or its owner are not displayable yet, both of them are made displayable
     * before calculating the preferred size. The Window is validated after its size is being calculated.
     * <p>
     * This implementation properly uses the layout manager to compute the preferred size,
     * adds the window's insets, and then sets the window's size accordingly.
     */
    public void pack() {
        // Ensure the window is validated before packing
        validate();
        
        // Get the preferred size from the layout manager
        TDimension preferredSize = getPreferredLayoutSize();
        
        // Get the insets (borders, title bar, etc.) - subclasses may override
        TInsets insets = getInsets();
        
        // Add insets to the preferred size to get the final window size
        int width = preferredSize.width + insets.left + insets.right;
        int height = preferredSize.height + insets.top + insets.bottom;
        
        // Ensure minimum size constraints are met (if any were set)
        // For now, we ensure at least 1x1 to avoid zero-sized windows
        width = Math.max(width, 1);
        height = Math.max(height, 1);
        
        // Set the window size
        setSize(width, height);
        
        // Validate the layout with the new size
        validate();
    }

    /**
     * Adds the specified window listener to receive window events from this window.
     * If the listener is null, no exception is thrown and no action is performed.
     *
     * @param l the window listener to add
     */
    public void addWindowListener(TWindowListener l) {
        // TODO: Implement event listener logic
        // This will be implemented when the event system is fully set up
    }

    /**
     * If this Window is visible, brings this Window to the front and may make it the focused Window.
     * <p>
     * Places this Window at the top of the stacking order and shows it in front of any other Windows
     * in this application.
     */
    public void toFront() {
        // Default implementation - subclasses with peers should override
    }

    /**
     * Shows or hides this Window depending on the value of parameter {@code b}.
     * <p>
     * If this window is not yet displayable, it is made displayable. The window is validated
     * prior to being made visible.
     *
     * @param b if {@code true}, makes the Window visible, otherwise hides the Window
     */
    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        // Subclasses with peers should override to also notify the peer
    }

    /**
     * Sets the size of this component to the specified width and height.
     * <p>
     * For windows, this method delegates to the superclass (TSurface) to handle
     * the actual size change. Subclasses like TFrame should override to add
     * invalidation and validation as needed.
     *
     * @param width  the new width of this component in pixels
     * @param height the new height of this component in pixels
     */
    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
    }

    /**
     * Sets the minimum size of this window.
     * <p>
     * Subsequent calls to {@code pack()} will ensure the window is at least this size.
     * If the minimum size is not set, the window can be sized arbitrarily small.
     *
     * @param dim the new minimum size of this window
     */
    public void setMinimumSize(Dimension dim) {
        // TODO: Store and enforce minimum size
        // For now, this is a no-op to maintain compatibility
    }

    /**
     * Adds the specified component listener to receive component events from this component.
     * If the listener is null, no exception is thrown and no action is performed.
     *
     * @param l the component listener to add
     */
    public void addComponentListener(java.awt.event.ComponentListener l) {
        // TODO: Implement component listener logic
        // This will be implemented when the event system is fully set up
    }

    /**
     * Determines the insets of this window, which indicate the size of the window's border.
     * <p>
     * The default implementation returns zero insets. Subclasses like TFrame may override
     * this to account for title bars, borders, and other decorations.
     *
     * @return the insets of this window
     */
    @Override
    public TInsets getInsets() {
        return new TInsets(0, 0, 0, 0);
    }
}
