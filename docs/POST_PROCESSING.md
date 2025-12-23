# WebGL Post-Processing Pipeline

The awtea WebGL backend supports advanced post-processing effects through a framebuffer object (FBO) management system. This enables multi-pass rendering, visual effects like bloom, blur, color correction, and more.

## Overview

Post-processing allows you to apply effects to rendered content before displaying it on screen. The system provides:

- **RenderTarget**: Offscreen framebuffers with texture attachments for intermediate rendering
- **RenderTargetPool**: Efficient resource pooling to minimize allocation overhead
- **PostProcessEffect**: Interface for implementing custom effects
- **PostProcessPipeline**: Chain multiple effects together with automatic ping-pong buffering
- **BloomEffect**: Built-in bloom effect demonstrating multi-pass rendering

## Architecture

### RenderTarget

A `RenderTarget` encapsulates a WebGL framebuffer and its associated texture. It represents an offscreen rendering surface that can be rendered to and then sampled as a texture.

```java
// Create a render target
RenderTarget target = new RenderTarget(gl, 800, 600);

// Render to it
target.bind();
// ... draw calls ...
target.unbind();

// Use as texture
gl.bindTexture(TEXTURE_2D, target.getTexture());

// Clean up
target.destroy();
```

**Key Methods:**
- `bind()` - Bind for rendering
- `unbind()` - Restore default framebuffer
- `clear(r, g, b, a)` - Clear with color
- `getTexture()` - Get texture for sampling
- `resize(width, height)` - Resize the target
- `destroy()` - Release GPU resources

### RenderTargetPool

The pool manages render target reuse to avoid expensive allocation/deallocation:

```java
RenderTargetPool pool = new RenderTargetPool(gl);

// Acquire a target
RenderTarget target = pool.acquire(800, 600);

// Use it...
target.bind();
// ...
target.unbind();

// Return to pool for reuse
pool.release(target);

// Clean up all
pool.destroy();
```

**Configuration:**
- `setMaxPooledPerSize(int)` - Max targets per dimension (default: 4)
- `setTextureFilter(int)` - Filter mode for new targets (LINEAR/NEAREST)
- `setTextureWrap(int)` - Wrap mode for new targets (CLAMP_TO_EDGE/REPEAT)

**Statistics:**
- `getTotalTargetCount()` - All managed targets
- `getPooledTargetCount()` - Currently available targets
- `getStats()` - Detailed pool statistics

### PostProcessEffect

Effects implement this interface to process textures:

```java
public interface PostProcessEffect {
    void apply(PostProcessContext ctx, RenderTarget input, RenderTarget output);
    default void dispose() {}
}
```

Example effect that inverts colors:

```java
public class InvertEffect implements PostProcessEffect {
    private CustomShaderProgram shader;
    
    @Override
    public void apply(PostProcessContext ctx, RenderTarget input, RenderTarget output) {
        output.bind();
        output.clear(0, 0, 0, 0);
        
        // Activate invert shader
        ctx.getBackend().activateCustomShader(shader);
        shader.setUniform2f("u_resolution", output.getWidth(), output.getHeight());
        shader.setUniform1i("u_texture", 0);
        
        // Blit input with inversion
        ctx.blit(input, output);
        
        ctx.getBackend().deactivateCustomShader();
        output.unbind();
    }
    
    @Override
    public void dispose() {
        // Clean up shader
    }
}
```

### PostProcessPipeline

The pipeline chains effects together automatically:

```java
PostProcessPipeline pipeline = new PostProcessPipeline(backend, pool);

// Add effects (applied in order)
pipeline.addEffect(new BloomEffect(backend));
pipeline.addEffect(new ColorCorrectionEffect());

// Apply to rendered scene
RenderTarget input = ...; // Your scene
RenderTarget output = pipeline.apply(input);

// Use output
gl.bindTexture(TEXTURE_2D, output.getTexture());

// Release output when done
pool.release(output);

// Clean up
pipeline.destroy();
```

The pipeline handles:
- Ping-pong buffering between effects
- Temporary target allocation/release
- Error handling and cleanup
- Logging and debugging

## Built-in Effects

### BloomEffect

Bloom creates a glow around bright areas. It uses three passes:

1. **Threshold** - Extract pixels brighter than threshold
2. **Blur** - Apply Gaussian blur to bright pixels
3. **Combine** - Add bloom back to original image

```java
BloomEffect bloom = new BloomEffect(backend);

// Configure effect
bloom.setThreshold(0.8f);    // Brightness threshold (0.0 - 1.0)
bloom.setIntensity(1.5f);     // Bloom strength (0.0+)
bloom.setBlurRadius(2.0f);    // Blur spread in pixels

// Add to pipeline
pipeline.addEffect(bloom);
```

**Parameters:**
- `threshold` - Only pixels with luminance > threshold will bloom (default: 0.8)
- `intensity` - Multiplier for bloom contribution (default: 1.0)
- `blurRadius` - Blur kernel radius in pixels (default: 1.5)

## Complete Example

Here's a complete post-processing setup:

```java
import me.mdbell.awtea.gfx.webgl.*;
import org.teavm.jso.webgl.WebGL2RenderingContext;

public class PostProcessExample {
    private WebGLSurfaceBackend backend;
    private RenderTargetPool pool;
    private PostProcessPipeline pipeline;
    private RenderTarget sceneTarget;
    
    public void init(WebGL2RenderingContext gl) {
        // Create backend (assuming canvas element setup)
        backend = new WebGLSurfaceBackend(canvasElement);
        
        // Create resource pool
        pool = new RenderTargetPool(gl);
        pool.setMaxPooledPerSize(6); // Allow more pooled targets
        
        // Create scene render target
        sceneTarget = new RenderTarget(gl, 800, 600);
        
        // Create pipeline with effects
        pipeline = new PostProcessPipeline(backend, pool);
        
        BloomEffect bloom = new BloomEffect(backend, pool);
        bloom.setThreshold(0.7f);
        bloom.setIntensity(1.2f);
        
        pipeline.addEffect(bloom);
    }
    
    public void render() {
        // 1. Render scene to offscreen target
        sceneTarget.bind();
        sceneTarget.clear(0, 0, 0, 1);
        renderScene(); // Your scene rendering
        sceneTarget.unbind();
        
        // 2. Apply post-processing
        RenderTarget finalOutput = pipeline.apply(sceneTarget);
        
        // 3. Blit final result to screen
        blitToScreen(finalOutput);
        
        // 4. Release output (if not sceneTarget)
        if (finalOutput != sceneTarget) {
            pool.release(finalOutput);
        }
    }
    
    private void renderScene() {
        // Your rendering code here
    }
    
    private void blitToScreen(RenderTarget target) {
        // Bind default framebuffer
        backend.getGL().bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, null);
        backend.getGL().viewport(0, 0, 800, 600);
        
        // Use texture program to blit
        backend.useTextureProgram(
            WebGLSurfaceBackend.SwizzleMode.NONE, 
            800, 600
        );
        
        // Setup fullscreen quad
        PostProcessContext ctx = new PostProcessContext(backend);
        ctx.setupFullscreenQuad(800, 600);
        
        // Bind and draw
        backend.getGL().activeTexture(WebGLRenderingContext.TEXTURE0);
        backend.getGL().bindTexture(WebGLRenderingContext.TEXTURE_2D, target.getTexture());
        backend.getGL().drawArrays(WebGLRenderingContext.TRIANGLES, 0, 6);
        backend.getGL().bindTexture(WebGLRenderingContext.TEXTURE_2D, null);
    }
    
    public void dispose() {
        pipeline.destroy();
        sceneTarget.destroy();
        pool.destroy();
    }
}
```

## Creating Custom Effects

To create a custom effect, implement `PostProcessEffect`:

```java
public class CustomEffect implements PostProcessEffect {
    private final WebGLSurfaceBackend backend;
    private CustomShaderProgram shader;
    
    public CustomEffect(WebGLSurfaceBackend backend) {
        this.backend = backend;
        initShader();
    }
    
    private void initShader() {
        String vertexShader = 
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
        
        String fragmentShader =
            "precision mediump float;\n" +
            "varying vec2 v_texCoord;\n" +
            "uniform sampler2D u_texture;\n" +
            "\n" +
            "void main() {\n" +
            "    vec4 color = texture2D(u_texture, v_texCoord);\n" +
            "    // Apply your effect here\n" +
            "    gl_FragColor = color;\n" +
            "}\n";
        
        shader = backend.registerCustomShader(
            "custom_effect",
            vertexShader,
            fragmentShader
        );
    }
    
    @Override
    public void apply(PostProcessContext ctx, RenderTarget input, RenderTarget output) {
        output.bind();
        output.clear(0, 0, 0, 0);
        
        backend.activateCustomShader(shader);
        
        // Set uniforms
        shader.setUniform2f("u_resolution", output.getWidth(), output.getHeight());
        shader.setUniform1i("u_texture", 0);
        
        // Render
        ctx.blit(input, output);
        
        backend.deactivateCustomShader();
        output.unbind();
    }
    
    @Override
    public void dispose() {
        if (shader != null) {
            backend.unregisterCustomShader("custom_effect");
            shader = null;
        }
    }
}
```

## Best Practices

### Resource Management

1. **Use the pool** - Always acquire/release targets through the pool
2. **Set pool size** - Configure `maxPooledPerSize` based on your effect chain depth
3. **Clean up** - Call `destroy()` on pipeline and pool when done
4. **Reuse targets** - Don't create new targets every frame

### Performance

1. **Minimize passes** - Each effect adds overhead; combine where possible
2. **Use smaller targets** - Consider half-resolution for some effects
3. **Optimize shaders** - Use mediump precision, minimize texture lookups
4. **Batch state changes** - Group effects using similar shaders

### Debugging

1. **Enable logging** - Set `LogLevel.TRACE` to see pool/pipeline activity
2. **Check pool stats** - Monitor `pool.getStats()` for leaks
3. **Use WebGL inspector** - Browser dev tools show framebuffer state
4. **Visualize targets** - Render intermediate targets to screen for debugging

### Common Pitfalls

1. **Not releasing targets** - Always release acquired targets to avoid leaks
2. **Wrong dimensions** - Ensure all targets in a chain have matching dimensions
3. **Forgetting to bind** - Always bind target before rendering to it
4. **Missing unbind** - Unbind before using texture to avoid feedback loop
5. **Shader precision** - Always specify `precision mediump float;` in shaders

## System Properties

- `me.mdbell.awtea.gfx.pool.max_size` - Override default pool size per dimension

## API Reference

### RenderTarget

| Method | Description |
|--------|-------------|
| `RenderTarget(gl, width, height)` | Create with default parameters |
| `RenderTarget(gl, width, height, filter, wrap)` | Create with custom parameters |
| `bind()` | Bind for rendering |
| `unbind()` | Unbind (restore default FB) |
| `clear(r, g, b, a)` | Clear with color |
| `getTexture()` | Get texture object |
| `getFramebuffer()` | Get framebuffer object |
| `getWidth()` / `getHeight()` | Get dimensions |
| `resize(width, height)` | Resize texture |
| `isDestroyed()` | Check if destroyed |
| `destroy()` | Release resources |

### RenderTargetPool

| Method | Description |
|--------|-------------|
| `RenderTargetPool(gl)` | Create pool |
| `acquire(width, height)` | Get target (reuse or create) |
| `release(target)` | Return target to pool |
| `setMaxPooledPerSize(max)` | Set pool size limit |
| `setTextureFilter(filter)` | Set filter for new targets |
| `setTextureWrap(wrap)` | Set wrap for new targets |
| `clear()` | Clear pool (destroy pooled) |
| `destroy()` | Destroy all targets |
| `getStats()` | Get pool statistics |
| `getTotalTargetCount()` | Total managed targets |
| `getPooledTargetCount()` | Available targets |

### PostProcessEffect

| Method | Description |
|--------|-------------|
| `apply(ctx, input, output)` | Apply effect |
| `dispose()` | Clean up resources |

### PostProcessPipeline

| Method | Description |
|--------|-------------|
| `PostProcessPipeline(backend, pool)` | Create pipeline |
| `addEffect(effect)` | Add effect to end |
| `removeEffect(effect)` | Remove effect |
| `clearEffects()` | Remove all effects |
| `apply(input)` | Apply all effects, return output |
| `getEffectCount()` | Get number of effects |
| `isEmpty()` | Check if empty |
| `getContext()` | Get PostProcessContext |
| `destroy()` | Dispose all effects |

### PostProcessContext

| Method | Description |
|--------|-------------|
| `getBackend()` | Get WebGLSurfaceBackend |
| `blit(input, output)` | Blit with current shader |
| `blitSimple(input, output)` | Blit with passthrough |
| `setupFullscreenQuad(w, h)` | Create fullscreen quad |

### BloomEffect

| Method | Description |
|--------|-------------|
| `BloomEffect(backend)` | Create with default pool |
| `BloomEffect(backend, pool)` | Create with shared pool |
| `setThreshold(threshold)` | Set brightness threshold (0-1) |
| `setIntensity(intensity)` | Set bloom strength (0+) |
| `setBlurRadius(radius)` | Set blur spread (pixels) |
| `apply(ctx, input, output)` | Apply bloom effect |
| `dispose()` | Clean up shaders |

## Future Enhancements

Planned features:
- More built-in effects (blur, sharpen, color grading, vignette)
- Separable Gaussian blur for better performance
- Render to multiple attachments (MRT)
- Depth/stencil buffer support
- Effect presets and parameter serialization
- GPU-based histogram and auto-exposure
- Temporal effects (motion blur, TAA)

## See Also

- [Custom Shader API](CUSTOM_SHADERS.md) - For creating effect shaders
- [Rendering Backends](RENDERING_BACKENDS.md) - Backend architecture
- [WebGL Fundamentals - Post Processing](https://webglfundamentals.org/webgl/lessons/webgl-post-processing.html)
