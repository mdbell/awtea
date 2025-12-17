package me.mdbell.awtea.font;

import me.mdbell.awtea.util.GlyphRasterizer;

/**
 * A rasterization-based implementation of {@link FontRenderer}.
 * 
 * <p>This renderer converts TrueType font outlines to pixel-based glyphs
 * using scanline rasterization with supersampling for anti-aliasing.
 * It caches rendered glyphs for performance.
 * 
 * <p>This is the default renderer and is suitable for most use cases.
 * It provides high-quality output with configurable supersampling.
 * 
 * @see FontRenderer
 * @see GlyphRasterizer
 */
public class RasterFontRenderer implements FontRenderer {
	
	private static final int DEFAULT_SUPERSAMPLE = 4;
	private final int supersample;
	private final boolean subpixelRendering;
	
	/**
	 * Create a new RasterFontRenderer with default supersampling (4x) and no sub-pixel rendering.
	 */
	public RasterFontRenderer() {
		this(DEFAULT_SUPERSAMPLE, false);
	}
	
	/**
	 * Create a new RasterFontRenderer with the specified supersampling factor and no sub-pixel rendering.
	 * 
	 * @param supersample the supersampling factor (1 = no AA, 2-4 recommended)
	 */
	public RasterFontRenderer(int supersample) {
		this(supersample, false);
	}
	
	/**
	 * Create a new RasterFontRenderer with the specified supersampling factor and sub-pixel rendering setting.
	 * 
	 * @param supersample the supersampling factor (1 = no AA, 2-4 recommended)
	 * @param subpixelRendering whether to enable sub-pixel rendering (LCD/ClearType-style)
	 */
	public RasterFontRenderer(int supersample, boolean subpixelRendering) {
		if (supersample < 1) {
			throw new IllegalArgumentException("supersample must be >= 1");
		}
		this.supersample = supersample;
		this.subpixelRendering = subpixelRendering;
	}
	
	@Override
	public void renderGlyph(TrueTypeFont font, int glyphId, RasterTarget target,
	                       float sizePx, int x, int y, int argb) {
		// Delegate to GlyphRasterizer - it already has the implementation
		GlyphRasterizer.RasterTarget adaptedTarget = adaptTarget(target);
		GlyphRasterizer.drawGlyph(font, glyphId, adaptedTarget, sizePx, x, y, argb, supersample, subpixelRendering);
	}
	
	@Override
	public void renderString(TrueTypeFont font, String text, RasterTarget target,
	                        float sizePx, int x, int y, int argb) {
		// Delegate to GlyphRasterizer - it already has the implementation
		GlyphRasterizer.RasterTarget adaptedTarget = adaptTarget(target);
		GlyphRasterizer.drawString(font, text, adaptedTarget, sizePx, x, y, argb, supersample, subpixelRendering);
	}
	
	@Override
	public int measureString(TrueTypeFont font, String text, float sizePx) {
		// Delegate to GlyphRasterizer - it already has the implementation
		return GlyphRasterizer.measureString(font, text, sizePx);
	}
	
	@Override
	public void clearCache() {
		GlyphRasterizer.clearCache();
	}
	
	/**
	 * Adapt our RasterTarget interface to GlyphRasterizer.RasterTarget.
	 * This allows us to reuse the existing GlyphRasterizer implementation
	 * while providing our own interface.
	 */
	private GlyphRasterizer.RasterTarget adaptTarget(RasterTarget target) {
		return new GlyphRasterizer.RasterTarget() {
			@Override
			public int getWidth() {
				return target.getWidth();
			}
			
			@Override
			public int getHeight() {
				return target.getHeight();
			}
			
			@Override
			public void setRGB(int x, int y, int argb) {
				target.setRGB(x, y, argb);
			}
			
			@Override
			public int getRGB(int x, int y) {
				return target.getRGB(x, y);
			}
		};
	}
}
