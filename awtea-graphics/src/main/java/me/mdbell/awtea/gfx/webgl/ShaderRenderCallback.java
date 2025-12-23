package me.mdbell.awtea.gfx.webgl;

/**
 * Callback interface for custom shader rendering operations.
 * <p>
 * This callback is invoked during the rendering pipeline execution,
 * allowing custom shader code to run at the appropriate time with
 * access to the WebGL backend and rasterizer.
 * </p>
 * 
 * @see WebGLShaderContext#queueShaderCall(CustomShaderProgram, ShaderRenderCallback)
 */
@FunctionalInterface
public interface ShaderRenderCallback {
    /**
     * Executes custom rendering logic with the provided backend and rasterizer.
     * <p>
     * The callback is executed with the shader already activated. After this method
     * returns, the shader is automatically deactivated.
     * </p>
     * 
     * @param backend the WebGL surface backend
     * @param rasterizer the WebGL rasterizer
     */
    void render(WebGLSurfaceBackend backend, WebGLRasterizer rasterizer);
}
