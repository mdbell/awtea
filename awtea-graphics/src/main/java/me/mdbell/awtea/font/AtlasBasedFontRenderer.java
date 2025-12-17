package me.mdbell.awtea.font;

import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.gfx.SurfaceBackend;
import me.mdbell.awtea.util.GlyphRasterizer;
import org.teavm.jso.typedarrays.Int32Array;
import org.teavm.jso.typedarrays.Uint8ClampedArray;

/**
 * A font renderer implementation that uses a persistent glyph atlas for caching.
 * 
 * <p>This renderer reduces memory allocation pressure by maintaining a persistent
 * texture atlas that stores pre-rasterized glyphs. Instead of creating temporary
 * surfaces for each glyph render, glyphs are rasterized once into the atlas and
 * then copied from the atlas to the target surface.
 * 
 * <p>This is especially beneficial for:
 * <ul>
 *   <li>WASM and software backends where surface allocation is expensive</li>
 *   <li>Scenarios with frequent text rendering</li>
 *   <li>Applications that reuse the same glyphs repeatedly</li>
 * </ul>
 * 
 * <p><b>Note:</b> This renderer works directly with Surface pixel data, bypassing
 * the RasterTarget abstraction for better performance.
 * 
 * @see GlyphAtlas
 * @see FontRenderer
 */
public class AtlasBasedFontRenderer implements FontRenderer {
	
	private final GlyphAtlas atlas;
	private final int supersample;
	private final boolean subpixelRendering;
	
	private static final int DEFAULT_SUPERSAMPLE = 4;
	
	/**
	 * Create a new AtlasBasedFontRenderer with default supersampling (4x) and no sub-pixel rendering.
	 * 
	 * @param backend the surface backend to use for atlas creation
	 */
	public AtlasBasedFontRenderer(SurfaceBackend backend) {
		this(backend, DEFAULT_SUPERSAMPLE, false);
	}
	
	/**
	 * Create a new AtlasBasedFontRenderer with the specified supersampling factor and no sub-pixel rendering.
	 * 
	 * @param backend the surface backend to use for atlas creation
	 * @param supersample the supersampling factor (1 = no AA, 2-4 recommended)
	 */
	public AtlasBasedFontRenderer(SurfaceBackend backend, int supersample) {
		this(backend, supersample, false);
	}
	
	/**
	 * Create a new AtlasBasedFontRenderer with the specified supersampling factor and sub-pixel rendering setting.
	 * 
	 * @param backend the surface backend to use for atlas creation
	 * @param supersample the supersampling factor (1 = no AA, 2-4 recommended)
	 * @param subpixelRendering whether to enable sub-pixel rendering (LCD/ClearType-style)
	 */
	public AtlasBasedFontRenderer(SurfaceBackend backend, int supersample, boolean subpixelRendering) {
		if (backend == null) {
			throw new IllegalArgumentException("backend must not be null");
		}
		if (supersample < 1) {
			throw new IllegalArgumentException("supersample must be >= 1");
		}
		
		this.atlas = new GlyphAtlas(backend);
		this.supersample = supersample;
		this.subpixelRendering = subpixelRendering;
	}
	
	/**
	 * Render a single glyph to a surface.
	 * 
	 * @param font the font containing the glyph
	 * @param glyphId the glyph identifier
	 * @param target the target surface (as RasterTarget)
	 * @param sizePx the font size in pixels
	 * @param x the x-coordinate (baseline origin)
	 * @param y the y-coordinate (baseline)
	 * @param argb the color in ARGB format
	 */
	@Override
	public void renderGlyph(TrueTypeFont font, int glyphId, RasterTarget target,
	                       float sizePx, int x, int y, int argb) {
		// Adapter for RasterTarget interface - check if it's a SurfaceContainer
		if (target instanceof me.mdbell.awtea.gfx.SurfaceContainer) {
			Surface surface = ((me.mdbell.awtea.gfx.SurfaceContainer) target).getSurface();
			if (surface != null) {
				renderGlyph(font, glyphId, surface, sizePx, x, y, argb);
				return;
			}
		}
		
		// Fallback: should not happen in normal usage
		throw new UnsupportedOperationException(
			"AtlasBasedFontRenderer requires a SurfaceContainer target");
	}
	
	/**
	 * Render a string of text to a surface (via RasterTarget).
	 * 
	 * @param font the font to use
	 * @param text the text to render
	 * @param target the target surface (as RasterTarget)
	 * @param sizePx the font size in pixels
	 * @param x the x-coordinate (baseline origin)
	 * @param y the y-coordinate (baseline)
	 * @param argb the color in ARGB format
	 */
	@Override
	public void renderString(TrueTypeFont font, String text, RasterTarget target,
	                        float sizePx, int x, int y, int argb) {
		// Adapter for RasterTarget interface - check if it's a SurfaceContainer
		if (target instanceof me.mdbell.awtea.gfx.SurfaceContainer) {
			Surface surface = ((me.mdbell.awtea.gfx.SurfaceContainer) target).getSurface();
			if (surface != null) {
				renderString(font, text, surface, sizePx, x, y, argb);
				return;
			}
		}
		
		// Fallback: should not happen in normal usage
		throw new UnsupportedOperationException(
			"AtlasBasedFontRenderer requires a SurfaceContainer target");
	}
	
	/**
	 * Render a single glyph to a surface.
	 * 
	 * @param font the font containing the glyph
	 * @param glyphId the glyph identifier
	 * @param target the target surface
	 * @param sizePx the font size in pixels
	 * @param x the x-coordinate (baseline origin)
	 * @param y the y-coordinate (baseline)
	 * @param argb the color in ARGB format
	 */
	public void renderGlyph(TrueTypeFont font, int glyphId, Surface target,
	                       float sizePx, int x, int y, int argb) {
		// Get or create the glyph in the atlas
		GlyphAtlas.GlyphEntry entry = atlas.getOrCreateGlyph(font, glyphId, sizePx, argb, supersample, subpixelRendering);
		if (entry == null) {
			// Glyph could not be rendered (empty or missing)
			return;
		}
		
		// Copy the glyph from the atlas to the target
		blitGlyphFromAtlas(entry, target, x, y);
	}
	
	/**
	 * Render a string of text to a surface.
	 * 
	 * @param font the font to use
	 * @param text the text to render
	 * @param target the target surface
	 * @param sizePx the font size in pixels
	 * @param x the x-coordinate (baseline origin)
	 * @param y the y-coordinate (baseline)
	 * @param argb the color in ARGB format
	 */
	public void renderString(TrueTypeFont font, String text, Surface target,
	                        float sizePx, int x, int y, int argb) {
		if (text == null || text.isEmpty()) {
			return;
		}
		
		double scale = sizePx / font.getUnitsPerEm();
		int prevGid = -1;
		double penX = x;
		
		for (int i = 0; i < text.length(); ) {
			int cp = text.codePointAt(i);
			int advanceChars = Character.charCount(cp);
			
			// Check for variation selector
			int variationSelector = 0;
			if (i + advanceChars < text.length()) {
				int nextCp = text.codePointAt(i + advanceChars);
				if (TrueTypeFont.isVariationSelector(nextCp)) {
					variationSelector = nextCp;
					advanceChars += Character.charCount(nextCp);
				}
			}
			
			int gid;
			if (variationSelector != 0) {
				gid = font.glyphForCodePointWithVariation(cp, variationSelector);
			} else {
				gid = font.glyphForCodePoint(cp);
			}
			
			// Apply kerning
			int kernUnits = (prevGid >= 0) ? font.getKerningUnits(prevGid, gid) : 0;
			double kernPx = kernUnits * scale;
			penX += kernPx;
			
			// Render the glyph
			int pxX = (int) Math.round(penX);
			renderGlyph(font, gid, target, sizePx, pxX, y, argb);
			
			// Advance pen position
			int advanceWidthUnits = (gid <= 0) 
				? (int) (font.getUnitsPerEm() * 0.6)
				: font.getAdvanceWidthUnits(gid);
			double advancePx = advanceWidthUnits * scale;
			penX += advancePx;
			
			prevGid = gid;
			i += advanceChars;
		}
	}
	
	/**
	 * Measure the width of a text string in pixels.
	 * 
	 * @param font the font to use
	 * @param text the text to measure
	 * @param sizePx the font size in pixels
	 * @return the width of the text in pixels
	 */
	@Override
	public int measureString(TrueTypeFont font, String text, float sizePx) {
		// Delegate to GlyphRasterizer for measurement (doesn't involve rendering)
		return GlyphRasterizer.measureString(font, text, sizePx);
	}
	
	/**
	 * Clear the glyph cache.
	 */
	@Override
	public void clearCache() {
		atlas.destroy();
	}
	
	/**
	 * Blit a glyph from the atlas to the target surface.
	 * 
	 * @param entry the glyph entry in the atlas
	 * @param target the target surface
	 * @param x the destination x coordinate
	 * @param y the destination y coordinate (baseline)
	 */
	private void blitGlyphFromAtlas(GlyphAtlas.GlyphEntry entry, Surface target, int x, int y) {
		// Calculate destination rectangle
		// The glyph entry contains offsets from the baseline
		int destX = x + entry.getOffsetX();
		int destY = y + entry.getOffsetY();
		
		int srcX = entry.getAtlasX();
		int srcY = entry.getAtlasY();
		int width = entry.getWidth();
		int height = entry.getHeight();
		
		// Perform the blit - work directly with pixel data
		Uint8ClampedArray srcPixelArray = entry.getAtlasSurface().getPixelData();
		Uint8ClampedArray dstPixelArray = target.getPixelData();
		
		if (srcPixelArray == null || dstPixelArray == null) {
			return; // Cannot blit without pixel data
		}
		
		// Convert to int arrays for efficient pixel access
		Int32Array srcData = new Int32Array(
			srcPixelArray.getBuffer(),
			srcPixelArray.getByteOffset(),
			srcPixelArray.getLength() / 4
		);
		Int32Array dstData = new Int32Array(
			dstPixelArray.getBuffer(),
			dstPixelArray.getByteOffset(),
			dstPixelArray.getLength() / 4
		);
		
		int srcWidth = entry.getAtlasSurface().getWidth();
		int targetWidth = target.getWidth();
		int targetHeight = target.getHeight();
		
		for (int dy = 0; dy < height; dy++) {
			int targetY = destY + dy;
			if (targetY < 0 || targetY >= targetHeight) {
				continue;
			}
			
			int srcRowOffset = (srcY + dy) * srcWidth + srcX;
			
			for (int dx = 0; dx < width; dx++) {
				int targetX = destX + dx;
				if (targetX < 0 || targetX >= targetWidth) {
					continue;
				}
				
				int srcIdx = srcRowOffset + dx;
				int dstIdx = targetY * targetWidth + targetX;
				
				// Read ARGB from atlas
				int srcARGB = srcData.get(srcIdx);
				int a = (srcARGB >>> 24) & 0xFF;
				
				if (a == 0) {
					continue; // Skip fully transparent pixels
				}
				
				// Alpha blend with destination
				if (a == 255) {
					// Fully opaque, direct copy
					dstData.set(dstIdx, srcARGB);
				} else {
					// Alpha blend
					int dstARGB = dstData.get(dstIdx);
					
					int srcR = (srcARGB >>> 16) & 0xFF;
					int srcG = (srcARGB >>> 8) & 0xFF;
					int srcB = srcARGB & 0xFF;
					
					int dstA = (dstARGB >>> 24) & 0xFF;
					int dstR = (dstARGB >>> 16) & 0xFF;
					int dstG = (dstARGB >>> 8) & 0xFF;
					int dstB = dstARGB & 0xFF;
					
					int outA = a + ((dstA * (255 - a)) / 255);
					int outR = (srcR * a + dstR * (255 - a)) / 255;
					int outG = (srcG * a + dstG * (255 - a)) / 255;
					int outB = (srcB * a + dstB * (255 - a)) / 255;
					
					int outARGB = (outA << 24) | (outR << 16) | (outG << 8) | outB;
					dstData.set(dstIdx, outARGB);
				}
			}
		}
	}
	
	/**
	 * Get the underlying glyph atlas.
	 * 
	 * @return the glyph atlas
	 */
	public GlyphAtlas getAtlas() {
		return atlas;
	}
}
