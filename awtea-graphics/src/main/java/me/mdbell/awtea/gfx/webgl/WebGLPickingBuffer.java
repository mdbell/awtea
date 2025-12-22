package me.mdbell.awtea.gfx.webgl;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.typedarrays.Uint8Array;
import org.teavm.jso.webgl.WebGL2RenderingContext;
import org.teavm.jso.webgl.WebGLFramebuffer;
import org.teavm.jso.webgl.WebGLRenderbuffer;
import org.teavm.jso.webgl.WebGLRenderingContext;
import org.teavm.jso.webgl.WebGLTexture;

/**
 * Manages the picking buffer for GPU-based component hit-testing.
 * <p>
 * The picking buffer is an off-screen framebuffer where components render themselves
 * with their unique ID encoded as an RGB color. This allows O(1) hit-testing by
 * reading a single pixel from the picking buffer.
 * </p>
 * <p>
 * Unlike traditional rectangular bounds testing, this approach automatically handles:
 * - Arbitrary component shapes (ovals, polygons, rounded rectangles, etc.)
 * - Pixel-perfect hit testing
 * - Proper z-ordering and overlapping components
 * - Component transforms and clipping
 * </p>
 */
public class WebGLPickingBuffer {
    
    private static final Logger log = LoggerFactory.getLogger(WebGLPickingBuffer.class);
    
    private final WebGL2RenderingContext gl;
    
    private WebGLFramebuffer framebuffer;
    private WebGLTexture colorTexture;
    private WebGLRenderbuffer depthBuffer;
    
    private int width;
    private int height;
    private boolean dirty = true;
    
    /**
     * Creates a new picking buffer.
     * 
     * @param gl the WebGL context
     * @param width the buffer width in pixels
     * @param height the buffer height in pixels
     */
    public WebGLPickingBuffer(WebGL2RenderingContext gl, int width, int height) {
        this.gl = gl;
        this.width = width;
        this.height = height;
        
        createFramebuffer();
    }
    
    /**
     * Creates the off-screen framebuffer and its attachments.
     */
    private void createFramebuffer() {
        // Create framebuffer
        framebuffer = gl.createFramebuffer();
        gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, framebuffer);
        
        // Create color texture attachment (RGB8 format)
        colorTexture = gl.createTexture();
        gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, colorTexture);
        gl.texImage2D(
            WebGLRenderingContext.TEXTURE_2D,
            0,
            WebGLRenderingContext.RGB,
            width,
            height,
            0,
            WebGLRenderingContext.RGB,
            WebGLRenderingContext.UNSIGNED_BYTE,
            (org.teavm.jso.typedarrays.ArrayBufferView) null
        );
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, 
            WebGLRenderingContext.TEXTURE_MIN_FILTER, 
            WebGLRenderingContext.NEAREST);
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, 
            WebGLRenderingContext.TEXTURE_MAG_FILTER, 
            WebGLRenderingContext.NEAREST);
        gl.framebufferTexture2D(
            WebGLRenderingContext.FRAMEBUFFER,
            WebGLRenderingContext.COLOR_ATTACHMENT0,
            WebGLRenderingContext.TEXTURE_2D,
            colorTexture,
            0
        );
        
        // Create depth buffer attachment (for proper z-ordering)
        depthBuffer = gl.createRenderbuffer();
        gl.bindRenderbuffer(WebGLRenderingContext.RENDERBUFFER, depthBuffer);
        gl.renderbufferStorage(
            WebGLRenderingContext.RENDERBUFFER,
            WebGLRenderingContext.DEPTH_COMPONENT16,
            width,
            height
        );
        gl.framebufferRenderbuffer(
            WebGLRenderingContext.FRAMEBUFFER,
            WebGLRenderingContext.DEPTH_ATTACHMENT,
            WebGLRenderingContext.RENDERBUFFER,
            depthBuffer
        );
        
        // Check framebuffer completeness
        int status = gl.checkFramebufferStatus(WebGLRenderingContext.FRAMEBUFFER);
        if (status != WebGLRenderingContext.FRAMEBUFFER_COMPLETE) {
            log.error("Picking buffer framebuffer is not complete: {}", status);
        }
        
        // Unbind framebuffer
        gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, null);
        
        log.debug("Created picking buffer: {}x{}", width, height);
    }
    
    /**
     * Gets the framebuffer for rendering the picking pass.
     */
    public WebGLFramebuffer getFramebuffer() {
        return framebuffer;
    }
    
    /**
     * Resizes the picking buffer.
     * 
     * @param newWidth the new width in pixels
     * @param newHeight the new height in pixels
     */
    public void resize(int newWidth, int newHeight) {
        if (newWidth == width && newHeight == height) {
            return;
        }
        
        log.debug("Resizing picking buffer: {}x{} -> {}x{}", width, height, newWidth, newHeight);
        
        // Delete old resources
        destroy();
        
        // Create new framebuffer with new size
        this.width = newWidth;
        this.height = newHeight;
        createFramebuffer();
        
        // Mark dirty to trigger rebuild
        dirty = true;
    }
    
    /**
     * Marks the picking buffer as dirty, requiring a rebuild.
     */
    public void invalidate() {
        dirty = true;
        log.trace("Picking buffer invalidated");
    }
    
    /**
     * Prepares the picking buffer for rendering (binds framebuffer and clears).
     */
    public void beginPickingPass() {
        // Bind picking framebuffer
        gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, framebuffer);
        
        // Set viewport
        gl.viewport(0, 0, width, height);
        
        // Clear to ID 0 (no component)
        gl.clearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl.clear(WebGLRenderingContext.COLOR_BUFFER_BIT | WebGLRenderingContext.DEPTH_BUFFER_BIT);
        
        dirty = false;
    }
    
    /**
     * Completes the picking pass (unbinds framebuffer).
     */
    public void endPickingPass() {
        // Unbind framebuffer
        gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, null);
    }
    
    /**
     * Reads the component ID at the specified pixel coordinates.
     * 
     * @param x the x coordinate (in buffer space)
     * @param y the y coordinate (in buffer space)
     * @return the component ID at the position, or 0 if none found
     */
    public int getComponentIdAt(int x, int y) {
        // Bounds check
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return 0;
        }
        
        // Bind picking framebuffer for reading
        gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, framebuffer);
        
        // Read pixel at (x, y)
        // Note: Y coordinate is already in WebGL space (flipped by rasterizer)
        Uint8Array pixel = Uint8Array.create(4);
        gl.readPixels(x, y, 1, 1, WebGLRenderingContext.RGBA, WebGLRenderingContext.UNSIGNED_BYTE, pixel);
        
        // Unbind framebuffer
        gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, null);
        
        // Decode component ID from pixel
        int[] pixelData = new int[4];
        for (int i = 0; i < 4; i++) {
            pixelData[i] = pixel.get(i);
        }
        
        int componentId = PickingColorEncoder.decodeIdFromPixel(pixelData);
        
        log.trace("Picking buffer read at ({}, {}) -> component ID {}", x, y, componentId);
        
        return componentId;
    }
    
    /**
     * Releases all GPU resources held by this picking buffer.
     */
    public void destroy() {
        if (framebuffer != null) {
            gl.deleteFramebuffer(framebuffer);
            framebuffer = null;
        }
        if (colorTexture != null) {
            gl.deleteTexture(colorTexture);
            colorTexture = null;
        }
        if (depthBuffer != null) {
            gl.deleteRenderbuffer(depthBuffer);
            depthBuffer = null;
        }
        
        log.debug("Picking buffer destroyed");
    }
    
    /**
     * Returns whether the picking buffer needs to be rebuilt.
     */
    public boolean isDirty() {
        return dirty;
    }
    
    /**
     * Gets the width of the picking buffer.
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Gets the height of the picking buffer.
     */
    public int getHeight() {
        return height;
    }
}
