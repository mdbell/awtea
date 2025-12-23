package me.mdbell.awtea.gfx.webgl;

import org.teavm.jso.typedarrays.ArrayBufferView;
import org.teavm.jso.webgl.WebGL2RenderingContext;
import org.teavm.jso.webgl.WebGLFramebuffer;
import org.teavm.jso.webgl.WebGLRenderingContext;
import org.teavm.jso.webgl.WebGLTexture;

/**
 * Represents an offscreen render target (FBO) for post-processing effects.
 * A render target encapsulates a framebuffer object (FBO) and its associated texture.
 * 
 * <p>Render targets can be used for multi-pass rendering, post-processing effects,
 * and offscreen rendering. They support efficient resource pooling and reuse.</p>
 * 
 * <p>Example usage:
 * <pre>
 * // Create a render target
 * RenderTarget target = new RenderTarget(gl, 800, 600);
 * 
 * // Bind for rendering
 * target.bind();
 * // ... render to target ...
 * target.unbind();
 * 
 * // Use as texture input for next pass
 * gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, target.getTexture());
 * 
 * // Clean up
 * target.destroy();
 * </pre>
 * </p>
 */
public class RenderTarget {
    
    private final WebGL2RenderingContext gl;
    private final int width;
    private final int height;
    private final WebGLFramebuffer framebuffer;
    private final WebGLTexture texture;
    private boolean destroyed = false;
    
    /**
     * Creates a new render target with default texture parameters.
     * 
     * @param gl the WebGL rendering context
     * @param width the width of the render target in pixels
     * @param height the height of the render target in pixels
     */
    public RenderTarget(WebGL2RenderingContext gl, int width, int height) {
        this(gl, width, height, WebGLRenderingContext.LINEAR, WebGLRenderingContext.CLAMP_TO_EDGE);
    }
    
    /**
     * Creates a new render target with custom texture parameters.
     * 
     * @param gl the WebGL rendering context
     * @param width the width of the render target in pixels
     * @param height the height of the render target in pixels
     * @param filter the texture filtering mode (LINEAR or NEAREST)
     * @param wrap the texture wrap mode (CLAMP_TO_EDGE or REPEAT)
     */
    public RenderTarget(WebGL2RenderingContext gl, int width, int height, int filter, int wrap) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height must be positive");
        }
        
        this.gl = gl;
        this.width = width;
        this.height = height;
        
        // Create texture
        this.texture = gl.createTexture();
        gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, texture);
        gl.texImage2D(
            WebGLRenderingContext.TEXTURE_2D, 
            0, 
            WebGLRenderingContext.RGBA,
            width, 
            height, 
            0, 
            WebGLRenderingContext.RGBA,
            WebGLRenderingContext.UNSIGNED_BYTE, 
            (ArrayBufferView) null
        );
        
        // Set texture parameters
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_MIN_FILTER, filter);
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_MAG_FILTER, filter);
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_WRAP_S, wrap);
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_WRAP_T, wrap);
        
        // Create framebuffer and attach texture
        this.framebuffer = gl.createFramebuffer();
        gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, framebuffer);
        gl.framebufferTexture2D(
            WebGLRenderingContext.FRAMEBUFFER,
            WebGLRenderingContext.COLOR_ATTACHMENT0,
            WebGLRenderingContext.TEXTURE_2D,
            texture,
            0
        );
        
        // Check framebuffer completeness
        int status = gl.checkFramebufferStatus(WebGLRenderingContext.FRAMEBUFFER);
        if (status != WebGLRenderingContext.FRAMEBUFFER_COMPLETE) {
            destroy();
            throw new RuntimeException("Framebuffer is not complete: " + status);
        }
        
        // Unbind
        gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, null);
        gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, null);
    }
    
    /**
     * Binds this render target for rendering.
     * All subsequent draw calls will render to this target until {@link #unbind()} is called.
     */
    public void bind() {
        checkNotDestroyed();
        gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, framebuffer);
        gl.viewport(0, 0, width, height);
    }
    
    /**
     * Unbinds this render target, restoring rendering to the default framebuffer.
     */
    public void unbind() {
        gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, null);
    }
    
    /**
     * Clears this render target with the specified color.
     * The target must be bound before calling this method.
     * 
     * @param r red component (0.0-1.0)
     * @param g green component (0.0-1.0)
     * @param b blue component (0.0-1.0)
     * @param a alpha component (0.0-1.0)
     */
    public void clear(float r, float g, float b, float a) {
        checkNotDestroyed();
        gl.clearColor(r, g, b, a);
        gl.clear(WebGLRenderingContext.COLOR_BUFFER_BIT);
    }
    
    /**
     * Gets the texture associated with this render target.
     * This texture contains the rendered output and can be used as input for subsequent passes.
     * 
     * @return the WebGL texture
     */
    public WebGLTexture getTexture() {
        checkNotDestroyed();
        return texture;
    }
    
    /**
     * Gets the framebuffer object.
     * 
     * @return the WebGL framebuffer
     */
    public WebGLFramebuffer getFramebuffer() {
        checkNotDestroyed();
        return framebuffer;
    }
    
    /**
     * Gets the width of this render target in pixels.
     * 
     * @return the width
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Gets the height of this render target in pixels.
     * 
     * @return the height
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Checks if this render target has been destroyed.
     * 
     * @return true if destroyed
     */
    public boolean isDestroyed() {
        return destroyed;
    }
    
    /**
     * Destroys this render target and releases GPU resources.
     * After calling this method, the render target cannot be used.
     */
    public void destroy() {
        if (!destroyed) {
            if (texture != null) {
                gl.deleteTexture(texture);
            }
            if (framebuffer != null) {
                gl.deleteFramebuffer(framebuffer);
            }
            destroyed = true;
        }
    }
    
    private void checkNotDestroyed() {
        if (destroyed) {
            throw new IllegalStateException("RenderTarget has been destroyed");
        }
    }
    
    /**
     * Resizes this render target to new dimensions.
     * This will recreate the texture with the new size.
     * 
     * @param newWidth the new width
     * @param newHeight the new height
     */
    public void resize(int newWidth, int newHeight) {
        checkNotDestroyed();
        
        if (newWidth == width && newHeight == height) {
            return;
        }
        
        if (newWidth <= 0 || newHeight <= 0) {
            throw new IllegalArgumentException("Width and height must be positive");
        }
        
        // Recreate texture with new size
        gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, texture);
        gl.texImage2D(
            WebGLRenderingContext.TEXTURE_2D, 
            0, 
            WebGLRenderingContext.RGBA,
            newWidth, 
            newHeight, 
            0, 
            WebGLRenderingContext.RGBA,
            WebGLRenderingContext.UNSIGNED_BYTE, 
            (ArrayBufferView) null
        );
        gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, null);
    }
}
