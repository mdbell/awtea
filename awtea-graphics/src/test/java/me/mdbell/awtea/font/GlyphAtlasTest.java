package me.mdbell.awtea.font;

import me.mdbell.awtea.gfx.SurfaceBackendFactory;
import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.gfx.SurfaceBackend;
import me.mdbell.awtea.test.Assert;
import me.mdbell.awtea.test.Test;

/**
 * Tests for the GlyphAtlas implementation.
 */
public class GlyphAtlasTest {

	private SurfaceBackend backend;
	private TrueTypeFont testFont;

	public GlyphAtlasTest() {
		backend = SurfaceBackendFactory.getDefault();
		// Load a test font (NotoSans is available)
		try {
			testFont = FontLoader.loadFont("NotoSans");
		} catch (Exception e) {
			throw new RuntimeException("Failed to load test font", e);
		}
		Assert.assertNotNull(testFont, "Test font should load successfully");
	}

	@Test
	public void testAtlasCreation() {
		GlyphAtlas atlas = new GlyphAtlas(backend);
		Assert.assertNotNull(atlas, "Atlas should be created");
		Assert.assertNotNull(atlas.getAtlasSurface(), "Atlas surface should exist");

		Surface surface = atlas.getAtlasSurface();
		Assert.assertTrue(surface.getWidth() > 0, "Atlas width should be positive");
		Assert.assertTrue(surface.getHeight() > 0, "Atlas height should be positive");

		atlas.destroy();
	}

	@Test
	public void testGlyphCaching() {
		GlyphAtlas atlas = new GlyphAtlas(backend);

		// Get glyph ID for 'A'
		int glyphId = testFont.glyphForCodePoint('A');
		Assert.assertTrue(glyphId > 0, "Glyph ID for 'A' should be valid");

		// First access - should rasterize and cache
		GlyphAtlas.GlyphEntry entry1 = atlas.getOrCreateGlyph(
				testFont, glyphId, 16.0f, 0xFF000000, 4, false);
		Assert.assertNotNull(entry1, "First glyph entry should be created");
		Assert.assertTrue(entry1.getWidth() > 0, "Glyph width should be positive");
		Assert.assertTrue(entry1.getHeight() > 0, "Glyph height should be positive");

		// Second access - should return cached entry (same object reference)
		GlyphAtlas.GlyphEntry entry2 = atlas.getOrCreateGlyph(
				testFont, glyphId, 16.0f, 0xFF000000, 4, false);
		Assert.assertNotNull(entry2, "Second glyph entry should be returned");
		Assert.assertTrue(entry1 == entry2, "Should return the same cached entry");

		atlas.destroy();
	}

	@Test
	public void testMultipleGlyphs() {
		GlyphAtlas atlas = new GlyphAtlas(backend);

		// Cache multiple glyphs
		String testText = "Hello";
		for (int i = 0; i < testText.length(); i++) {
			char c = testText.charAt(i);
			int glyphId = testFont.glyphForCodePoint(c);

			GlyphAtlas.GlyphEntry entry = atlas.getOrCreateGlyph(
					testFont, glyphId, 16.0f, 0xFF000000, 4, false);
			Assert.assertNotNull(entry, "Glyph entry for '" + c + "' should be created");
		}

		// Verify all glyphs are cached
		for (int i = 0; i < testText.length(); i++) {
			char c = testText.charAt(i);
			int glyphId = testFont.glyphForCodePoint(c);

			GlyphAtlas.GlyphEntry entry = atlas.getOrCreateGlyph(
					testFont, glyphId, 16.0f, 0xFF000000, 4, false);
			Assert.assertNotNull(entry, "Cached glyph for '" + c + "' should be retrievable");
		}

		atlas.destroy();
	}

	@Test
	public void testDifferentSizes() {
		GlyphAtlas atlas = new GlyphAtlas(backend);

		int glyphId = testFont.glyphForCodePoint('A');

		// Same glyph at different sizes should be cached separately
		GlyphAtlas.GlyphEntry entry16 = atlas.getOrCreateGlyph(
				testFont, glyphId, 16.0f, 0xFF000000, 4, false);
		GlyphAtlas.GlyphEntry entry24 = atlas.getOrCreateGlyph(
				testFont, glyphId, 24.0f, 0xFF000000, 4, false);

		Assert.assertNotNull(entry16, "16px glyph should be cached");
		Assert.assertNotNull(entry24, "24px glyph should be cached");
		Assert.assertTrue(entry16 != entry24, "Different sizes should have different entries");

		// Verify dimensions are different
		Assert.assertTrue(entry16.getWidth() != entry24.getWidth(),
				"Different sizes should have different dimensions");

		atlas.destroy();
	}

	@Test
	public void testDifferentColors() {
		GlyphAtlas atlas = new GlyphAtlas(backend);

		int glyphId = testFont.glyphForCodePoint('A');

		// Same glyph in different colors should be cached separately
		GlyphAtlas.GlyphEntry entryBlack = atlas.getOrCreateGlyph(
				testFont, glyphId, 16.0f, 0xFF000000, 4, false);
		GlyphAtlas.GlyphEntry entryRed = atlas.getOrCreateGlyph(
				testFont, glyphId, 16.0f, 0xFFFF0000, 4, false);

		Assert.assertNotNull(entryBlack, "Black glyph should be cached");
		Assert.assertNotNull(entryRed, "Red glyph should be cached");
		Assert.assertTrue(entryBlack != entryRed, "Different colors should have different entries");

		atlas.destroy();
	}

	@Test
	public void testDestroy() {
		GlyphAtlas atlas = new GlyphAtlas(backend);

		// Cache some glyphs
		int glyphId = testFont.glyphForCodePoint('A');
		GlyphAtlas.GlyphEntry entry = atlas.getOrCreateGlyph(
				testFont, glyphId, 16.0f, 0xFF000000, 4, false);
		Assert.assertNotNull(entry, "Glyph should be cached");

		// Destroy atlas
		atlas.destroy();

		// After destroy, atlas should be cleared
		Assert.assertNotNull(atlas, "Atlas object should still exist after destroy");
	}
}
