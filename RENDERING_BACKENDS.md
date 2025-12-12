# Rendering Backend Architecture

## Overview

The awtea project implements a flexible rendering system that supports multiple backend implementations. The architecture follows a Surface/Rasterizer pattern where surfaces represent pixel buffers and rasterizers execute drawing commands.

## Backend Types

### 1. WebGL Backend (WebGLRasterizer/WebGLSurface)

**Priority**: First choice for screen rendering

**Use Case**: Direct rendering to HTML Canvas elements using WebGL 2.0

**Characteristics**:
- Hardware-accelerated rendering
- Optimal performance for screen display
- Requires WebGL 2.0 support in the browser
- Only available for screen surfaces (requires HTMLCanvasElement)
- Uses GPU textures and shaders for all operations

**Supported Pixel Formats**: `FORMAT_INT_RGBA`

### 2. WebAssembly Backend (WasmRasterizer/WasmSurface)

**Priority**: First choice for offscreen rendering

**Use Case**: High-performance offscreen rendering using compiled C/WASM module

**Characteristics**:
- Near-native performance through WebAssembly
- Efficient for complex rendering operations
- Requires WASM support in the browser
- Uses native C rendering code compiled to WASM
- Memory is managed in WASM linear memory space

**Supported Pixel Formats**: 
- `FORMAT_INT_ARGB`
- `FORMAT_INT_RGB`
- `FORMAT_INT_BGR`

### 3. Java Software Backend (SoftwareRasterizer/SoftwareSurface)

**Priority**: Fallback for all rendering (lowest priority)

**Use Case**: Compatibility fallback when WASM is unavailable

**Characteristics**:
- Pure Java implementation
- Works in all environments
- Slower than WebGL/WASM but provides guaranteed compatibility
- Direct pixel buffer manipulation
- No external dependencies

**Supported Surface Creation Formats**:
- `FORMAT_INT_ARGB` (via TYPE_INT_ARGB)
- `FORMAT_INT_RGB` (via TYPE_INT_RGB)
- `FORMAT_INT_BGR` (via TYPE_INT_BGR)

**Note**: The SoftwareRasterizer can read and write all pixel formats (including RGBA and ABGR) through its format conversion logic when blitting between surfaces. However, only ARGB, RGB, and BGR formats can be used when creating new surfaces.

## Backend Selection

The `DefaultSurfaceBackend` class manages backend selection using a priority system:

### For Screen Surfaces
```java
Surface surface = DefaultSurfaceBackend.getDefault()
    .createScreenSurface(width, height, canvas);
```

**Priority Order**:
1. **WebGL** - Always used when a canvas element is provided

### For Offscreen Surfaces
```java
Surface surface = DefaultSurfaceBackend.getDefault()
    .createCompatibleSurface(width, height, BufferedImage.TYPE_INT_ARGB);
```

**Priority Order**:
1. **WebAssembly (WASM)** - First choice if available
2. **Java Software** - Fallback if WASM unavailable

## Surface API

All surfaces implement the `Surface` interface:

```java
public interface Surface {
    Rasterizer createRasterizer();
    void resize(int width, int height);
    int getWidth();
    int getHeight();
    Uint8ClampedArray getPixelData();
    int getFormat();
    boolean isDirty();
    void destroy();
}
```

## Rasterizer API

All rasterizers implement the `Rasterizer` interface:

```java
public interface Rasterizer {
    Rasterizer create();
    void reset();
    void rasterizeCommands(List<SurfaceCommand> cmds);
}
```

## Supported Operations

All backends support the following rendering operations through the command pattern:

- **SET_COLOR** - Set foreground/background color
- **SET_TRANSFORM** - Apply affine transformations
- **SET_CLIP_RECT** - Set clipping rectangle
- **FILL_RECT** - Fill a rectangle
- **DRAW_RECT** - Draw rectangle outline
- **CLEAR_RECT** - Clear rectangle with background color
- **DRAW_LINE** - Draw a line
- **BLIT_IMAGE** - Copy pixels from another surface (with scaling)

## Pixel Format Support

| Format | WebGL | WASM | Software |
|--------|-------|------|----------|
| FORMAT_INT_ARGB | ✗ | ✓ | ✓ |
| FORMAT_INT_RGB | ✗ | ✓ | ✓ |
| FORMAT_INT_RGBA | ✓ | ✗ | ✓ |
| FORMAT_INT_ABGR | ✗ | ✗ | ✓ |
| FORMAT_INT_BGR | ✗ | ✓ | ✓ |

**Note**: The Software backend includes format conversion logic to handle blitting between surfaces with different pixel formats, so it can read and write all pixel formats even though surface creation only supports INT_ARGB, INT_RGB, and INT_BGR.

## Performance Considerations

### WebGL Backend
- **Best**: Hardware-accelerated, ideal for screen rendering
- **Limitations**: Requires canvas context, overhead for small operations

### WASM Backend
- **Best**: Complex rendering operations, offscreen buffers
- **Limitations**: WASM module load time, limited surface pool

### Software Backend
- **Best**: Compatibility, simple operations on small surfaces
- **Limitations**: CPU-bound, slower for large surfaces or complex operations

## Usage Example

```java
// Create a BufferedImage - backend will be selected automatically
TBufferedImage image = new TBufferedImage(800, 600, 
    TBufferedImage.TYPE_INT_ARGB);

// Get graphics context - uses the surface's rasterizer
TGraphics2D g = (TGraphics2D) image.getGraphics();

// Draw operations are converted to commands and executed
g.setColor(Color.RED);
g.fillRect(10, 10, 100, 100);
g.setColor(Color.BLUE);
g.drawRect(50, 50, 200, 150);
```

## Implementation Notes

### Command Pattern
All backends use a command-based rendering system. Graphics operations are recorded as `SurfaceCommand` objects and executed in batch by the rasterizer's `rasterizeCommands()` method.

### Transform and Clip
The Software backend applies transforms by translating coordinates before drawing. Clipping is enforced by bounds checking during pixel operations.

### Dirty Tracking
Surfaces track whether they've been modified since the last read. This allows optimization for operations that need to sync pixel data (e.g., WebGL texture uploads).

### Resource Management
All surfaces implement `destroy()` for proper cleanup. Users should call this when a surface is no longer needed, especially for WebGL and WASM backends which allocate GPU/native resources.
