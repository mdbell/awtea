package me.mdbell.awtea.font;

/**
 * Example implementation of a custom font renderer.
 * This demonstrates how to create alternative rendering strategies.
 * 
 * <p>This example shows a simple "debug" renderer that could be used
 * for testing, performance measurement, or as a starting point for
 * more sophisticated implementations like SDF or canvas-based rendering.
 * 
 * <p><b>This is an example/template class and is not intended for production use.</b>
 * 
 * @see FontRenderer
 */
public class ExampleDebugFontRenderer implements FontRenderer {
	
	private int renderCallCount = 0;
	
	@Override
	public void renderGlyph(TrueTypeFont font, int glyphId, RasterTarget target,
	                       float sizePx, int x, int y, int argb) {
		renderCallCount++;
		
		// Example: Draw a simple placeholder box instead of the actual glyph
		// This could be useful for debugging or performance testing
		int boxSize = (int) sizePx;
		for (int dx = 0; dx < boxSize && x + dx < target.getWidth(); dx++) {
			for (int dy = 0; dy < boxSize && y + dy < target.getHeight(); dy++) {
				if (dx == 0 || dy == 0 || dx == boxSize - 1 || dy == boxSize - 1) {
					target.setRGB(x + dx, y + dy, argb);
				}
			}
		}
	}
	
	@Override
	public void renderString(TrueTypeFont font, String text, RasterTarget target,
	                        float sizePx, int x, int y, int argb) {
		renderCallCount++;
		
		// Simple implementation: render each character as a box
		int currentX = x;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			int codePoint = (int) c;
			int glyphId = font.glyphForCodePoint(codePoint);
			
			renderGlyph(font, glyphId, target, sizePx, currentX, y, argb);
			
			// Advance position (simplified - no kerning)
			int advanceWidth = font.getAdvanceWidthUnits(glyphId);
			float scale = sizePx / font.getUnitsPerEm();
			currentX += (int) (advanceWidth * scale);
		}
	}
	
	@Override
	public int measureString(TrueTypeFont font, String text, float sizePx) {
		int width = 0;
		float scale = sizePx / font.getUnitsPerEm();
		
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			int codePoint = (int) c;
			int glyphId = font.glyphForCodePoint(codePoint);
			int advanceWidth = font.getAdvanceWidthUnits(glyphId);
			width += (int) (advanceWidth * scale);
		}
		
		return width;
	}
	
	@Override
	public void clearCache() {
		// This simple renderer doesn't cache anything
		renderCallCount = 0;
	}
	
	/**
	 * Get the number of render calls made by this renderer.
	 * Useful for performance testing and debugging.
	 * @return the number of render calls
	 */
	public int getRenderCallCount() {
		return renderCallCount;
	}
}
