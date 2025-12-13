package me.mdbell.awtea.font;

/**
 * Interface for rendering glyphs to a target surface.
 * This abstraction enables different rendering strategies (e.g., rasterization,
 * distance fields, canvas-based, shader-based) to be plugged in without
 * modifying the core font management code.
 * 
 * <p>Inspired by AWT's {@code sun.font.GlyphRenderer} design, this interface
 * separates the concerns of font data management (handled by {@link TrueTypeFont})
 * from the actual rendering implementation.
 * 
 * @see TrueTypeFont
 */
public interface FontRenderer {
	
	/**
	 * Render a single glyph at the specified position.
	 * 
	 * @param font the font containing the glyph data
	 * @param glyphId the glyph identifier to render
	 * @param target the surface to render to
	 * @param sizePx the font size in pixels
	 * @param x the x-coordinate (baseline origin)
	 * @param y the y-coordinate (baseline)
	 * @param argb the color in ARGB format
	 */
	void renderGlyph(TrueTypeFont font, int glyphId, RasterTarget target, 
	                 float sizePx, int x, int y, int argb);
	
	/**
	 * Render a string of text at the specified position.
	 * This method handles text shaping, kerning, and proper glyph positioning.
	 * 
	 * @param font the font to use for rendering
	 * @param text the text string to render
	 * @param target the surface to render to
	 * @param sizePx the font size in pixels
	 * @param x the x-coordinate (baseline origin)
	 * @param y the y-coordinate (baseline)
	 * @param argb the color in ARGB format
	 */
	void renderString(TrueTypeFont font, String text, RasterTarget target,
	                  float sizePx, int x, int y, int argb);
	
	/**
	 * Measure the width of a text string in pixels.
	 * 
	 * @param font the font to use for measurement
	 * @param text the text string to measure
	 * @param sizePx the font size in pixels
	 * @return the width of the text in pixels
	 */
	int measureString(TrueTypeFont font, String text, float sizePx);
	
	/**
	 * Clear any cached rendering data.
	 * Implementations may cache rendered glyphs; this method forces a cache clear.
	 */
	void clearCache();
	
	/**
	 * A target surface that can receive rendered pixels.
	 * This abstraction allows rendering to different types of surfaces
	 * (BufferedImage, WebGL texture, WASM buffer, etc.).
	 */
	interface RasterTarget {
		/**
		 * Get the width of the target surface in pixels.
		 * @return width in pixels
		 */
		int getWidth();
		
		/**
		 * Get the height of the target surface in pixels.
		 * @return height in pixels
		 */
		int getHeight();
		
		/**
		 * Set a pixel at the specified coordinates.
		 * @param x the x-coordinate
		 * @param y the y-coordinate
		 * @param argb the color in ARGB format
		 */
		void setRGB(int x, int y, int argb);
		
		/**
		 * Get the pixel color at the specified coordinates.
		 * @param x the x-coordinate
		 * @param y the y-coordinate
		 * @return the color in ARGB format
		 */
		int getRGB(int x, int y);
	}
}
