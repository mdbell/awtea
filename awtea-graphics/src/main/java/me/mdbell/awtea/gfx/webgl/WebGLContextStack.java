package me.mdbell.awtea.gfx.webgl;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.webgl.WebGL2RenderingContext;
import org.teavm.jso.webgl.WebGLRenderingContext;

import java.awt.*;
import java.util.Stack;

/**
 * Manages WebGL rendering state with save/restore semantics.
 * Provides state isolation for child rasterizers by maintaining a stack
 * of rendering state snapshots.
 * 
 * <p>This class tracks all rendering state that needs to be preserved
 * across rasterizer creation/disposal:
 * <ul>
 *   <li>Composite/blend modes</li>
 *   <li>Foreground and background colors</li>
 *   <li>Affine transforms</li>
 *   <li>Clip rectangles</li>
 * </ul>
 */
class WebGLContextStack {
    
    private static final Logger log = LoggerFactory.getLogger(WebGLContextStack.class);
    
    private final WebGL2RenderingContext gl;
    private final Stack<WebGLState> stateStack = new Stack<>();
    private WebGLState currentState;
    
    // Surface dimensions needed for clip application
    private int surfaceWidth;
    private int surfaceHeight;
    
    // Transform array for passing to shaders (in column-major order)
    private final Float32Array transformArray = new Float32Array(9);
    
    // Uniform location for color (set by backend after program creation)
    private org.teavm.jso.webgl.WebGLUniformLocation uColorLoc;
    
    /**
     * Snapshot of WebGL rendering state.
     */
    static class WebGLState {
        Composite composite;
        int blendSrc;
        int blendDst;
        Color foreground;
        Color background;
        java.awt.geom.AffineTransform transform;
        Rectangle clip;
        
        /**
         * Creates a new state with default values.
         */
        WebGLState() {
            this.composite = AlphaComposite.SrcOver;
            this.blendSrc = WebGLRenderingContext.SRC_ALPHA;
            this.blendDst = WebGLRenderingContext.ONE_MINUS_SRC_ALPHA;
            this.foreground = Color.WHITE;
            this.background = Color.BLACK;
            this.transform = new java.awt.geom.AffineTransform();
            this.clip = null;
        }
        
        /**
         * Creates a copy of another state.
         */
        WebGLState(WebGLState other) {
            this.composite = other.composite;
            this.blendSrc = other.blendSrc;
            this.blendDst = other.blendDst;
            this.foreground = other.foreground;
            this.background = other.background;
            this.transform = new java.awt.geom.AffineTransform(other.transform);
            this.clip = other.clip != null ? new Rectangle(other.clip) : null;
        }
    }
    
    /**
     * Creates a new context stack with default initial state.
     * 
     * @param gl the WebGL rendering context
     */
    WebGLContextStack(WebGL2RenderingContext gl) {
        this.gl = gl;
        this.currentState = new WebGLState();
        updateTransformArray(); // Initialize transform array with identity matrix
    }
    
    /**
     * Sets the surface dimensions for clip application.
     */
    void setSurfaceDimensions(int width, int height) {
        this.surfaceWidth = width;
        this.surfaceHeight = height;
    }
    
    /**
     * Sets the color uniform location for applying foreground color.
     */
    void setColorUniformLocation(org.teavm.jso.webgl.WebGLUniformLocation uColorLoc) {
        this.uColorLoc = uColorLoc;
    }
    
    /**
     * Saves the current state by pushing a copy onto the stack.
     * This should be called when creating a child rasterizer.
     */
    void save() {
        log.trace("WebGLContextStack: Saving state (stack depth: {})", stateStack.size());
        stateStack.push(new WebGLState(currentState));
    }
    
    /**
     * Restores the previously saved state by popping from the stack.
     * This should be called when disposing a child rasterizer.
     * Safe to call on empty stack (logs warning but doesn't throw).
     */
    void restore() {
        if (stateStack.isEmpty()) {
            log.warn("WebGLContextStack: Attempted to restore from empty stack");
            return;
        }
        
        log.trace("WebGLContextStack: Restoring state (stack depth: {})", stateStack.size());
        currentState = stateStack.pop();
        
        // Update transform array with restored transform
        updateTransformArray();
        
        // Re-apply the restored state to WebGL context
        applyComposite(currentState.composite);
    }
    
    /**
     * Sets the composite mode and applies it to the WebGL context.
     */
    void setComposite(Composite composite) {
        currentState.composite = composite != null ? composite : AlphaComposite.SrcOver;
        applyComposite(currentState.composite);
    }
    
    /**
     * Gets the current composite mode.
     */
    Composite getComposite() {
        return currentState.composite;
    }
    
    /**
     * Sets the foreground color.
     */
    void setForeground(Color color) {
        currentState.foreground = color;
    }
    
    /**
     * Gets the current foreground color.
     */
    Color getForeground() {
        return currentState.foreground;
    }
    
    /**
     * Sets the background color.
     */
    void setBackground(Color color) {
        currentState.background = color;
    }
    
    /**
     * Gets the current background color.
     */
    Color getBackground() {
        return currentState.background;
    }
    
    /**
     * Sets the transform.
     */
    void setTransform(java.awt.geom.AffineTransform transform) {
        currentState.transform.setTransform(transform);
        updateTransformArray();
    }
    
    /**
     * Gets the current transform.
     */
    java.awt.geom.AffineTransform getTransform() {
        return currentState.transform;
    }
    
    /**
     * Gets the transform array in column-major format for shader uniforms.
     */
    Float32Array getTransformArray() {
        return transformArray;
    }
    
    /**
     * Updates the transform array from the current transform.
     * Matrix is in column-major order for WebGL:
     * | m00 m10 0 |
     * | m01 m11 0 |
     * | m02 m12 1 |
     * 
     * Note: Y translation is negated because WebGL uses bottom-up coordinates
     * while AWT uses top-down coordinates.
     */
    private void updateTransformArray() {
        java.awt.geom.AffineTransform t = currentState.transform;
        transformArray.set(0, (float) t.getScaleX());
        transformArray.set(1, (float) t.getShearY());
        transformArray.set(2, 0f);
        transformArray.set(3, (float) t.getShearX());
        transformArray.set(4, (float) t.getScaleY());
        transformArray.set(5, 0f);
        transformArray.set(6, (float) t.getTranslateX());
        transformArray.set(7, (float) -t.getTranslateY()); // Negate Y translation for WebGL space
        transformArray.set(8, 1f);
    }
    
    /**
     * Sets the clip rectangle.
     */
    void setClip(Rectangle clip) {
        currentState.clip = clip != null ? new Rectangle(clip) : null;
    }
    
    /**
     * Gets the current clip rectangle.
     */
    Rectangle getClip() {
        return currentState.clip;
    }
    
    /**
     * Applies the composite mode to the WebGL context by setting blend functions.
     */
    private void applyComposite(Composite composite) {
        if (composite instanceof AlphaComposite) {
            AlphaComposite ac = (AlphaComposite) composite;
            int rule = ac.getRule();
            
            // Map AlphaComposite rules to WebGL blend functions
            switch (rule) {
                case AlphaComposite.CLEAR:
                    currentState.blendSrc = WebGLRenderingContext.ZERO;
                    currentState.blendDst = WebGLRenderingContext.ZERO;
                    break;
                case AlphaComposite.SRC:
                    currentState.blendSrc = WebGLRenderingContext.ONE;
                    currentState.blendDst = WebGLRenderingContext.ZERO;
                    break;
                case AlphaComposite.DST:
                    currentState.blendSrc = WebGLRenderingContext.ZERO;
                    currentState.blendDst = WebGLRenderingContext.ONE;
                    break;
                case AlphaComposite.SRC_OVER:
                    currentState.blendSrc = WebGLRenderingContext.SRC_ALPHA;
                    currentState.blendDst = WebGLRenderingContext.ONE_MINUS_SRC_ALPHA;
                    break;
                case AlphaComposite.DST_OVER:
                    currentState.blendSrc = WebGLRenderingContext.ONE_MINUS_DST_ALPHA;
                    currentState.blendDst = WebGLRenderingContext.DST_ALPHA;
                    break;
                case AlphaComposite.SRC_IN:
                    currentState.blendSrc = WebGLRenderingContext.DST_ALPHA;
                    currentState.blendDst = WebGLRenderingContext.ZERO;
                    break;
                case AlphaComposite.DST_IN:
                    currentState.blendSrc = WebGLRenderingContext.ZERO;
                    currentState.blendDst = WebGLRenderingContext.SRC_ALPHA;
                    break;
                case AlphaComposite.SRC_OUT:
                    currentState.blendSrc = WebGLRenderingContext.ONE_MINUS_DST_ALPHA;
                    currentState.blendDst = WebGLRenderingContext.ZERO;
                    break;
                case AlphaComposite.DST_OUT:
                    currentState.blendSrc = WebGLRenderingContext.ZERO;
                    currentState.blendDst = WebGLRenderingContext.ONE_MINUS_SRC_ALPHA;
                    break;
                case AlphaComposite.SRC_ATOP:
                    currentState.blendSrc = WebGLRenderingContext.DST_ALPHA;
                    currentState.blendDst = WebGLRenderingContext.ONE_MINUS_SRC_ALPHA;
                    break;
                case AlphaComposite.DST_ATOP:
                    currentState.blendSrc = WebGLRenderingContext.ONE_MINUS_DST_ALPHA;
                    currentState.blendDst = WebGLRenderingContext.SRC_ALPHA;
                    break;
                case AlphaComposite.XOR:
                    currentState.blendSrc = WebGLRenderingContext.ONE_MINUS_DST_ALPHA;
                    currentState.blendDst = WebGLRenderingContext.ONE_MINUS_SRC_ALPHA;
                    break;
                default:
                    // Default to SRC_OVER
                    currentState.blendSrc = WebGLRenderingContext.SRC_ALPHA;
                    currentState.blendDst = WebGLRenderingContext.ONE_MINUS_SRC_ALPHA;
                    break;
            }
            
            gl.blendFunc(currentState.blendSrc, currentState.blendDst);
        }
    }
    
    /**
     * Applies all WebGL-level state to the context.
     * This applies state that can be set directly on the WebGL context:
     * - Blend enable
     * - Composite/blend modes (glBlendFunc)
     * - Clip rectangle (glScissor)
     * - Transform array (updated for shader uniforms)
     * 
     * Note: Color uniform is NOT applied here because it's only valid for the color program.
     * Call applyColorUniform() separately when using the color program.
     * 
     * This is called automatically by useColorProgram() and useTextureProgram()
     * to ensure state consistency whenever a shader program is activated.
     */
    void apply() {
        // Enable blending
        gl.enable(WebGLRenderingContext.BLEND);
        
        // Update transform array from current transform
        updateTransformArray();
        
        // Apply composite/blend mode
        applyComposite(currentState.composite);
        
        // Apply clip rectangle
        applyClip();
    }
    
    /**
     * Applies the foreground color to the color uniform.
     * This should only be called when the color program is active.
     */
    void applyColorUniform() {
        if (uColorLoc == null) {
            return; // Color uniform not set yet
        }
        Color c = currentState.foreground;
        float r = c.getRed() / 255.0f;
        float g = c.getGreen() / 255.0f;
        float b = c.getBlue() / 255.0f;
        float a = c.getAlpha() / 255.0f;
        gl.uniform4f(uColorLoc, r, g, b, a);
    }
    
    /**
     * Applies the clip rectangle to the WebGL context.
     */
    private void applyClip() {
        if (currentState.clip == null) {
            gl.disable(WebGLRenderingContext.SCISSOR_TEST);
            return;
        }
        gl.enable(WebGLRenderingContext.SCISSOR_TEST);
        gl.scissor(currentState.clip.x, currentState.clip.y, 
                   currentState.clip.width, currentState.clip.height);
    }
}
