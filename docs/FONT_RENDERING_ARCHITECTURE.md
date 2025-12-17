# Font Rendering Architecture

## Overview

The awtea font rendering system has been refactored to follow a modular, backend-independent architecture inspired by Java AWT's font peer design. This document describes the new architecture and how to use and extend it.

## Architecture Components

### 1. FontRenderer Interface

The `FontRenderer` interface defines the contract for font rendering implementations. It separates the concerns of font data management from the actual rendering strategy.

```java
public interface FontRenderer {
    void renderGlyph(TrueTypeFont font, int glyphId, RasterTarget target, 
                     float sizePx, int x, int y, int argb);
    void renderString(TrueTypeFont font, String text, RasterTarget target,
                      float sizePx, int x, int y, int argb);
    int measureString(TrueTypeFont font, String text, float sizePx);
    void clearCache();
}
```

**Key Benefits:**
- Backend independence: Font data is separate from rendering implementation
- Pluggable strategies: Easy to add new rendering approaches (SDF, canvas, shaders, etc.)
- Testability: Mock renderers for testing
- Performance: Can optimize per-renderer

### 2. FontPeer

The `FontPeer` class acts as a bridge between logical fonts (`TFont`) and rendering implementations (`FontRenderer`). It coordinates font data and rendering strategy.

```java
public class FontPeer {
    private final TrueTypeFont font;
    private final FontRenderer renderer;
    
    public void renderString(String text, RasterTarget target,
                            float sizePx, int x, int y, int argb) {
        renderer.renderString(font, text, target, sizePx, x, y, argb);
    }
    
    public int measureString(String text, float sizePx) {
        return renderer.measureString(font, text, sizePx);
    }
    
    public FontMetrics getFontMetrics(float sizePx) { ... }
}
```

**Design Pattern:** This follows the Peer pattern from Java AWT, where platform-specific implementations are hidden behind a common interface.

### 3. FontRendererFactory

The `FontRendererFactory` provides centralized creation and configuration of font renderers.

```java
// Get default renderer
FontRenderer renderer = FontRendererFactory.getDefaultRenderer();

// Create specific renderer
FontRenderer rasterRenderer = FontRendererFactory.createRasterRenderer(4);

// Set custom default
FontRendererFactory.setDefaultRenderer(myCustomRenderer);
```

**Configuration via System Properties:**
- `me.mdbell.awtea.font.renderer`: Select renderer type ("raster")
- `me.mdbell.awtea.font.supersample`: Configure supersampling (1-4, default: 4)

### 4. RasterFontRenderer

The default implementation that converts TrueType outlines to rasterized glyphs with antialiasing.

```java
public class RasterFontRenderer implements FontRenderer {
    // Uses scanline rasterization with supersampling
    // Caches rendered glyphs for performance
}
```

### 5. SyntheticStyledFontRenderer

A decorator renderer that applies synthetic bold and italic styling when actual font variant files are missing.

```java
public class SyntheticStyledFontRenderer implements FontRenderer {
    // Wraps another renderer and applies synthetic styling effects
    // Used automatically when font variants are not available
}
```

**Synthetic Bold:**
- Renders each glyph multiple times with 1-pixel horizontal offset
- Creates heavier text appearance
- Not as uniform as real bold fonts, especially at small sizes

**Synthetic Italic:**
- Applies -0.2 radian shear transformation (approximately 11 degrees)
- Simulates oblique text slant
- Lacks the design refinements of true italic fonts

**Automatic Fallback:**
When you create a bold or italic font and the variant file is missing, awtea automatically:
1. Loads the plain font variant
2. Wraps it with `SyntheticStyledFontRenderer`
3. Applies synthetic styling at render time

```java
// If NotoSans-Bold.ttf is missing:
TFont bold = new TFont("NotoSans", TFont.BOLD, 12);
// Automatically uses NotoSans.ttf with synthetic bold styling
```

## Synthetic Font Styling

When a bold or italic font variant file is not available, awtea automatically applies synthetic styling to provide visually distinct text while maintaining functionality.

### Quality Comparison

| Aspect | Real Font Variants | Synthetic Styling |
|--------|-------------------|-------------------|
| Quality | Professional design, optimized hinting | Approximation of style |
| Consistency | Uniform weight/angle across all glyphs | May vary slightly |
| File Size | Requires separate font files | Uses existing font |
| Performance | Same as plain fonts | Slightly slower (extra passes) |
| Recommendation | **Preferred** for production | Acceptable fallback |

### When Synthetic Styling is Used

Synthetic styling is automatically applied when:
1. A bold or italic font is requested
2. The corresponding font variant file (e.g., `FontName-Bold.ttf`) is not found
3. The plain font file (e.g., `FontName.ttf`) is available

### Example Behavior

```java
// Scenario 1: Real font variants deployed
// Files: NotoSans.ttf, NotoSans-Bold.ttf, NotoSans-Italic.ttf, NotoSans-BoldItalic.ttf
TFont bold = new TFont("NotoSans", TFont.BOLD, 12);
// → Loads NotoSans-Bold.ttf (real bold font) ✓

// Scenario 2: Only plain font deployed
// Files: Helvetica.ttf
TFont bold = new TFont("Helvetica", TFont.BOLD, 12);
// → Loads Helvetica.ttf + applies synthetic bold styling

// Scenario 3: Mixed deployment
// Files: Roboto.ttf, Roboto-Bold.ttf (no italic variants)
TFont italic = new TFont("Roboto", TFont.ITALIC, 12);
// → Loads Roboto.ttf + applies synthetic italic styling
```

### Configuration

Synthetic styling is enabled automatically and requires no configuration. To disable it and require real font files only:

1. Deploy all required font variants
2. Font loading will fail if variants are missing (falls back to default font)

### Implementation Details

**Synthetic Bold Algorithm:**
```java
// Render glyph at position
renderGlyph(font, glyphId, target, x, y, color);
// Render again 1 pixel to the right
renderGlyph(font, glyphId, target, x + 1, y, color);
```

**Synthetic Italic Algorithm:**
```java
// Apply shear transformation: x' = x + (y * ITALIC_SHEAR)
int offsetX = x + (int)(y * -0.2f);
renderGlyph(font, glyphId, target, offsetX, y, color);
```

### Best Practices

1. **For Production**: Deploy all font variants (Plain, Bold, Italic, BoldItalic) for best quality
2. **For Development**: Rely on synthetic styling for quick prototyping
3. **For Fallback Fonts**: Synthetic styling ensures bold/italic work even with limited font files
4. **Performance**: Synthetic bold is ~2x slower per string (two render passes)

### Font Deployment Checklist

For optimal text rendering quality, deploy these variants for each font family:

- [ ] `FontName.ttf` (Plain/Regular)
- [ ] `FontName-Bold.ttf` (Bold)
- [ ] `FontName-Italic.ttf` (Italic)
- [ ] `FontName-BoldItalic.ttf` (Bold Italic)

If any variant is missing, synthetic styling will automatically be applied for that style.

## Component Relationships

```
TFont
  └─> FontPeer
        ├─> TrueTypeFont (font data)
        └─> FontRenderer (rendering strategy)
              ├─> RasterFontRenderer (default impl)
              ├─> AtlasBasedFontRenderer (optimized caching)
              └─> SyntheticStyledFontRenderer (decorator for synthetic bold/italic)
                    └─> delegates to another FontRenderer
```

## Usage Examples

### Basic Text Rendering

```java
// Create a font
TFont font = new TFont("Arial", TFont.PLAIN, 12);

// Get graphics context
TGraphics2D g = (TGraphics2D) image.getGraphics();
g.setFont(font);

// Draw text - uses the font's peer internally
g.drawString("Hello, World!", 10, 20);
```

### Custom Renderer

```java
// Create a custom renderer
class MyCustomRenderer implements FontRenderer {
    @Override
    public void renderString(TrueTypeFont font, String text, 
                            RasterTarget target, float sizePx, 
                            int x, int y, int argb) {
        // Custom rendering logic
    }
    // ... implement other methods
}

// Use custom renderer
FontRenderer myRenderer = new MyCustomRenderer();
FontRendererFactory.setDefaultRenderer(myRenderer);
```

### Per-Font Renderer

```java
// Load a font with custom renderer
TrueTypeFont ttf = TrueTypeFont.read(fontBytes);
FontRenderer customRenderer = new SDFFontRenderer(); // hypothetical
FontPeer peer = new FontPeer(ttf, customRenderer);

// Use this peer directly or create TFont with it
```

## Adding New Rendering Strategies

To add a new font rendering strategy (e.g., Signed Distance Field fonts):

1. **Implement FontRenderer interface:**

```java
public class SDFFontRenderer implements FontRenderer {
    @Override
    public void renderGlyph(TrueTypeFont font, int glyphId, 
                           RasterTarget target, float sizePx, 
                           int x, int y, int argb) {
        // Generate SDF texture
        // Render using shader
    }
    
    @Override
    public void renderString(TrueTypeFont font, String text, 
                            RasterTarget target, float sizePx,
                            int x, int y, int argb) {
        // Process string, handle kerning
        // Render each glyph using SDF
    }
    
    // ... other methods
}
```

2. **Register in FontRendererFactory:**

```java
// In FontRendererFactory.createDefaultRenderer():
switch (rendererType) {
    case "raster":
        return new RasterFontRenderer(supersample);
    case "sdf":
        return new SDFFontRenderer();
    // ...
}
```

3. **Use via configuration:**

```bash
-Dme.mdbell.awtea.font.renderer=sdf
```

## Backend Integration

The font rendering system is now backend-independent. Each rendering backend (WebGL, WASM, Software) can have its own optimized font renderer:

### WebGL Backend
- Could use texture atlases for glyph caching
- Shader-based rendering for effects
- GPU-accelerated text layout

### WASM Backend
- Could use native font rasterization
- Hardware-accelerated text rendering
- Integration with system fonts

### Software Backend
- Current rasterization approach works well
- Pure Java, no dependencies
- Guaranteed compatibility

## Performance Considerations

### Glyph Caching

The `RasterFontRenderer` caches rendered glyphs using a composite key:
- Font identity
- Glyph ID
- Size in pixels
- Supersampling factor
- Sub-pixel rendering mode

Cache size is configurable via `GlyphRasterizer.setMaxGlyphCacheEntries()`.

### Supersampling Trade-offs

| Supersample | Quality | Memory | Speed |
|-------------|---------|--------|-------|
| 1           | Low     | Low    | Fast  |
| 2           | Medium  | Medium | Medium|
| 4 (default) | High    | High   | Slow  |

Configure via system property:
```bash
-Dme.mdbell.awtea.font.supersample=2
```

### Sub-Pixel Rendering

Sub-pixel rendering (LCD/ClearType-style) is an optional enhancement that significantly improves text sharpness on LCD displays:

**How it Works:**
- Takes advantage of the physical RGB sub-pixel arrangement on LCD displays
- Each color channel (R, G, B) is sampled independently at slightly offset horizontal positions
- Increases apparent horizontal resolution by ~3x for text
- Best suited for horizontal RGB stripe layouts (R-G-B from left to right)

**Trade-offs:**

| Mode | Sharpness | Color Fringing | Display Compatibility |
|------|-----------|----------------|----------------------|
| Disabled (default) | Good | None | Universal |
| Enabled | Excellent | Minimal (on LCD) | Best on RGB LCD |

**Configuration:**
```bash
# Enable sub-pixel rendering
-Dme.mdbell.awtea.font.subpixel=true
```

**Display Considerations:**
- **RGB LCD (Horizontal):** Optimal - sharp text with minimal color fringing
- **BGR LCD:** May show reversed color fringing (BGR subpixel order detection could be added in future iterations)
- **Vertical RGB/BGR:** Not optimal for this horizontal implementation
- **OLED/PenTile:** May show color artifacts
- **Retina/Hi-DPI:** Less benefit due to already high pixel density

**Performance Impact:**
Minimal - uses the same supersampling approach but applies independent per-channel sampling during downsampling.

## Future Rendering Strategies

The modular architecture enables investigation of advanced techniques:

1. **Signed Distance Field (SDF) Fonts**
   - Scale-independent rendering
   - Effects: outline, glow, shadow
   - GPU-friendly

2. **Canvas-based Rendering**
   - Leverage browser's native text rendering
   - System font support
   - OS-native text quality

3. **Vector Font Shaders**
   - Direct outline rendering
   - Perfect scalability
   - Real-time effects

4. **Hybrid Approaches**
   - Use different renderers for different sizes
   - Fallback strategies
   - Dynamic selection based on performance

## Testing and Benchmarking

The new architecture simplifies testing:

```java
// Mock renderer for testing
class MockRenderer implements FontRenderer {
    List<RenderCall> calls = new ArrayList<>();
    
    @Override
    public void renderString(...) {
        calls.add(new RenderCall(...));
    }
}

// Use in tests
FontRenderer mockRenderer = new MockRenderer();
FontPeer peer = new FontPeer(font, mockRenderer);
// ... test and verify calls
```

For performance benchmarking, see issue #13.

## Migration Guide

### Before (Direct GlyphRasterizer usage)

```java
TrueTypeFont ttf = font.getTrueType();
GlyphRasterizer.drawString(ttf, text, target, size, x, y, color);
```

### After (Using FontPeer)

```java
font.getFontPeer().renderString(text, target, size, x, y, color);
```

The refactor maintains backward compatibility where possible. Existing code using `GlyphRasterizer` directly continues to work, but new code should use the `FontPeer` abstraction.

## References

- [AWT GlyphRenderer Design](https://github.com/openjdk/jdk/blob/master/src/java.desktop/share/classes/sun/font/GlyphRenderer.java)
- [AWT FontPeer Architecture](https://github.com/openjdk/jdk/blob/master/src/java.desktop/share/classes/java/awt/peer/FontPeer.java)
- [Distance Field Font Rendering](https://github.com/Chlumsky/msdfgen)
- [Rendering Backends Documentation](RENDERING_BACKENDS.md)
