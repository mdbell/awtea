package me.mdbell.awtea.font;

import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.gfx.SurfaceBackend;
import me.mdbell.awtea.util.GlyphRasterizer;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.teavm.jso.typedarrays.Int32Array;
import org.teavm.jso.typedarrays.Uint8ClampedArray;

/**
 * A persistent texture atlas for caching rendered glyphs.
 * 
 * <p>This class manages one or more surfaces (atlases) that store pre-rasterized glyph images.
 * When a glyph is requested, it is either retrieved from the atlas or rasterized into it.
 * This eliminates the need to create temporary surfaces for each text render operation.
 * 
 * <p>Key benefits:
 * <ul>
 *   <li>Eliminates per-glyph/per-string temporary surface allocations</li>
 *   <li>Reduces memory pressure and GC overhead</li>
 *   <li>Improves rendering performance through glyph reuse</li>
 *   <li>Works with both WASM and software backends</li>
 * </ul>
 * 
 * <p>The atlas uses a simple horizontal packing strategy with multiple rows.
 * When an atlas fills up, the least recently used glyphs are evicted using an LRU policy.
 * 
 * @see GlyphRasterizer
 * @see FontRenderer
 */
public class GlyphAtlas {
	
	private static final Logger log = LoggerFactory.getLogger(GlyphAtlas.class);
	
	// Atlas dimensions - chosen to balance memory usage and capacity
	private static final int ATLAS_WIDTH = 2048;
	private static final int ATLAS_HEIGHT = 2048;
	
	// Maximum number of glyphs to cache before evicting
	private static final int MAX_GLYPHS = 512;
	
	// Padding around each glyph to prevent bleeding
	private static final int GLYPH_PADDING = 2;
	
	private final SurfaceBackend backend;
	private final Map<GlyphKey, GlyphEntry> glyphCache;
	
	// Current atlas surface
	private Surface atlasSurface;
	
	// Current packing position
	private int currentX = GLYPH_PADDING;
	private int currentY = GLYPH_PADDING;
	private int currentRowHeight = 0;
	
	/**
	 * Create a new glyph atlas using the specified backend.
	 * 
	 * @param backend the surface backend to use for creating atlas surfaces
	 */
	public GlyphAtlas(SurfaceBackend backend) {
		if (backend == null) {
			throw new IllegalArgumentException("backend must not be null");
		}
		this.backend = backend;
		
		// Use LinkedHashMap with access order for LRU behavior
		this.glyphCache = new LinkedHashMap<GlyphKey, GlyphEntry>(MAX_GLYPHS, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<GlyphKey, GlyphEntry> eldest) {
				return size() > MAX_GLYPHS;
			}
		};
		
		// Create initial atlas surface
		createAtlasSurface();
	}
	
	/**
	 * Get or create a glyph entry in the atlas.
	 * 
	 * @param font the font containing the glyph
	 * @param glyphId the glyph identifier
	 * @param sizePx the font size in pixels
	 * @param argb the color in ARGB format (used as part of cache key)
	 * @param supersample the supersampling factor
	 * @return the glyph entry, or null if the glyph cannot be rendered
	 */
	public GlyphEntry getOrCreateGlyph(TrueTypeFont font, int glyphId, float sizePx, 
	                                    int argb, int supersample) {
		GlyphKey key = new GlyphKey(font, glyphId, sizePx, argb, supersample);
		
		synchronized (glyphCache) {
			GlyphEntry entry = glyphCache.get(key);
			if (entry != null) {
				return entry;
			}
			
			// Need to rasterize this glyph into the atlas
			entry = rasterizeToAtlas(font, glyphId, sizePx, argb, supersample);
			if (entry != null) {
				glyphCache.put(key, entry);
			}
			
			return entry;
		}
	}
	
	/**
	 * Rasterize a glyph and add it to the atlas.
	 * 
	 * @param font the font containing the glyph
	 * @param glyphId the glyph identifier
	 * @param sizePx the font size in pixels
	 * @param argb the color in ARGB format
	 * @param supersample the supersampling factor
	 * @return the glyph entry, or null if the glyph cannot be rendered
	 */
	private GlyphEntry rasterizeToAtlas(TrueTypeFont font, int glyphId, float sizePx, 
	                                     int argb, int supersample) {
		// First, get the glyph metrics to determine size
		me.mdbell.awtea.font.Glyph glyph = font.loadGlyph(glyphId);
		if (glyph == null || glyph.isEmpty()) {
			return null;
		}
		
		// Calculate glyph bounding box in pixels
		float unitsPerEm = font.getUnitsPerEm();
		float scalePx = sizePx / unitsPerEm;
		
		int xMinUnits = glyph.getXMin();
		int yMinUnits = glyph.getYMin();
		int xMaxUnits = glyph.getXMax();
		int yMaxUnits = glyph.getYMax();
		
		float xMinPx = xMinUnits * scalePx;
		float xMaxPx = xMaxUnits * scalePx;
		float yMaxPx = -yMinUnits * scalePx;
		float yMinPx = -yMaxUnits * scalePx;
		
		int glyphWidth = (int) Math.ceil(xMaxPx - xMinPx) + 1;
		int glyphHeight = (int) Math.ceil(yMaxPx - yMinPx) + 1;
		
		if (glyphWidth <= 0 || glyphHeight <= 0) {
			return null;
		}
		
		// Check if we need to move to the next row or reset atlas
		if (currentX + glyphWidth + GLYPH_PADDING > ATLAS_WIDTH) {
			// Move to next row
			currentX = GLYPH_PADDING;
			currentY += currentRowHeight + GLYPH_PADDING;
			currentRowHeight = 0;
			
			// Check if we've run out of vertical space
			if (currentY + glyphHeight + GLYPH_PADDING > ATLAS_HEIGHT) {
				// Atlas is full, clear it and start over
				log.debug("Atlas full, clearing and restarting");
				clearAtlas();
			}
		}
		
		// Create a temporary surface to render the glyph
		Surface tempSurface = backend.createFontRenderSurface(glyphWidth, glyphHeight);
		if (tempSurface == null) {
			return null;
		}
		
		try {
			// Clear temporary surface to transparent
			org.teavm.jso.typedarrays.Uint8ClampedArray tempPixels = tempSurface.getPixelData();
			if (tempPixels != null) {
				for (int i = 0; i < tempPixels.getLength(); i++) {
					tempPixels.set(i, (byte) 0);
				}
			}
			
			// Render the glyph directly to the temporary surface
			// Position at origin relative to glyph bounds
			int renderX = -(int) Math.floor(xMinPx);
			int renderY = glyphHeight + (int) Math.floor(yMinPx);
			
			renderGlyphToSurface(font, glyphId, tempSurface, sizePx, renderX, renderY, argb, supersample);
			
			// Copy the rendered glyph from temp surface to atlas
			copyToAtlas(tempSurface, currentX, currentY, glyphWidth, glyphHeight);
			
			// Create the glyph entry
			GlyphEntry entry = new GlyphEntry(
				atlasSurface,
				currentX,
				currentY,
				glyphWidth,
				glyphHeight,
				(int) Math.floor(xMinPx),
				(int) Math.floor(yMinPx)
			);
			
			// Update packing position
			currentX += glyphWidth + GLYPH_PADDING;
			currentRowHeight = Math.max(currentRowHeight, glyphHeight);
			
			return entry;
			
		} finally {
			// Always destroy the temporary surface
			tempSurface.destroy();
		}
	}
	
	/**
	 * Render a glyph directly to a surface's pixel buffer.
	 * 
	 * @param font the font containing the glyph
	 * @param glyphId the glyph identifier
	 * @param surface the target surface
	 * @param sizePx the font size in pixels
	 * @param x the x-coordinate (baseline origin)
	 * @param y the y-coordinate (baseline)
	 * @param argb the color in ARGB format
	 * @param supersample the supersampling factor
	 */
	private void renderGlyphToSurface(TrueTypeFont font, int glyphId, Surface surface,
	                                   float sizePx, int x, int y, int argb, int supersample) {
		// Use GlyphRasterizer with a surface adapter
		SurfaceRasterTarget target = new SurfaceRasterTarget(surface);
		me.mdbell.awtea.util.GlyphRasterizer.drawGlyph(
			font, glyphId, target, sizePx, x, y, argb, supersample
		);
	}
	
	/**
	 * Copy pixels from a source surface to the atlas.
	 * 
	 * @param source the source surface
	 * @param destX the destination X coordinate in the atlas
	 * @param destY the destination Y coordinate in the atlas
	 * @param width the width to copy
	 * @param height the height to copy
	 */
	private void copyToAtlas(Surface source, int destX, int destY, int width, int height) {
		// Convert pixel data to int arrays for efficient copying
		Uint8ClampedArray srcPixelArray = source.getPixelData();
		Uint8ClampedArray dstPixelArray = atlasSurface.getPixelData();
		
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
		
		int srcWidth = source.getWidth();
		int dstWidth = atlasSurface.getWidth();
		
		for (int y = 0; y < height; y++) {
			int srcRowOffset = y * srcWidth;
			int dstRowOffset = (destY + y) * dstWidth + destX;
			
			for (int x = 0; x < width; x++) {
				int srcIdx = srcRowOffset + x;
				int dstIdx = dstRowOffset + x;
				
				// Copy ARGB integer directly
				dstData.set(dstIdx, srcData.get(srcIdx));
			}
		}
	}
	
	/**
	 * Clear the atlas and restart packing.
	 */
	private void clearAtlas() {
		synchronized (glyphCache) {
			glyphCache.clear();
			currentX = GLYPH_PADDING;
			currentY = GLYPH_PADDING;
			currentRowHeight = 0;
			
			// Clear the atlas surface
			if (atlasSurface != null) {
				org.teavm.jso.typedarrays.Uint8ClampedArray data = atlasSurface.getPixelData();
				if (data != null) {
					// Clear to transparent
					for (int i = 0; i < data.getLength(); i++) {
						data.set(i, (byte) 0);
					}
				}
			}
		}
	}
	
	/**
	 * Create a new atlas surface.
	 */
	private void createAtlasSurface() {
		if (atlasSurface != null) {
			atlasSurface.destroy();
		}
		
		// Create atlas with ARGB format for font rendering
		atlasSurface = backend.createCompatibleSurface(
			ATLAS_WIDTH, 
			ATLAS_HEIGHT, 
			Surface.FORMAT_INT_ARGB
		);
		
		if (atlasSurface == null) {
			throw new IllegalStateException("Failed to create atlas surface");
		}
		
		log.info("Created glyph atlas: {}x{}", ATLAS_WIDTH, ATLAS_HEIGHT);
	}
	
	/**
	 * Destroy the atlas and free resources.
	 */
	public void destroy() {
		synchronized (glyphCache) {
			glyphCache.clear();
			if (atlasSurface != null) {
				atlasSurface.destroy();
				atlasSurface = null;
			}
		}
	}
	
	/**
	 * Get the atlas surface.
	 * 
	 * @return the atlas surface
	 */
	public Surface getAtlasSurface() {
		return atlasSurface;
	}
	
	/**
	 * Key for identifying a unique glyph in the cache.
	 */
	private static class GlyphKey {
		final TrueTypeFont font;
		final int glyphId;
		final float sizePx;
		final int argb;
		final int supersample;
		private final int hash;
		
		GlyphKey(TrueTypeFont font, int glyphId, float sizePx, int argb, int supersample) {
			this.font = font;
			this.glyphId = glyphId;
			this.sizePx = sizePx;
			this.argb = argb;
			this.supersample = supersample;
			
			int h = System.identityHashCode(font);
			h = 31 * h + glyphId;
			h = 31 * h + Float.floatToIntBits(sizePx);
			h = 31 * h + argb;
			h = 31 * h + supersample;
			this.hash = h;
		}
		
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof GlyphKey)) return false;
			GlyphKey other = (GlyphKey) o;
			return this.font == other.font
				&& this.glyphId == other.glyphId
				&& Float.floatToIntBits(this.sizePx) == Float.floatToIntBits(other.sizePx)
				&& this.argb == other.argb
				&& this.supersample == other.supersample;
		}
		
		@Override
		public int hashCode() {
			return hash;
		}
	}
	
	/**
	 * Entry representing a glyph in the atlas.
	 */
	public static class GlyphEntry {
		private final Surface atlasSurface;
		private final int atlasX;
		private final int atlasY;
		private final int width;
		private final int height;
		private final int offsetX;
		private final int offsetY;
		
		GlyphEntry(Surface atlasSurface, int atlasX, int atlasY, 
		           int width, int height, int offsetX, int offsetY) {
			this.atlasSurface = atlasSurface;
			this.atlasX = atlasX;
			this.atlasY = atlasY;
			this.width = width;
			this.height = height;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
		}
		
		public Surface getAtlasSurface() {
			return atlasSurface;
		}
		
		public int getAtlasX() {
			return atlasX;
		}
		
		public int getAtlasY() {
			return atlasY;
		}
		
		public int getWidth() {
			return width;
		}
		
		public int getHeight() {
			return height;
		}
		
		public int getOffsetX() {
			return offsetX;
		}
		
		public int getOffsetY() {
			return offsetY;
		}
	}
	
	/**
	 * Adapter to make a Surface work as a GlyphRasterizer.RasterTarget.
	 * This is temporary until we fully remove RasterTarget abstraction.
	 */
	private static class SurfaceRasterTarget implements me.mdbell.awtea.util.GlyphRasterizer.RasterTarget {
		private final Surface surface;
		private final int[] pixels;
		private final int width;
		private final int height;
		
		SurfaceRasterTarget(Surface surface) {
			Uint8ClampedArray pixelArray = surface.getPixelData();
			Int32Array intArray = new Int32Array(
				pixelArray.getBuffer(),
				pixelArray.getByteOffset(),
				pixelArray.getLength() / 4
			);
			this.surface = surface;
			this.pixels = intArray.toJavaArray();
			this.width = surface.getWidth();
			this.height = surface.getHeight();
		}
		
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
			if (x < 0 || x >= width || y < 0 || y >= height || pixels == null) {
				return;
			}
			int idx = (y * width + x);
			pixels[idx] = argb;
		}
		
		@Override
		public int getRGB(int x, int y) {
			if (x < 0 || x >= width || y < 0 || y >= height || pixels == null) {
				return 0;
			}
			return pixels[y * width + x];
		}
	}
}
