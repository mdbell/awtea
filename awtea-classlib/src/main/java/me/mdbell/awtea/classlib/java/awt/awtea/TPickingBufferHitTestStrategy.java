package me.mdbell.awtea.classlib.java.awt.awtea;

import me.mdbell.awtea.classlib.java.awt.TComponent;
import me.mdbell.awtea.classlib.java.awt.TContainer;
import me.mdbell.awtea.gfx.webgl.WebGLPickingBuffer;
import me.mdbell.awtea.gfx.webgl.WebGLSurfaceBackend;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

/**
 * GPU-based hit-testing strategy using an off-screen picking buffer.
 * <p>
 * This strategy uses the WebGL rasterizer's dual-rendering capability to render
 * all components to both the normal framebuffer AND a picking buffer. In the picking
 * buffer, each component is rendered with a unique color encoding its ID, allowing
 * O(1) hit-testing by reading a single pixel.
 * </p>
 * <p>
 * Unlike traditional rectangular bounds testing, this approach automatically handles:
 * <ul>
 *   <li>Arbitrary component shapes (ovals, polygons, rounded rectangles, etc.)</li>
 *   <li>Pixel-perfect hit testing</li>
 *   <li>Proper z-ordering and overlapping components</li>
 *   <li>Component transforms and clipping</li>
 * </ul>
 * </p>
 * <p>
 * The picking buffer is lazily rebuilt when invalidated (e.g., due to layout changes).
 * Rebuilding happens by re-rendering all components with picking mode enabled.
 * </p>
 */
public class TPickingBufferHitTestStrategy implements THitTestStrategy {
    
    private static final Logger log = LoggerFactory.getLogger(TPickingBufferHitTestStrategy.class);
    
    private final TContainer rootContainer;
    private final WebGLSurfaceBackend backend;
    private final WebGLPickingBuffer pickingBuffer;
    
    /**
     * Creates a new picking buffer hit-test strategy.
     * 
     * @param backend the WebGL backend
     * @param rootContainer the root container to test against
     * @param width the canvas width in pixels
     * @param height the canvas height in pixels
     */
    public TPickingBufferHitTestStrategy(
            WebGLSurfaceBackend backend,
            TContainer rootContainer, 
            int width, 
            int height) {
        this.backend = backend;
        this.rootContainer = rootContainer;
        
        // Create picking buffer in the backend
        backend.createPickingBuffer(width, height);
        this.pickingBuffer = backend.getPickingBuffer();
        
        log.info("Initialized picking buffer hit-test strategy ({}x{})", width, height);
    }
    
    @Override
    public TComponent getComponentAt(int x, int y) {
        // Rebuild picking buffer if dirty
        if (pickingBuffer.isDirty()) {
            rebuildPickingBuffer();
        }
        
        // Read component ID from picking buffer
        int componentId = pickingBuffer.getComponentIdAt(x, y);
        
        // Look up component by ID
        TComponent component = TComponent.getComponentById(componentId);
        
        // Fallback to root container if no component found
        if (component == null) {
            component = rootContainer;
        }
        
        log.trace("Picking buffer hit-test at ({}, {}) -> {} (ID: {})", 
            x, y, component.getClass().getSimpleName(), componentId);
        
        return component;
    }
    
    /**
     * Rebuilds the picking buffer by re-rendering the component tree.
     * This is done by triggering a repaint with picking mode enabled.
     */
    private void rebuildPickingBuffer() {
        log.trace("Rebuilding picking buffer");
        
        // Begin picking pass
        pickingBuffer.beginPickingPass();
        
        // TODO: Trigger component tree re-render with picking enabled
        // This will be integrated with TContainer.paint() to call
        // rasterizer.pushComponentId() / popComponentId() / setPickingEnabled()
        // For now, this is a placeholder - actual integration happens in TContainer
        
        // End picking pass
        pickingBuffer.endPickingPass();
        
        log.trace("Picking buffer rebuild complete");
    }
    
    @Override
    public void invalidate() {
        pickingBuffer.invalidate();
        log.trace("Picking buffer invalidated");
    }
    
    @Override
    public void dispose() {
        backend.destroyPickingBuffer();
        log.debug("Picking buffer hit-test strategy disposed");
    }
    
    /**
     * Resizes the picking buffer to match a new canvas size.
     * 
     * @param width the new width in pixels
     * @param height the new height in pixels
     */
    public void resize(int width, int height) {
        pickingBuffer.resize(width, height);
    }
}
