package me.mdbell.awtea.gfx.webgl;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.webgl.WebGLRenderingContext;

/**
 * Bloom post-processing effect with threshold, blur, and combine passes.
 * 
 * <p>Bloom creates a glow around bright areas of the image. It works in three passes:
 * <ol>
 *   <li><b>Threshold:</b> Extract bright regions above a luminance threshold</li>
 *   <li><b>Blur:</b> Apply Gaussian blur to the bright regions</li>
 *   <li><b>Combine:</b> Add the blurred bright regions back to the original</li>
 * </ol>
 * </p>
 * 
 * <p>Example usage:
 * <pre>
 * BloomEffect bloom = new BloomEffect(backend);
 * bloom.setThreshold(0.8f);    // Only pixels brighter than 80% bloom
 * bloom.setIntensity(1.5f);     // Bloom intensity
 * bloom.setBlurRadius(2.0f);    // Blur spread
 * 
 * pipeline.addEffect(bloom);
 * </pre>
 * </p>
 */
public class BloomEffect implements PostProcessEffect {
    
    private static final Logger log = LoggerFactory.getLogger(BloomEffect.class);
    
    private final WebGLSurfaceBackend backend;
    private final RenderTargetPool pool;
    
    // Shaders
    private CustomShaderProgram thresholdShader;
    private CustomShaderProgram blurShader;
    private CustomShaderProgram combineShader;
    
    // Effect parameters
    private float threshold = 0.8f;
    private float intensity = 1.0f;
    private float blurRadius = 1.5f;
    
    private boolean shadersInitialized = false;
    
    /**
     * Creates a new bloom effect.
     * 
     * @param backend the WebGL surface backend
     */
    public BloomEffect(WebGLSurfaceBackend backend) {
        this(backend, new RenderTargetPool(backend.getGL()));
    }
    
    /**
     * Creates a new bloom effect with a shared render target pool.
     * 
     * @param backend the WebGL surface backend
     * @param pool the render target pool
     */
    public BloomEffect(WebGLSurfaceBackend backend, RenderTargetPool pool) {
        this.backend = backend;
        this.pool = pool;
    }
    
    @Override
    public void apply(PostProcessContext ctx, RenderTarget input, RenderTarget output) {
        if (!shadersInitialized) {
            initializeShaders();
        }
        
        int width = input.getWidth();
        int height = input.getHeight();
        
        // Acquire temporary targets
        RenderTarget brightPass = pool.acquire(width, height);
        RenderTarget blurPass = pool.acquire(width, height);
        
        try {
            // Pass 1: Extract bright regions
            applyThreshold(ctx, input, brightPass);
            
            // Pass 2: Blur the bright regions
            applyBlur(ctx, brightPass, blurPass);
            
            // Pass 3: Combine original + bloom
            combine(ctx, input, blurPass, output);
            
        } finally {
            // Release temporary targets
            pool.release(brightPass);
            pool.release(blurPass);
        }
    }
    
    /**
     * Pass 1: Threshold - Extract pixels brighter than threshold.
     */
    private void applyThreshold(PostProcessContext ctx, RenderTarget input, RenderTarget output) {
        output.bind();
        output.clear(0, 0, 0, 0);
        
        backend.activateCustomShader(thresholdShader);
        thresholdShader.setUniform2f("u_resolution", output.getWidth(), output.getHeight());
        thresholdShader.setUniform1f("u_threshold", threshold);
        thresholdShader.setUniform1i("u_texture", 0);
        
        ctx.blit(input, output);
        
        backend.deactivateCustomShader();
        output.unbind();
    }
    
    /**
     * Pass 2: Blur - Apply Gaussian blur to bright regions.
     */
    private void applyBlur(PostProcessContext ctx, RenderTarget input, RenderTarget output) {
        output.bind();
        output.clear(0, 0, 0, 0);
        
        backend.activateCustomShader(blurShader);
        blurShader.setUniform2f("u_resolution", output.getWidth(), output.getHeight());
        blurShader.setUniform1f("u_blurRadius", blurRadius);
        blurShader.setUniform1i("u_texture", 0);
        
        ctx.blit(input, output);
        
        backend.deactivateCustomShader();
        output.unbind();
    }
    
    /**
     * Pass 3: Combine - Add bloom to original image.
     */
    private void combine(PostProcessContext ctx, RenderTarget original, RenderTarget bloom, RenderTarget output) {
        output.bind();
        output.clear(0, 0, 0, 0);
        
        backend.activateCustomShader(combineShader);
        combineShader.setUniform2f("u_resolution", output.getWidth(), output.getHeight());
        combineShader.setUniform1f("u_intensity", intensity);
        combineShader.setUniform1i("u_original", 0);
        combineShader.setUniform1i("u_bloom", 1);
        
        // Bind both textures
        backend.getGL().activeTexture(WebGLRenderingContext.TEXTURE0);
        backend.getGL().bindTexture(WebGLRenderingContext.TEXTURE_2D, original.getTexture());
        
        backend.getGL().activeTexture(WebGLRenderingContext.TEXTURE1);
        backend.getGL().bindTexture(WebGLRenderingContext.TEXTURE_2D, bloom.getTexture());
        
        ctx.setupFullscreenQuad(output.getWidth(), output.getHeight());
        backend.getGL().drawArrays(WebGLRenderingContext.TRIANGLES, 0, 6);
        
        // Cleanup
        backend.getGL().activeTexture(WebGLRenderingContext.TEXTURE1);
        backend.getGL().bindTexture(WebGLRenderingContext.TEXTURE_2D, null);
        backend.getGL().activeTexture(WebGLRenderingContext.TEXTURE0);
        backend.getGL().bindTexture(WebGLRenderingContext.TEXTURE_2D, null);
        
        backend.deactivateCustomShader();
        output.unbind();
    }
    
    private void initializeShaders() {
        try {
            // Threshold shader: Extract bright pixels
            thresholdShader = backend.registerCustomShader(
                "bloom_threshold",
                THRESHOLD_VERTEX_SHADER,
                THRESHOLD_FRAGMENT_SHADER
            );
            
            // Blur shader: Gaussian blur
            blurShader = backend.registerCustomShader(
                "bloom_blur",
                BLUR_VERTEX_SHADER,
                BLUR_FRAGMENT_SHADER
            );
            
            // Combine shader: Add bloom to original
            combineShader = backend.registerCustomShader(
                "bloom_combine",
                COMBINE_VERTEX_SHADER,
                COMBINE_FRAGMENT_SHADER
            );
            
            shadersInitialized = true;
            log.info("Bloom effect shaders initialized");
            
        } catch (RuntimeException e) {
            log.error("Failed to initialize bloom shaders: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Sets the brightness threshold for bloom extraction.
     * Pixels with luminance below this value won't bloom.
     * 
     * @param threshold threshold value (0.0 - 1.0), default 0.8
     */
    public void setThreshold(float threshold) {
        this.threshold = Math.max(0.0f, Math.min(1.0f, threshold));
    }
    
    /**
     * Sets the bloom intensity (strength).
     * 
     * @param intensity intensity multiplier (0.0+), default 1.0
     */
    public void setIntensity(float intensity) {
        this.intensity = Math.max(0.0f, intensity);
    }
    
    /**
     * Sets the blur radius (spread).
     * 
     * @param radius blur radius in pixels, default 1.5
     */
    public void setBlurRadius(float radius) {
        this.blurRadius = Math.max(0.0f, radius);
    }
    
    @Override
    public void dispose() {
        if (thresholdShader != null) {
            backend.unregisterCustomShader("bloom_threshold");
            thresholdShader = null;
        }
        if (blurShader != null) {
            backend.unregisterCustomShader("bloom_blur");
            blurShader = null;
        }
        if (combineShader != null) {
            backend.unregisterCustomShader("bloom_combine");
            combineShader = null;
        }
        shadersInitialized = false;
        log.debug("Bloom effect disposed");
    }
    
    // Shader sources
    
    private static final String THRESHOLD_VERTEX_SHADER =
        "precision mediump float;\n" +
        "attribute vec2 a_position;\n" +
        "attribute vec2 a_texCoord;\n" +
        "varying vec2 v_texCoord;\n" +
        "uniform vec2 u_resolution;\n" +
        "\n" +
        "void main() {\n" +
        "    vec2 clipSpace = (a_position / u_resolution) * 2.0 - 1.0;\n" +
        "    gl_Position = vec4(clipSpace * vec2(1, -1), 0, 1);\n" +
        "    v_texCoord = a_texCoord;\n" +
        "}\n";
    
    private static final String THRESHOLD_FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "varying vec2 v_texCoord;\n" +
        "uniform sampler2D u_texture;\n" +
        "uniform float u_threshold;\n" +
        "\n" +
        "void main() {\n" +
        "    vec4 color = texture2D(u_texture, v_texCoord);\n" +
        "    float luminance = dot(color.rgb, vec3(0.299, 0.587, 0.114));\n" +
        "    \n" +
        "    // Only output pixels brighter than threshold\n" +
        "    if (luminance > u_threshold) {\n" +
        "        gl_FragColor = color;\n" +
        "    } else {\n" +
        "        gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);\n" +
        "    }\n" +
        "}\n";
    
    private static final String BLUR_VERTEX_SHADER =
        "precision mediump float;\n" +
        "attribute vec2 a_position;\n" +
        "attribute vec2 a_texCoord;\n" +
        "varying vec2 v_texCoord;\n" +
        "uniform vec2 u_resolution;\n" +
        "\n" +
        "void main() {\n" +
        "    vec2 clipSpace = (a_position / u_resolution) * 2.0 - 1.0;\n" +
        "    gl_Position = vec4(clipSpace * vec2(1, -1), 0, 1);\n" +
        "    v_texCoord = a_texCoord;\n" +
        "}\n";
    
    private static final String BLUR_FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "varying vec2 v_texCoord;\n" +
        "uniform sampler2D u_texture;\n" +
        "uniform vec2 u_resolution;\n" +
        "uniform float u_blurRadius;\n" +
        "\n" +
        "void main() {\n" +
        "    vec2 texelSize = 1.0 / u_resolution;\n" +
        "    vec4 result = vec4(0.0);\n" +
        "    \n" +
        "    // Simple box blur (can be optimized with separable Gaussian)\n" +
        "    float totalWeight = 0.0;\n" +
        "    int samples = 9;\n" +
        "    \n" +
        "    for (int x = -1; x <= 1; x++) {\n" +
        "        for (int y = -1; y <= 1; y++) {\n" +
        "            vec2 offset = vec2(float(x), float(y)) * texelSize * u_blurRadius;\n" +
        "            float weight = 1.0;\n" +
        "            result += texture2D(u_texture, v_texCoord + offset) * weight;\n" +
        "            totalWeight += weight;\n" +
        "        }\n" +
        "    }\n" +
        "    \n" +
        "    gl_FragColor = result / totalWeight;\n" +
        "}\n";
    
    private static final String COMBINE_VERTEX_SHADER =
        "precision mediump float;\n" +
        "attribute vec2 a_position;\n" +
        "attribute vec2 a_texCoord;\n" +
        "varying vec2 v_texCoord;\n" +
        "uniform vec2 u_resolution;\n" +
        "\n" +
        "void main() {\n" +
        "    vec2 clipSpace = (a_position / u_resolution) * 2.0 - 1.0;\n" +
        "    gl_Position = vec4(clipSpace * vec2(1, -1), 0, 1);\n" +
        "    v_texCoord = a_texCoord;\n" +
        "}\n";
    
    private static final String COMBINE_FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "varying vec2 v_texCoord;\n" +
        "uniform sampler2D u_original;\n" +
        "uniform sampler2D u_bloom;\n" +
        "uniform float u_intensity;\n" +
        "\n" +
        "void main() {\n" +
        "    vec4 original = texture2D(u_original, v_texCoord);\n" +
        "    vec4 bloom = texture2D(u_bloom, v_texCoord);\n" +
        "    \n" +
        "    // Additive blend with intensity\n" +
        "    gl_FragColor = original + bloom * u_intensity;\n" +
        "}\n";
}
