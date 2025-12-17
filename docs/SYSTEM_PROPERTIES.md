# awtea System Properties

This document provides comprehensive documentation for all system properties that awtea recognizes and uses. System properties can be set via command-line arguments (e.g., `-Dproperty.name=value`) or programmatically via `System.setProperty()`.

## Table of Contents
- [Graphics and Rendering](#graphics-and-rendering)
- [Font Configuration](#font-configuration)
- [WebAssembly (WASM)](#webassembly-wasm)
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
- **Code Location**: `awtea-graphics/src/main/java/me/mdbell/awtea/gfx/DefaultSurfaceBackend.java:24`
- **Since**: v0.1.0

**Example:**
```bash
# Force software renderer
-Dme.mdbell.awtea.gfx.backend=software

# Force WASM renderer
-Dme.mdbell.awtea.gfx.backend=wasm
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

### Font Loading Failure Cache

**Note:** As of v0.3.0, `FontLoader` automatically caches font loading failures to prevent repeated performance hits from retrying failed loads. When a font fails to load (e.g., network error, missing file), the failure is recorded and subsequent attempts to load the same font will fail immediately without retrying the network/resource operation.

**Key behaviors:**
- Failed font loads are cached permanently for the lifetime of the application
- The failure cache is thread-safe and uses minimal memory (only stores font names)
- Use `FontLoader.clearFailureCache()` programmatically to clear the failure cache for development/debugging
- Use `FontLoader.clearCache()` to clear both success and failure caches
- Use `FontLoader.isFailureCached(fontName)` to check if a font is in the failure cache

**Performance Impact:** Eliminates CPU and network overhead from repeated failed font loads, resulting in faster UI initialization and painting, especially in font-intensive contexts like the GUI demo.

**Code Location**: `awtea-graphics/src/main/java/me/mdbell/awtea/font/FontLoader.java`

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
