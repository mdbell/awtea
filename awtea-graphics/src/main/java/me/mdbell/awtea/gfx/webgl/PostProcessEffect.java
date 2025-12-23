package me.mdbell.awtea.gfx.webgl;

/**
 * Interface for post-processing effects that can be applied to rendered content.
 * 
 * <p>Post-processing effects take an input texture and produce an output texture,
 * potentially using multiple passes. Effects can be chained together to create
 * complex visual enhancements.</p>
 * 
 * <p>Example implementation:
 * <pre>
 * public class BlurEffect implements PostProcessEffect {
 *     private CustomShaderProgram shader;
 *     
 *     &#64;Override
 *     public void apply(PostProcessContext ctx, RenderTarget input, RenderTarget output) {
 *         output.bind();
 *         output.clear(0, 0, 0, 0);
 *         
 *         // Activate blur shader and render
 *         ctx.getBackend().activateCustomShader(shader);
 *         shader.setUniform2f("u_resolution", output.getWidth(), output.getHeight());
 *         
 *         // Blit input texture with blur
 *         ctx.blit(input, output);
 *         
 *         ctx.getBackend().deactivateCustomShader();
 *         output.unbind();
 *     }
 * }
 * </pre>
 * </p>
 */
public interface PostProcessEffect {
    
    /**
     * Applies this effect to the input and renders to the output.
     * 
     * <p>Implementations should:
     * <ul>
     *   <li>Bind the output target</li>
     *   <li>Set up shaders and uniforms</li>
     *   <li>Render using the input texture</li>
     *   <li>Unbind the output target</li>
     * </ul>
     * </p>
     * 
     * <p>The context provides helper methods for common operations like blitting
     * and accessing the WebGL backend.</p>
     * 
     * @param ctx the post-processing context with helper methods
     * @param input the input render target to read from
     * @param output the output render target to render to
     */
    void apply(PostProcessContext ctx, RenderTarget input, RenderTarget output);
    
    /**
     * Called when the effect is no longer needed.
     * Implementations should clean up any resources (shaders, buffers, etc.).
     */
    default void dispose() {
        // Default: no cleanup needed
    }
}
