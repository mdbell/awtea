package me.mdbell.awtea.gfx.webgl;

import me.mdbell.awtea.gfx.Rasterizer;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

/**
 * Context manager for WebGL custom shader operations within a paint() call.
 * <p>
 * This class provides a ThreadLocal-like API for accessing the current WebGL context
 * during rendering operations. It allows queueing custom shader calls that will be
 * executed at the appropriate time in the rendering pipeline.
 * </p>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * public void paint(Graphics g) {
 *     g.drawString("Hello World!", 50, 50);
 *     
 *     WebGLShaderContext ctx = WebGLShaderContext.getCurrentContext();
 *     CustomShaderProgram shader = ctx.getShader("glow");
 *     if (shader != null) {
 *         ctx.queueShaderCall(shader, (backend, rasterizer) -> {
 *             // Custom rendering code here
 *             // Shader is already activated and will be deactivated automatically
 *         });
 *     }
 *     
 *     g.drawString("Hello Again!", 50, 100);
 * }
 * }</pre>
 */
public class WebGLShaderContext {
    
    private static final Logger log = LoggerFactory.getLogger(WebGLShaderContext.class);
    
    private static final ThreadLocal<WebGLShaderContext> currentContext = new ThreadLocal<>();
    
    private final WebGLSurfaceBackend backend;
    private final Rasterizer rasterizer;
    
    /**
     * Creates a new shader context for the given backend and rasterizer.
     * 
     * @param backend the WebGL surface backend
     * @param rasterizer the rasterizer
     */
    public WebGLShaderContext(WebGLSurfaceBackend backend, Rasterizer rasterizer) {
        this.backend = backend;
        this.rasterizer = rasterizer;
    }
    
    /**
     * Gets the current WebGL shader context for this thread/paint call.
     * <p>
     * Returns null if called outside of a WebGL rendering context or if
     * the current backend is not WebGL.
     * </p>
     * <p>
     * The context is automatically set up when the first drawing command is queued
     * in a paint() method and remains available until the frame is fully rendered.
     * </p>
     * 
     * @return the current shader context, or null if not available
     */
    public static WebGLShaderContext getCurrentContext() {
        return currentContext.get();
    }
    
    /**
     * Sets the current shader context for this thread.
     * This is called internally by the rendering pipeline.
     * 
     * @param context the context to set, or null to clear
     */
    public static void setCurrentContext(WebGLShaderContext context) {
        if (context == null) {
            currentContext.remove();
        } else {
            currentContext.set(context);
        }
    }
    
    /**
     * Gets a registered custom shader by name.
     * 
     * @param name the shader name
     * @return the shader program, or null if not found
     */
    public CustomShaderProgram getShader(String name) {
        return backend.getCustomShader(name);
    }
    
    /**
     * Queues a custom shader rendering callback to be executed in the rendering pipeline.
     * <p>
     * The callback will be executed after all previously queued drawing commands and before
     * any subsequent commands. The shader will be activated before the callback is invoked
     * and automatically deactivated afterwards.
     * </p>
     * 
     * @param shader the shader to activate
     * @param callback the rendering callback to execute
     */
    public void queueShaderCall(CustomShaderProgram shader, ShaderRenderCallback callback) {
        if (shader == null || callback == null) {
            log.warn("Cannot queue shader call: shader or callback is null");
            return;
        }
        
        // Create a wrapper object that holds both the shader and callback
        ShaderCallbackWrapper wrapper = new ShaderCallbackWrapper(shader, callback);
        
        // Queue the command through the rasterizer
        rasterizer.queueRenderCallback(wrapper);
    }
    
    /**
     * Gets the WebGL surface backend.
     * 
     * @return the backend
     */
    public WebGLSurfaceBackend getBackend() {
        return backend;
    }
    
    /**
     * Gets the current rasterizer.
     * 
     * @return the rasterizer
     */
    public Rasterizer getRasterizer() {
        return rasterizer;
    }
}
