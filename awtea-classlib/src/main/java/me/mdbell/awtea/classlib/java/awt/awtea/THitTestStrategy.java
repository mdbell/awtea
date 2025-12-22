package me.mdbell.awtea.classlib.java.awt.awtea;

import me.mdbell.awtea.classlib.java.awt.TComponent;

/**
 * Strategy interface for component hit-testing.
 * <p>
 * This abstraction allows different hit-testing implementations to be used,
 * such as tree-walk (recursive depth-first search) or GPU-based picking buffer.
 * The strategy is selected at runtime based on system properties and backend availability.
 * </p>
 * 
 * @see TreeWalkHitTestStrategy
 */
public interface THitTestStrategy {
    
    /**
     * Finds the component at the specified coordinates.
     * 
     * @param x the x coordinate in the root container's coordinate space
     * @param y the y coordinate in the root container's coordinate space
     * @return the component at the specified coordinates, or null if none found
     */
    TComponent getComponentAt(int x, int y);
    
    /**
     * Notifies the strategy that the component hierarchy or layout has changed.
     * Implementations may use this to invalidate cached data (e.g., picking buffer).
     */
    void invalidate();
    
    /**
     * Releases any resources held by this strategy.
     * Should be called when the strategy is no longer needed.
     */
    void dispose();
}
