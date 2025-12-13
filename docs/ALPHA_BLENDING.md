# Alpha Blending Support

## Overview

Alpha blending support has been added to the Java software rasterizer, enabling transparency and semi-transparent rendering in awtea applications.

## Features

- **Porter-Duff Compositing Rules**: Supports all standard alpha compositing modes
- **Per-Pixel Alpha Blending**: Blend source and destination pixels with configurable alpha values
- **Performance Optimized**: Fast-path for opaque rendering when blending is not needed
- **Full Format Support**: Works with all pixel formats (ARGB, RGB, RGBA, ABGR, BGR)

## Supported Compositing Modes

The following Porter-Duff compositing rules are implemented:

- `CLEAR` - Clear destination (alpha = 0)
- `SRC` - Copy source to destination, replacing destination
- `DST` - Leave destination unchanged
- `SRC_OVER` - Source over destination (default blending)
- `DST_OVER` - Destination over source
- `SRC_IN` - Source where destination is opaque
- `DST_IN` - Destination where source is opaque
- `SRC_OUT` - Source where destination is transparent
- `DST_OUT` - Destination where source is transparent
- `SRC_ATOP` - Source over destination, only where destination is opaque
- `DST_ATOP` - Destination over source, only where source is opaque
- `XOR` - Source xor destination

## API Usage

### Basic Alpha Blending

```java
import me.mdbell.awtea.classlib.java.awt.TGraphics2D;
import me.mdbell.awtea.gfx.TAlphaComposite;
import java.awt.*;

public void paint(Graphics g) {
    TGraphics2D g2d = (TGraphics2D) g;
    
    // Set semi-transparent rendering (50% opacity)
    g2d.setComposite(TAlphaComposite.getInstance(TAlphaComposite.SRC_OVER, 0.5f));
    
    // Draw with 50% transparency
    g2d.setColor(Color.RED);
    g2d.fillRect(10, 10, 100, 100);
    
    // Reset to fully opaque rendering
    g2d.setComposite(TAlphaComposite.SrcOver);
}
```

### Using Colors with Alpha

You can also use colors with alpha directly:

```java
// Create a semi-transparent color (70% opacity)
Color semiTransparentRed = new Color(255, 0, 0, 180);
g.setColor(semiTransparentRed);
g.fillRect(20, 20, 80, 80);
```

### Advanced Compositing Modes

```java
// Source In - only draw where destination is opaque
g2d.setComposite(TAlphaComposite.getInstance(TAlphaComposite.SRC_IN));

// XOR mode - useful for selection highlights
g2d.setComposite(TAlphaComposite.getInstance(TAlphaComposite.XOR, 0.8f));
```

## Implementation Details

### Architecture

The alpha blending implementation consists of three main components:

1. **TComposite/TAlphaComposite** (`awtea-graphics` module)
   - Interface and implementation for compositing rules
   - Porter-Duff blending formulas
   - Alpha value management

2. **SoftwareRasterizer** (`awtea-graphics` module)
   - Per-pixel blending logic
   - Format-agnostic color conversion
   - Performance-optimized fast paths

3. **TSurfaceRasterizerGraphics** (`awtea-classlib` module)
   - Graphics2D API implementation
   - Composite state management
   - Command buffering and dispatch

### Performance Considerations

- **Fast Path**: When composite is `SRC` with alpha=1.0, blending is skipped entirely
- **Format Conversion**: Colors are converted to ARGB for blending, then back to the surface format
- **Caching**: Composite state is cached to avoid redundant command generation

### Blending Formula (SRC_OVER)

For the most common SRC_OVER mode:

```
outAlpha = srcAlpha + dstAlpha * (1 - srcAlpha)
outColor = (srcColor * srcAlpha + dstColor * dstAlpha * (1 - srcAlpha)) / outAlpha
```

## Example

See the `AlphaBlendingDemoPanel` in the `gui-demo` example for a working demonstration:

```bash
cd examples/gui-demo
../../gradlew generateJavaScript
# Open build/dist/index.html in a browser
```

## Limitations

- Transform support is currently limited to translation (no rotation/scale with alpha blending)
- Blending is performed in software and may be slower than hardware-accelerated alternatives
- This feature is specific to the software rasterizer backend

## Configuration

To ensure you're using the software rasterizer with alpha blending support:

```bash
-Dme.mdbell.awtea.gfx.backend=software
```

Or in Java:

```java
System.setProperty("me.mdbell.awtea.gfx.backend", "software");
```

## Future Enhancements

Potential improvements for future versions:

- Hardware-accelerated alpha blending via WebGL backend
- Support for more complex blending modes
- Alpha blending with full affine transforms (rotation, scale, shear)
- Optimization using SIMD operations where available
