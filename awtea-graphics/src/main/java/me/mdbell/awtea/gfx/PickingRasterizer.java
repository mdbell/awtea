package me.mdbell.awtea.gfx;

/**
 * Interface for rasterizers that support GPU-based picking.
 * <p>
 * This interface allows the graphics layer to communicate picking-related
 * information to the rasterizer without creating circular dependencies
 * between modules.
 * </p>
 */
public interface PickingRasterizer {
    
    /**
     * Sets the active component ID for picking buffer rendering.
     * When picking is enabled, all subsequent rendering operations will
     * be rendered to the picking buffer with this component's ID color.
     * 
     * @param componentId the component ID
     */
    void setActiveComponentId(int componentId);
    
    /**
     * Enables or disables picking buffer rendering mode.
     * When enabled, all rendering operations are duplicated to the picking buffer.
     * 
     * @param enabled true to enable picking mode
     */
    void setPickingEnabled(boolean enabled);
    
    /**
     * Returns whether picking mode is currently enabled.
     * 
     * @return true if picking is enabled
     */
    boolean isPickingEnabled();
}
