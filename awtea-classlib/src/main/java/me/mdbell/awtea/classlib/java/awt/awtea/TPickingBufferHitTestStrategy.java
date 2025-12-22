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
 * This strategy uses dual-rendering to build a picking buffer where components
 * are rendered with their ID encoded as an RGB color. Hit-testing is then O(1)
 * by reading a single pixel from the picking buffer.
 * </p>
 * <p>
 * <b>Rendering Approach:</b>
 * The picking buffer is rebuilt when invalidated using a separate render pass:
 * <ol>
 *   <li>Enable picking mode on rasterizers</li>
 *   <li>For each component: set its ID, call paint() - renders to picking buffer with ID color</li>
 *   <li>Disable picking mode</li>
 *   <li>Normal rendering continues as usual with actual colors</li>
 * </ol>
 * </p>
 * <p>
 * This means components render normally during picking rebuild - they call setColor(),
 * fillRect(), etc. as usual. The rasterizer intercepts these operations and when
 * picking is enabled, it renders to the picking buffer using the component ID color
 * instead of the requested color.
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
 * <p>
 * The picking buffer is lazily rebuilt only when invalidated (e.g., layout changes).
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
     * This triggers a complete render pass with picking mode enabled.
     */
    private void rebuildPickingBuffer() {
        log.trace("Rebuilding picking buffer");
        
        // Begin picking pass
        pickingBuffer.beginPickingPass();
        
        // Get graphics context for rendering
        TGraphics g = rootContainer.getGraphics();
        if (g == null) {
            log.warn("Cannot rebuild picking buffer - no graphics context available");
            pickingBuffer.endPickingPass();
            return;
        }
        
        try {
            // Enable picking mode on the rasterizer
            setPickingEnabled(g, true);
            
            // Render the entire component tree
            // The paint() methods will call setActiveComponentId() for each component
            rootContainer.paint(g);
            
            // Disable picking mode
            setPickingEnabled(g, false);
            
        } finally {
            g.dispose();
            pickingBuffer.endPickingPass();
        }
        
        log.trace("Picking buffer rebuild complete");
    }
    
    /**
     * Enables or disables picking mode on the rasterizer.
     */
    private void setPickingEnabled(TGraphics g, boolean enabled) {
        if (g instanceof me.mdbell.awtea.classlib.java.awt.TSurfaceRasterizerGraphics) {
            me.mdbell.awtea.classlib.java.awt.TSurfaceRasterizerGraphics srg = 
                (me.mdbell.awtea.classlib.java.awt.TSurfaceRasterizerGraphics) g;
            
            // Use reflection to call setPickingEnabled on WebGLRasterizer
            try {
                Object rasterizer = srg.getClass().getDeclaredField("rasterizer").get(srg);
                if (rasterizer.getClass().getName().contains("WebGLRasterizer")) {
                    java.lang.reflect.Method method = rasterizer.getClass().getMethod("setPickingEnabled", boolean.class);
                    method.invoke(rasterizer, enabled);
                }
            } catch (Exception e) {
                log.trace("Could not set picking enabled: {}", e.getMessage());
            }
        }
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
