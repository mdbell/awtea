package me.mdbell.awtea.font;

import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.gfx.SurfaceBackend;
import me.mdbell.awtea.gfx.SurfaceBackendFactory;
import me.mdbell.awtea.test.Assert;
import me.mdbell.awtea.test.Test;
import me.mdbell.awtea.util.GlyphRasterizer;

/**
 * Tests for sub-pixel font rendering.
 */
public class SubPixelRenderingTest {

	private TrueTypeFont testFont;

	public SubPixelRenderingTest() {
		// Load a test font (NotoSans is available)
		try {
			testFont = FontLoader.loadFont("NotoSans");
		} catch (Exception e) {
			throw new RuntimeException("Failed to load test font", e);
		}
		Assert.assertNotNull(testFont, "Test font should load successfully");
	}

	@Test
	public void testSubPixelRenderingCreatesOutput() {
		// Create a simple raster target
		int width = 100;
		int height = 50;
		int[] pixels = new int[width * height];

		GlyphRasterizer.RasterTarget target = new GlyphRasterizer.RasterTarget() {
			@Override
			public int getWidth() {
				return width;
			}

			@Override
			public int getHeight() {
				return height;
			}

			@Override
			public void setRGB(int x, int y, int argb) {
				if (x >= 0 && x < width && y >= 0 && y < height) {
					pixels[y * width + x] = argb;
				}
			}

			@Override
			public int getRGB(int x, int y) {
				if (x >= 0 && x < width && y >= 0 && y < height) {
					return pixels[y * width + x];
				}
				return 0;
			}
		};

		// Clear to white background
		for (int i = 0; i < pixels.length; i++) {
			pixels[i] = 0xFFFFFFFF;
		}

		// Render "A" with sub-pixel rendering enabled
		int glyphId = testFont.glyphForCodePoint('A');
		Assert.assertTrue(glyphId > 0, "Glyph ID for 'A' should be valid");

		int black = 0xFF000000;
		GlyphRasterizer.drawGlyph(testFont, glyphId, target, 16.0f, 10, 30, black, 4, true);

		// Verify that some pixels were written (glyph was rendered)
		int nonWhitePixels = 0;
		for (int pixel : pixels) {
			if (pixel != 0xFFFFFFFF) {
				nonWhitePixels++;
			}
		}

		Assert.assertTrue(nonWhitePixels > 0, "Sub-pixel rendering should produce visible output");
	}

	@Test
	public void testSubPixelVsRegularRendering() {
		// Create two raster targets for comparison
		int width = 100;
		int height = 50;
		int[] pixelsRegular = new int[width * height];
		int[] pixelsSubPixel = new int[width * height];

		GlyphRasterizer.RasterTarget targetRegular = createTarget(width, height, pixelsRegular);
		GlyphRasterizer.RasterTarget targetSubPixel = createTarget(width, height, pixelsSubPixel);

		// Initialize both to white
		for (int i = 0; i < width * height; i++) {
			pixelsRegular[i] = 0xFFFFFFFF;
			pixelsSubPixel[i] = 0xFFFFFFFF;
		}

		int glyphId = testFont.glyphForCodePoint('A');
		int black = 0xFF000000;

		// Render with regular mode
		GlyphRasterizer.drawGlyph(testFont, glyphId, targetRegular, 16.0f, 10, 30, black, 4, false);

		// Render with sub-pixel mode
		GlyphRasterizer.drawGlyph(testFont, glyphId, targetSubPixel, 16.0f, 10, 30, black, 4, true);

		// Both should produce output
		int nonWhiteRegular = countNonWhite(pixelsRegular);
		int nonWhiteSubPixel = countNonWhite(pixelsSubPixel);

		Assert.assertTrue(nonWhiteRegular > 0, "Regular rendering should produce output");
		Assert.assertTrue(nonWhiteSubPixel > 0, "Sub-pixel rendering should produce output");

		// The outputs should be different (sub-pixel rendering alters the rendering)
		boolean hasDifference = false;
		for (int i = 0; i < pixelsRegular.length; i++) {
			if (pixelsRegular[i] != pixelsSubPixel[i]) {
				hasDifference = true;
				break;
			}
		}

		Assert.assertTrue(hasDifference, "Sub-pixel and regular rendering should produce different results");
	}

	@Test
	public void testRasterFontRendererWithSubPixel() {
		// Test the RasterFontRenderer with sub-pixel rendering
		RasterFontRenderer renderer = new RasterFontRenderer(4, true);
		Assert.assertNotNull(renderer, "Renderer with sub-pixel should be created");

		int width = 100;
		int height = 50;
		int[] pixels = new int[width * height];
		for (int i = 0; i < pixels.length; i++) {
			pixels[i] = 0xFFFFFFFF;
		}

		FontRenderer.RasterTarget target = new FontRenderer.RasterTarget() {
			@Override
			public int getWidth() {
				return width;
			}

			@Override
			public int getHeight() {
				return height;
			}

			@Override
			public void setRGB(int x, int y, int argb) {
				if (x >= 0 && x < width && y >= 0 && y < height) {
					pixels[y * width + x] = argb;
				}
			}

			@Override
			public int getRGB(int x, int y) {
				if (x >= 0 && x < width && y >= 0 && y < height) {
					return pixels[y * width + x];
				}
				return 0;
			}
		};

		// Render some text
		renderer.renderString(testFont, "Test", target, 16.0f, 10, 30, 0xFF000000);

		// Verify output was produced
		int nonWhitePixels = countNonWhite(pixels);
		Assert.assertTrue(nonWhitePixels > 0, "RasterFontRenderer with sub-pixel should produce output");
	}

	@Test
	public void testAtlasBasedFontRendererWithSubPixel() {
		// Test the AtlasBasedFontRenderer with sub-pixel rendering
		SurfaceBackend backend = SurfaceBackendFactory.getDefault();
		AtlasBasedFontRenderer renderer = new AtlasBasedFontRenderer(backend, 4, true);
		Assert.assertNotNull(renderer, "Atlas renderer with sub-pixel should be created");

		// Create a test surface
		Surface surface = backend.createCompatibleSurface(
			100, 50, java.awt.image.BufferedImage.TYPE_INT_ARGB);
		Assert.assertNotNull(surface, "Test surface should be created");

		// Clear to white
		org.teavm.jso.typedarrays.Uint8ClampedArray pixels = surface.getPixelData();
		if (pixels != null) {
			for (int i = 0; i < pixels.getLength(); i++) {
				pixels.set(i, (byte) 0xFF);
			}
		}

		// Render some text
		renderer.renderString(testFont, "Test", surface, 16.0f, 10, 30, 0xFF000000);

		// Verify output was produced
		boolean hasNonWhitePixels = false;
		org.teavm.jso.typedarrays.Int32Array intPixels = new org.teavm.jso.typedarrays.Int32Array(
			pixels.getBuffer(), pixels.getByteOffset(), pixels.getLength() / 4);
		for (int i = 0; i < intPixels.getLength(); i++) {
			if (intPixels.get(i) != 0xFFFFFFFF) {
				hasNonWhitePixels = true;
				break;
			}
		}
		Assert.assertTrue(hasNonWhitePixels, "Atlas renderer with sub-pixel should produce output");

		surface.destroy();
		renderer.clearCache();
	}

	private GlyphRasterizer.RasterTarget createTarget(int width, int height, int[] pixels) {
		return new GlyphRasterizer.RasterTarget() {
			@Override
			public int getWidth() {
				return width;
			}

			@Override
			public int getHeight() {
				return height;
			}

			@Override
			public void setRGB(int x, int y, int argb) {
				if (x >= 0 && x < width && y >= 0 && y < height) {
					pixels[y * width + x] = argb;
				}
			}

			@Override
			public int getRGB(int x, int y) {
				if (x >= 0 && x < width && y >= 0 && y < height) {
					return pixels[y * width + x];
				}
				return 0;
			}
		};
	}

	private int countNonWhite(int[] pixels) {
		int count = 0;
		for (int pixel : pixels) {
			if (pixel != 0xFFFFFFFF) {
				count++;
			}
		}
		return count;
	}
}
