# Post-Processing Demo

This example demonstrates the WebGL post-processing pipeline in awtea, featuring:

- **RenderTarget**: Offscreen framebuffer management
- **RenderTargetPool**: Efficient FBO resource pooling
- **PostProcessPipeline**: Multi-pass effect chaining
- **BloomEffect**: 3-pass bloom implementation (threshold, blur, combine)

## Running the Demo

Build and run the demo:

```bash
# From the repository root
./gradlew :examples:post-process-demo:build

# Open in browser
open examples/post-process-demo/build/dist/index.html
```

Or use `--no-daemon` to avoid TeaVM plugin issues:

```bash
./gradlew --no-daemon :examples:post-process-demo:build
```

## Controls

- **B** - Toggle bloom effect on/off
- **+/-** - Adjust bloom intensity
- **T** - Cycle bloom threshold (brightness cutoff)

## How It Works

### Rendering Pipeline

1. **Scene Rendering** - Draw bright objects to an offscreen `RenderTarget`
2. **Bloom Processing** - Apply the bloom effect pipeline:
   - **Pass 1 (Threshold)**: Extract pixels brighter than threshold
   - **Pass 2 (Blur)**: Gaussian blur the bright regions
   - **Pass 3 (Combine)**: Add bloom to original scene
3. **Screen Blit** - Composite final result to the canvas

### Resource Management

The demo uses a `RenderTargetPool` to reuse framebuffers efficiently:

```java
// Acquire from pool
RenderTarget temp = pool.acquire(width, height);

// Use it
temp.bind();
// ... render ...
temp.unbind();

// Return to pool for reuse
pool.release(temp);
```

### Effect Configuration

Bloom parameters can be adjusted:

```java
bloomEffect.setThreshold(0.6f);  // Brightness threshold (0.0-1.0)
bloomEffect.setIntensity(1.5f);   // Bloom strength
bloomEffect.setBlurRadius(2.0f);  // Blur spread
```

## Implementation Details

### Scene Rendering

The demo renders an animated scene with:
- Central bright "sun" object
- 5 orbiting bright colored orbs
- Twinkling dimmer stars

All geometry is drawn using WebGL primitives (triangle fans for circles).

### Bloom Effect Shaders

The bloom effect uses three custom GLSL shaders:

1. **Threshold Shader** - Calculates luminance and filters pixels:
   ```glsl
   float luminance = dot(color.rgb, vec3(0.299, 0.587, 0.114));
   if (luminance > u_threshold) {
       gl_FragColor = color;
   }
   ```

2. **Blur Shader** - Simple 3x3 box blur (could be optimized with separable Gaussian)

3. **Combine Shader** - Additive blend:
   ```glsl
   gl_FragColor = original + bloom * u_intensity;
   ```

## API Usage Example

```java
// Setup
WebGLSurfaceBackend backend = new WebGLSurfaceBackend(canvas);
RenderTargetPool pool = new RenderTargetPool(backend.getGL());
PostProcessPipeline pipeline = new PostProcessPipeline(backend, pool);

// Add effects
BloomEffect bloom = new BloomEffect(backend, pool);
pipeline.addEffect(bloom);

// Each frame
sceneTarget.bind();
renderScene(); // Your rendering
sceneTarget.unbind();

RenderTarget output = pipeline.apply(sceneTarget);
blitToScreen(output);
pool.release(output);

// Cleanup
pipeline.destroy();
pool.destroy();
```

## Performance Notes

- The demo runs at ~60fps with 3 rendering passes
- Pool reuse eliminates FBO allocation overhead
- Bloom uses simple box blur (separable Gaussian would be faster)
- Half-resolution bloom would improve performance further

## See Also

- [POST_PROCESSING.md](../../docs/POST_PROCESSING.md) - Full API documentation
- [CUSTOM_SHADERS.md](../../docs/CUSTOM_SHADERS.md) - Shader programming guide
- [WebGL Fundamentals - Post Processing](https://webglfundamentals.org/webgl/lessons/webgl-post-processing.html)
