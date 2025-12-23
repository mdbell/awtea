package me.mdbell.awtea.gfx.webgl;

import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.webgl.WebGLRenderingContext;

/**
 * Context object providing helper methods for post-processing effects.
 * This class encapsulates common operations needed when applying effects.
 */
public class PostProcessContext {
    
    private final WebGLSurfaceBackend backend;
    
    /**
     * Creates a new post-processing context.
     * 
     * @param backend the WebGL surface backend
     */
    public PostProcessContext(WebGLSurfaceBackend backend) {
        this.backend = backend;
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
     * Blits (copies) the input texture to the output target using the currently active shader.
     * This is a common operation for applying effects that process a texture.
     * 
     * <p>The output target must be bound before calling this method.</p>
     * 
     * @param input the input render target containing the texture to blit
     * @param output the output render target (must be bound)
     */
    public void blit(RenderTarget input, RenderTarget output) {
        // Full-screen quad vertices
        float[] vertices = {
            0, 0,
            output.getWidth(), 0,
            0, output.getHeight(),
            0, output.getHeight(),
            output.getWidth(), 0,
            output.getWidth(), output.getHeight()
        };
        
        // Texture coordinates
        float[] uvs = {
            0f, 0f,
            1f, 0f,
            0f, 1f,
            0f, 1f,
            1f, 0f,
            1f, 1f
        };
        
        // Upload vertices
        backend.uploadQuadVertices(vertices, uvs);
        
        // Bind input texture
        backend.getGL().activeTexture(WebGLRenderingContext.TEXTURE0);
        backend.getGL().bindTexture(WebGLRenderingContext.TEXTURE_2D, input.getTexture());
        
        // Draw
        backend.getGL().drawArrays(WebGLRenderingContext.TRIANGLES, 0, 6);
        
        // Cleanup
        backend.getGL().bindTexture(WebGLRenderingContext.TEXTURE_2D, null);
    }
    
    /**
     * Blits the input texture to the output using a simple passthrough shader.
     * This is useful for copying without any effects.
     * 
     * @param input the input render target
     * @param output the output render target (must be bound)
     */
    public void blitSimple(RenderTarget input, RenderTarget output) {
        // Use texture program with no swizzling
        backend.useTextureProgram(WebGLSurfaceBackend.SwizzleMode.NONE, 
            output.getWidth(), output.getHeight());
        
        blit(input, output);
    }
    
    /**
     * Creates a full-screen quad for rendering.
     * Uploads vertex and UV data to the backend's quad buffers.
     * 
     * @param width the quad width
     * @param height the quad height
     */
    public void setupFullscreenQuad(int width, int height) {
        float[] vertices = {
            0, 0,
            width, 0,
            0, height,
            0, height,
            width, 0,
            width, height
        };
        
        float[] uvs = {
            0f, 0f,
            1f, 0f,
            0f, 1f,
            0f, 1f,
            1f, 0f,
            1f, 1f
        };
        
        backend.uploadQuadVertices(vertices, uvs);
    }
}
