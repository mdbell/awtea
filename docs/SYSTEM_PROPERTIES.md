# awtea System Properties

This document provides comprehensive documentation for all system properties that awtea recognizes and uses. System properties can be set via command-line arguments (e.g., `-Dproperty.name=value`) or programmatically via `System.setProperty()`.

## Table of Contents
- [Graphics and Rendering](#graphics-and-rendering)
- [Hit-Testing](#hit-testing)
- [Font Configuration](#font-configuration)
- [WebAssembly (WASM)](#webassembly-wasm)
- [Audio Configuration](#audio-configuration)
- [Logging and Debugging](#logging-and-debugging)
- [Standard AWT Properties](#standard-awt-properties)

---

## Graphics and Rendering

### `me.mdbell.awtea.gfx.backend`

- **Type**: String (enum)
- **Default**: Auto-selected (priority: WASM > Software)
- **Valid Values**: 
  - `"wasm"` or `"webassembly"` - Use WebAssembly backend
  - `"software"` or `"java"` - Use pure Java software renderer
- **Description**: Forces awtea to use a specific rendering backend instead of the default auto-selection. When not set, awtea tries backends in priority order (WASM first, then Software).
- **Performance Impact**: WASM backend typically provides better performance than pure Java software rendering.
- **Code Location**: `awtea-graphics/src/main/java/me/mdbell/awtea/gfx/SurfaceBackendFactory.java`
- **Since**: v0.1.0

**Example:**
```bash
# Force software renderer
-Dme.mdbell.awtea.gfx.backend=software

# Force WASM renderer
-Dme.mdbell.awtea.gfx.backend=wasm
```

---

## Hit-Testing

### `me.mdbell.awtea.hit_test.strategy`

- **Type**: String (enum)
- **Default**: `"auto"` (automatically enables GPU picking when WebGL is available)
- **Valid Values**: 
  - `"tree_walk"` - Force traditional recursive tree traversal (O(n) complexity)
  - `"picking_buffer"` - Force GPU-based picking buffer (O(1) complexity, WebGL only)
  - `"auto"` - Automatically select picking buffer if WebGL is available, otherwise use tree walk
- **Description**: Controls the component hit-testing strategy used for mouse event dispatch. By default, GPU-based picking is **automatically enabled** when WebGL backend is detected (see `THeavyCanvas.initializeWebGLPickingStrategy()`). GPU-based picking provides O(1) hit-testing by rendering components to an off-screen buffer with unique ID colors, then reading the pixel at the mouse position. This is significantly faster for complex UIs with deep component hierarchies (>100 components).
- **Performance Impact**: 
  - Tree-walk: O(n) per hit-test, no memory overhead
  - Picking buffer: O(1) per hit-test after initial build, uses canvas_width × canvas_height × 4 bytes of VRAM
- **Code Location**: 
  - `awtea-classlib/src/main/java/me/mdbell/awtea/classlib/java/awt/awtea/TEventManager.java`
  - `awtea-classlib/src/main/java/me/mdbell/awtea/classlib/java/awt/THeavyCanvas.java` (auto-initialization)
- **Since**: v0.3.0
- **See Also**: [HIT_PICKING.md](HIT_PICKING.md) for detailed architecture documentation

**Example:**
```bash
# Force GPU-based picking (requires WebGL)
-Dme.mdbell.awtea.hit_test.strategy=picking_buffer

# Force tree-walk strategy (disable GPU picking, always available, lower memory)
-Dme.mdbell.awtea.hit_test.strategy=tree_walk

# Auto-select based on WebGL availability (default behavior)
-Dme.mdbell.awtea.hit_test.strategy=auto
```

### `me.mdbell.awtea.hit_test.debug_render`

- **Type**: Boolean
- **Default**: `false`
- **Valid Values**: `true`, `false`
- **Description**: Enables debug visualization mode for the GPU picking buffer. When enabled, the screen displays a color-coded representation of the picking buffer instead of normal rendering. Each component is rendered with a unique HSL color based on its ID, making it easy to visualize which components occupy which screen regions. This is useful for debugging hit-testing issues, verifying component bounds, and understanding the picking buffer's contents.
- **Visual Effect**: 
  - Each component gets a unique vivid color using golden ratio distribution for maximum distinction
  - Background/empty regions appear black (component ID 0)
  - Colors update when layout changes or components are added/removed
- **Performance Impact**: Minimal - only affects rendering output when enabled, doesn't change hit-testing logic
- **Requires**: GPU picking buffer must be enabled (`me.mdbell.awtea.hit_test.strategy=picking_buffer` or `auto` with WebGL)
- **Code Location**: `awtea-graphics/src/main/java/me/mdbell/awtea/gfx/webgl/WebGLSurfaceBackend.java`
- **Since**: v0.3.0

**Example:**
```bash
# Enable debug visualization (requires WebGL + picking buffer)
-Dme.mdbell.awtea.hit_test.debug_render=true

# Typical debugging setup
-Dme.mdbell.awtea.hit_test.strategy=picking_buffer -Dme.mdbell.awtea.hit_test.debug_render=true
```

---

## Font Configuration

### `me.mdbell.awtea.font.renderer`

- **Type**: String (enum)
- **Default**: `"raster"`
- **Valid Values**: 
  - `"raster"` - Use raster font renderer (default and currently only option)
- **Description**: Configures which font rendering strategy to use. Currently only the raster renderer is implemented, which rasterizes TrueType fonts to bitmaps. Future versions may support additional rendering strategies such as SDF (Signed Distance Field), canvas-based, or vector rendering.
- **Code Location**: `awtea-graphics/src/main/java/me/mdbell/awtea/font/FontRendererFactory.java:36`
- **Since**: v0.1.0

**Example:**
```bash
-Dme.mdbell.awtea.font.renderer=raster
```

### `me.mdbell.awtea.font.supersample`

- **Type**: Integer
- **Default**: `4`
- **Valid Values**: `1`, `2`, `3`, `4`
- **Description**: Controls the supersampling factor for the raster font renderer. Higher values produce smoother text at the cost of increased memory usage and processing time. A value of 4 means rendering at 4x resolution and then downsampling.
- **Performance Impact**: Higher values increase rendering time and memory usage but improve text quality.
- **Code Location**: `awtea-graphics/src/main/java/me/mdbell/awtea/font/FontRendererFactory.java:42`
- **Since**: v0.1.0

**Example:**
```bash
# High quality (default)
-Dme.mdbell.awtea.font.supersample=4

# Faster rendering, lower quality
-Dme.mdbell.awtea.font.supersample=2
```

### `me.mdbell.awtea.font.base_url`

- **Type**: String (URL path)
- **Default**: `"fonts/"`
- **Valid Values**: Any valid URL path (will be normalized to end with `/`)
- **Description**: Specifies the base URL path from which font files will be loaded. This allows customization of font file locations and integration with different deployment structures. The URL is automatically normalized to ensure it ends with a forward slash.
- **Code Location**: `awtea-graphics/src/main/java/me/mdbell/awtea/font/FontLoader.java:44`
- **Since**: v0.1.0

**Example:**
```bash
# Use a CDN for fonts
-Dme.mdbell.awtea.font.base_url=https://cdn.example.com/fonts/

# Use a relative path
-Dme.mdbell.awtea.font.base_url=assets/fonts/
```

### `me.mdbell.awtea.font.subpixel`

- **Type**: Boolean
- **Default**: `false`
- **Valid Values**: `true`, `false`
- **Description**: Enables sub-pixel font rendering (LCD/ClearType-style) for improved text clarity on LCD displays with horizontal RGB stripe layout. When enabled, each color channel (R, G, B) is sampled independently at slightly offset horizontal positions, taking advantage of the physical sub-pixel arrangement to increase apparent horizontal resolution. This significantly improves sharpness and readability, especially for smaller font sizes. Defaults to `false` for full backward compatibility.
- **Performance Impact**: Minimal performance impact. Sub-pixel rendering uses the same supersampling approach but applies independent sampling per color channel during the downsample phase.
- **Display Requirements**: Works best on LCD displays with horizontal RGB stripe layout (R-G-B from left to right). May appear colored on other display types or pixel layouts (BGR, vertical stripes, PenTile, etc.).
- **Code Location**: `awtea-graphics/src/main/java/me/mdbell/awtea/font/FontRendererFactory.java:49`
- **Since**: v0.2.0

**Example:**
```bash
# Enable sub-pixel rendering for sharper text on LCD displays
-Dme.mdbell.awtea.font.subpixel=true

# Disable sub-pixel rendering (default)
-Dme.mdbell.awtea.font.subpixel=false
```

---

## WebAssembly (WASM)

### `me.mdbell.awtea.wasm.module_path`

- **Type**: String (file path)
- **Default**: `"build/wasm/awt_raster.wasm"`
- **Valid Values**: Any valid path to a WASM module file
- **Description**: Specifies the location of the WebAssembly rasterization module. This path is used when loading the WASM backend for high-performance graphics operations.
- **Code Location**: `awtea-graphics/src/main/java/me/mdbell/awtea/gfx/wasm/WasmSurfaceBackend.java:8`
- **Since**: v0.1.0

**Example:**
```bash
# Use a custom WASM module location
-Dme.mdbell.awtea.wasm.module_path=custom/path/awt_raster.wasm
```

### `me.mdbell.awtea.wasm.surface_cache_size`

- **Type**: Integer
- **Default**: `100`
- **Valid Values**: Any positive integer
- **Description**: Controls the size of the LRU (Least Recently Used) cache for WASM surfaces. Larger values can improve performance by reducing surface recreation but will use more memory. Invalid values are silently ignored and the default is used.
- **Performance Impact**: Higher values use more memory but can reduce allocation overhead for frequently used surface sizes.
- **Code Location**: `awtea-graphics/src/main/java/me/mdbell/awtea/gfx/wasm/WasmSurfaceBackend.java:54`
- **Since**: v0.1.0

**Example:**
```bash
# Increase cache for better performance
-Dme.mdbell.awtea.wasm.surface_cache_size=200

# Reduce cache to save memory
-Dme.mdbell.awtea.wasm.surface_cache_size=50
```

---

## Audio Configuration

### `me.mdbell.awtea.sound.pcm.buffer_size`

- **Type**: Integer
- **Default**: `sample_rate * channels` (e.g., 88200 for 44.1kHz stereo)
- **Valid Values**: Any positive integer
- **Description**: Global fallback for PCM audio line buffer sizes. This value is used when no more specific size replacement matches. More specific properties (with size, rate, or channel filters) take precedence over this global setting. Subject to min/max constraints. Invalid values (non-positive or non-numeric) are silently ignored.
- **Performance Impact**: Larger buffers provide more tolerance for timing variations but increase end-to-end latency. Smaller buffers reduce latency but require more precise timing.
- **Code Location**: `awtea-sound/src/main/java/me/mdbell/awtea/sound/AudioContextLine.java:35`
- **Since**: v0.1.0

**Example:**
```bash
# Use 176400 samples as fallback for any unmatched buffer sizes
-Dme.mdbell.awtea.sound.pcm.buffer_size=176400
```

### `me.mdbell.awtea.sound.pcm.buffer_size.<size>`

- **Type**: Integer (property suffix is the requested buffer size)
- **Default**: Not set (no replacement)
- **Valid Values**: Any positive integer
- **Description**: Replaces a specific requested buffer size with a different value. When an audio line is opened with a buffer size matching `<size>`, it will be replaced with the configured value. This allows targeted adjustment of problematic buffer sizes without affecting all audio lines. Takes precedence over the global fallback. Subject to min/max constraints.
- **Performance Impact**: Allows fine-tuning specific buffer sizes that may cause issues.
- **Code Location**: `awtea-sound/src/main/java/me/mdbell/awtea/sound/AudioContextLine.java:53`
- **Since**: v0.1.0

**Example:**
```bash
# Replace 4096-sample buffers with 1024 samples
-Dme.mdbell.awtea.sound.pcm.buffer_size.4096=1024

# Make 8192-sample buffers larger
-Dme.mdbell.awtea.sound.pcm.buffer_size.8192=16384
```

### `me.mdbell.awtea.sound.pcm.buffer_size.<size>.<rate>`

- **Type**: Integer (property suffix is buffer size + sample rate)
- **Default**: Not set (no replacement)
- **Valid Values**: Any positive integer
- **Description**: Replaces a specific requested buffer size only when the audio format has a matching sample rate. This provides more targeted control than size-only replacement. Takes precedence over size-only replacements and global fallback. Sample rate is rounded to nearest integer. Subject to min/max constraints.
- **Performance Impact**: Allows different buffer size adjustments for different sample rates (e.g., 44100 Hz vs 48000 Hz).
- **Code Location**: `awtea-sound/src/main/java/me/mdbell/awtea/sound/AudioContextLine.java:53`
- **Since**: v0.1.0

**Example:**
```bash
# Replace 4096-sample buffers with 2048 samples, but only for 44.1kHz audio
-Dme.mdbell.awtea.sound.pcm.buffer_size.4096.44100=2048

# Different replacement for 48kHz audio
-Dme.mdbell.awtea.sound.pcm.buffer_size.4096.48000=3072
```

### `me.mdbell.awtea.sound.pcm.buffer_size.<size>.<rate>.<channels>`

- **Type**: Integer (property suffix is buffer size + sample rate + channel count)
- **Default**: Not set (no replacement)
- **Valid Values**: Any positive integer
- **Description**: Replaces a specific requested buffer size only when the audio format matches both sample rate and channel count. This is the most specific filter available and has highest priority. Takes precedence over all other buffer size properties. Sample rate is rounded to nearest integer. Subject to min/max constraints.
- **Performance Impact**: Allows very precise buffer size control for specific audio configurations.
- **Code Location**: `awtea-sound/src/main/java/me/mdbell/awtea/sound/AudioContextLine.java:53`
- **Since**: v0.1.0

**Example:**
```bash
# Replace 4096-sample buffers with 1024 samples for 44.1kHz stereo only
-Dme.mdbell.awtea.sound.pcm.buffer_size.4096.44100.2=1024

# Different replacement for 44.1kHz mono
-Dme.mdbell.awtea.sound.pcm.buffer_size.4096.44100.1=2048

# And for 48kHz stereo
-Dme.mdbell.awtea.sound.pcm.buffer_size.4096.48000.2=1536
```

### `me.mdbell.awtea.sound.pcm.buffer_size.min`

- **Type**: Integer
- **Default**: No minimum (unconstrained)
- **Valid Values**: Any positive integer
- **Description**: Enforces a minimum buffer size for PCM audio lines. After all size replacements are applied, if the resulting buffer size is smaller than this value, it will be increased to meet the minimum. This ensures adequate buffering to prevent underruns. Applied last after all other properties. Invalid values are silently ignored.
- **Performance Impact**: Prevents overly small buffers that could cause audio glitches, at the cost of slightly increased latency.
- **Code Location**: `awtea-sound/src/main/java/me/mdbell/awtea/sound/AudioContextLine.java:41`
- **Since**: v0.1.0

**Example:**
```bash
# Ensure all buffers are at least 22050 samples
-Dme.mdbell.awtea.sound.pcm.buffer_size.min=22050
```

### `me.mdbell.awtea.sound.pcm.buffer_size.max`

- **Type**: Integer
- **Default**: No maximum (unconstrained)
- **Valid Values**: Any positive integer
- **Description**: Enforces a maximum buffer size for PCM audio lines. After all size replacements are applied, if the resulting buffer size is larger than this value, it will be reduced to meet the maximum. This limits latency and memory usage. Applied last after all other properties. Invalid values are silently ignored.
- **Performance Impact**: Caps latency and memory usage, but may increase the risk of audio underruns if the maximum is too restrictive.
- **Code Location**: `awtea-sound/src/main/java/me/mdbell/awtea/sound/AudioContextLine.java:47`
- **Since**: v0.1.0

**Example:**
```bash
# Limit all buffers to at most 176400 samples
-Dme.mdbell.awtea.sound.pcm.buffer_size.max=176400
```

### Buffer Size Configuration Priority

When multiple buffer size properties are configured, they are applied in the following priority order (highest to lowest):

1. **Size + Rate + Channel Replacement** (`me.mdbell.awtea.sound.pcm.buffer_size.<size>.<rate>.<channels>`) - Most specific, highest priority
2. **Size + Rate Replacement** (`me.mdbell.awtea.sound.pcm.buffer_size.<size>.<rate>`) - Rate-specific
3. **Size-Only Replacement** (`me.mdbell.awtea.sound.pcm.buffer_size.<size>`) - Size-specific
4. **Global Fallback** (`me.mdbell.awtea.sound.pcm.buffer_size`) - Lowest priority, used when no specific match
5. **Requested Size** - The size provided via `open(AudioFormat, int)` or calculated default (only if none of the above match)
6. **Min/Max Constraints** - Applied last to clamp the final value

**Example:**
```bash
# Comprehensive buffer size configuration
-Dme.mdbell.awtea.sound.pcm.buffer_size=8192 \
-Dme.mdbell.awtea.sound.pcm.buffer_size.min=2048 \
-Dme.mdbell.awtea.sound.pcm.buffer_size.max=176400 \
-Dme.mdbell.awtea.sound.pcm.buffer_size.4096=8192 \
-Dme.mdbell.awtea.sound.pcm.buffer_size.4096.44100=2048 \
-Dme.mdbell.awtea.sound.pcm.buffer_size.4096.44100.2=1024

# In this example:
# - Any 4096-sample 44.1kHz stereo buffer → 1024 samples (most specific - wins!)
# - Any other 4096-sample 44.1kHz buffer → 2048 samples
# - Any other 4096-sample buffer → 8192 samples
# - Any other unmatched buffer → 8192 samples (global fallback)
# - All buffers are clamped between 2048 and 176400 samples
```

---

## Logging and Debugging

### `me.mdbell.awtea.log.level`

- **Type**: String (enum)
- **Default**: `INFO`
- **Valid Values**: `ERROR`, `WARN`, `INFO`, `DEBUG` (case-insensitive)
- **Description**: Sets the global logging level for all awtea components. This controls which log messages are emitted to the console. Invalid values will print a warning and fall back to INFO level.
- **Code Location**: `awtea-util/src/main/java/me/mdbell/awtea/util/logging/LoggerFactory.java:24`
- **Since**: v0.1.0

**Example:**
```bash
# Enable debug logging
-Dme.mdbell.awtea.log.level=DEBUG

# Only show errors
-Dme.mdbell.awtea.log.level=ERROR
```

---

## Standard AWT Properties

awtea supports standard Java AWT system properties for font configuration, maintaining compatibility with the AWT specification.

### Font System Properties

- **Type**: String (font specification)
- **Format**: `"<name>-<style>-<size>"` (e.g., `"Dialog-BOLD-12"`)
- **Valid Styles**: `PLAIN`, `BOLD`, `ITALIC`, `BOLDITALIC`
- **Valid Names**: Standard font family names (Dialog, SansSerif, Serif, Monospaced, etc.)
- **Description**: Any system property can be used as a font specification via `Font.getFont(String propertyName)`. This follows the standard AWT API pattern where the property name is looked up and its value is decoded as a font specification.
- **Code Location**: `awtea-classlib/src/main/java/me/mdbell/awtea/classlib/java/awt/TFont.java:257`
- **Since**: v0.1.0

**Example:**
```bash
# Define a custom font via system property
-Dmy.app.title.font=Dialog-BOLD-16
-Dmy.app.body.font=SansSerif-PLAIN-12
```

**Usage in code:**
```java
// Retrieve font from system property
Font titleFont = Font.getFont("my.app.title.font", new Font("Dialog", Font.PLAIN, 12));
```

---

## Best Practices

1. **Property Naming**: All awtea-specific properties use the `me.mdbell.awtea.*` prefix to avoid conflicts with other libraries.

2. **Validation**: Most properties have built-in validation with fallback to sensible defaults when invalid values are provided.

3. **Performance Tuning**: Consider adjusting `me.mdbell.awtea.font.supersample` and `me.mdbell.awtea.wasm.surface_cache_size` based on your application's memory and performance requirements.

4. **Debugging**: Set `me.mdbell.awtea.log.level=DEBUG` when troubleshooting issues to get detailed diagnostic information.

5. **Testing**: When developing or testing, you may want to force specific backends using `me.mdbell.awtea.gfx.backend` to ensure consistent behavior.

---

## References

- [Java System Properties Documentation](https://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html)
- [AWT System Properties](https://docs.oracle.com/javase/8/docs/technotes/guides/awt/AWT_Native_Interface.html)
- [TeaVM System Properties](https://teavm.org/docs/runtime/system-properties.html)

---

## See Also

- [Rendering Backends](RENDERING_BACKENDS.md) - Details on different rendering backends
- [Font Rendering Architecture](FONT_RENDERING_ARCHITECTURE.md) - Font rendering system design
- [Font Loading Strategy](FONT_LOADING.md) - How fonts are loaded at runtime
- [Logging](LOGGING.md) - Logging system documentation
