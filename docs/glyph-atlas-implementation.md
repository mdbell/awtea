# Glyph Atlas Implementation

## Overview

This document describes the implementation of a persistent glyph cache (texture atlas) to improve font rendering memory usage in the awtea project.

## Problem Statement

Previously, font rendering in the WASM and software backends created a large number of small temporary surfaces (one per glyph or string render), which resulted in:
- Significant memory allocation pressure
- Memory errors and crashes with high text rendering activity
- Poor performance due to constant allocation/deallocation cycles
- GC pressure from short-lived surface objects

## Solution Architecture

### Components

#### 1. GlyphAtlas (`me.mdbell.awtea.font.GlyphAtlas`)

A persistent texture atlas that manages one or more surfaces for caching rendered glyphs.

**Key Features:**
- **Persistent Surface**: Maintains a 2048x2048 atlas surface that lives for the lifetime of the renderer
- **Horizontal Packing**: Uses a simple row-based packing strategy with padding between glyphs
- **LRU Eviction**: Implements Least Recently Used eviction when the cache exceeds 512 glyphs
- **Multi-key Caching**: Caches glyphs by font, glyph ID, size, color, and supersampling factor
- **Immediate Cleanup**: Destroys temporary surfaces immediately after copying to atlas

**Memory Benefits:**
- Eliminates per-glyph surface allocations during rendering
- Reuses pre-rasterized glyphs across multiple render calls
- Only one temporary surface needed per new glyph (destroyed immediately)
- Single persistent atlas surface replaces hundreds/thousands of temporary surfaces

#### 2. GlyphEntry (`GlyphAtlas.GlyphEntry`)

Tracks the position and dimensions of a glyph within the atlas.

**Properties:**
- Atlas coordinates (x, y)
- Glyph dimensions (width, height)
- Offset from baseline (offsetX, offsetY)
- Reference to the atlas surface

#### 3. AtlasBasedFontRenderer (`me.mdbell.awtea.font.AtlasBasedFontRenderer`)

A `FontRenderer` implementation that uses the glyph atlas for rendering.

**Rendering Flow:**
1. Check atlas for cached glyph
2. If not cached, rasterize to temporary surface
3. Copy from temporary surface to atlas
4. Destroy temporary surface immediately
5. Blit from atlas to target surface

**Features:**
- Direct pixel-level operations for performance
- Alpha blending support
- Backward compatible with RasterTarget interface
- Works with both Surface and SurfaceContainer targets

#### 4. Integration Points

**FontRendererFactory** - Updated to create AtlasBasedFontRenderer by default
- Can be configured via system property: `me.mdbell.awtea.font.renderer=atlas|raster`
- Falls back to original RasterFontRenderer when set to "raster"

**FontPeer** - Enhanced to work with both Surface and RasterTarget
- Detects AtlasBasedFontRenderer and uses Surface directly
- Falls back to RasterTarget for backward compatibility

**TSurfaceRasterizerGraphics** - Updated comments to clarify memory benefits
- Still creates temporary surface per string (for layout/composition)
- But glyphs are now copied from atlas, not rasterized from scratch
- Significantly reduced memory pressure

## Performance Characteristics

### Memory Usage

**Before (per text render):**
- N temporary surfaces (one per glyph)
- Each surface: width × height × 4 bytes
- Example: "Hello" at 16px ≈ 5 surfaces × 20×20×4 = 8 KB per render

**After (per text render):**
- 1 temporary surface for the entire string
- Glyphs copied from atlas (no new allocations)
- Example: "Hello" at 16px ≈ 1 surface × 50×20×4 = 4 KB first render, ~0 KB subsequent

**Atlas Memory:**
- Single 2048×2048 ARGB surface = 16 MB
- Shared across all text rendering
- Amortized over hundreds/thousands of renders

### Performance

**Cache Hit (common case):**
1. Lookup in LRU cache: O(1)
2. Blit from atlas: O(glyph pixels)
3. No rasterization needed

**Cache Miss (first render of a glyph):**
1. Rasterize to temporary surface: O(glyph complexity)
2. Copy to atlas: O(glyph pixels)
3. Destroy temporary surface
4. Subsequent renders are cache hits

## Configuration

### System Properties

- `me.mdbell.awtea.font.renderer` - Choose renderer type
  - `atlas` (default) - Use atlas-based renderer
  - `raster` - Use original raster renderer
  
- `me.mdbell.awtea.font.supersample` - Supersampling factor (1-4, default: 4)

### Atlas Parameters (in GlyphAtlas)

- `ATLAS_WIDTH` / `ATLAS_HEIGHT` - Atlas surface dimensions (2048×2048)
- `MAX_GLYPHS` - Maximum cached glyphs before LRU eviction (512)
- `GLYPH_PADDING` - Padding between glyphs (2 pixels)

## Testing

Comprehensive test suite in `GlyphAtlasTest`:
- Atlas creation and initialization
- Glyph caching and retrieval
- Cache hit verification
- Multiple glyphs caching
- Different sizes and colors
- LRU behavior (implicitly tested through cache size)

## Future Improvements

Potential enhancements:
1. **Smarter Packing**: Use bin-packing algorithms for better space utilization
2. **Multiple Atlases**: Create additional atlases when one fills up
3. **Grayscale Atlas**: Store only alpha channel + color at render time (4× memory savings)
4. **Metrics**: Track cache hit rate, atlas utilization, memory savings
5. **Preloading**: Pre-cache common glyphs on startup
6. **Compression**: LZ4/Snappy compression for atlas in memory
7. **GPU Upload**: Direct WebGL texture upload (for WebGL backend)

## Migration Notes

The implementation is **fully backward compatible**:
- Existing code continues to work without changes
- Can opt-out via system property if needed
- RasterTarget interface still supported
- No API breaking changes

Applications automatically benefit from improved memory usage when using the default FontRendererFactory configuration.
