package me.mdbell.awtea.classlib.java.awt.awtea;

import me.mdbell.awtea.classlib.java.awt.TComponent;
import me.mdbell.awtea.classlib.java.awt.TContainer;
import me.mdbell.awtea.classlib.java.awt.TGraphics;
import me.mdbell.awtea.gfx.webgl.WebGLPickingBuffer;
import me.mdbell.awtea.gfx.webgl.WebGLSurfaceBackend;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

/**
 * GPU-based hit-testing strategy using an off-screen picking buffer.
 * <p>
 * This strategy uses continuous dual-rendering where all graphics operations
 * are automatically mirrored to both the screen buffer and the picking buffer.
 * Components are rendered with their ID encoded as an RGB color in the picking
 * buffer, enabling O(1) hit-testing by reading a single pixel.
 * </p>
 * <p>
 * <b>Rendering Approach:</b>
 * Picking mode is permanently enabled on all graphics contexts:
 * <ol>
 *   <li>When a component calls getGraphics(), its component ID is set on the graphics context</li>
 *   <li>All rendering operations (fillRect, drawImage, etc.) are automatically duplicated to the picking buffer</li>
 *   <li>The rasterizer renders with actual colors to the screen, and with ID colors to the picking buffer</li>
 * </ol>
 * </p>
 * <p>
 * This means the picking buffer is always in sync with what's actually rendered on screen,
 * regardless of whether rendering happens via paint() or direct getGraphics() calls.
 * </p>
 * <p>
 * <b>Automatic Shape Support:</b>
 * Unlike traditional rectangular bounds testing, this approach automatically handles:
 * <ul>
 *   <li>Arbitrary component shapes (ovals, polygons, rounded rectangles, etc.)</li>
 *   <li>Pixel-perfect hit testing</li>
 *   <li>Proper z-ordering and overlapping components</li>
 *   <li>Component transforms and clipping</li>
 * </ul>
 * </p>
 */
public class TPickingBufferHitTestStrategy implements THitTestStrategy {
    
    private static final Logger log = LoggerFactory.getLogger(TPickingBufferHitTestStrategy.class);
    
    private final TContainer rootContainer;
    private final WebGLSurfaceBackend backend;
    private final WebGLPickingBuffer pickingBuffer;
    
    /**
     * Creates a new picking buffer hit-test strategy.
     * Picking mode is automatically enabled when components call getGraphics().
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
        
        // Initialize picking buffer (clear it)
        pickingBuffer.beginPickingPass();
        pickingBuffer.endPickingPass();
        
        log.info("Initialized continuous picking buffer hit-test strategy ({}x{}) - picking enabled automatically on getGraphics()", width, height);
    }
    
    @Override
    public TComponent getComponentAt(int x, int y) {
        // Read component ID from picking buffer
        // No rebuild needed - picking buffer is continuously updated as components render
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
    
    @Override
    public void invalidate() {
        // With continuous picking, we don't need to invalidate the buffer
        // It's always up-to-date with what's rendered
        log.trace("Picking buffer invalidate called (no-op with continuous picking)");
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
        // Clear the resized buffer
        pickingBuffer.beginPickingPass();
        pickingBuffer.endPickingPass();
    }
}
