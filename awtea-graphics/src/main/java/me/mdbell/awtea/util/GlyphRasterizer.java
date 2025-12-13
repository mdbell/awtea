package me.mdbell.awtea.util;

import lombok.Data;
import lombok.ToString;
import me.mdbell.awtea.font.Glyph;
import me.mdbell.awtea.font.GlyphPath;
import me.mdbell.awtea.font.GlyphPathBuilder;
import me.mdbell.awtea.font.TrueTypeFont;

import java.util.*;

/**
 * Utility class for rasterizing TrueType font glyphs to pixel-based representations.
 * 
 * <p>This class provides the core rasterization logic for converting vector font outlines
 * to antialiased pixel data. It uses scanline filling with configurable supersampling
 * for high-quality text rendering.
 * 
 * <p><b>Architecture Note:</b> This class is now used internally by {@link me.mdbell.awtea.font.RasterFontRenderer}.
 * New code should use the {@link me.mdbell.awtea.font.FontRenderer} abstraction via
 * {@link me.mdbell.awtea.font.FontPeer} rather than calling this class directly.
 * Direct usage is maintained for backward compatibility and internal implementation.
 * 
 * <p>The rasterization process:
 * <ol>
 *   <li>Extract glyph outline as bezier curves from TrueType font</li>
 *   <li>Flatten curves into line segments</li>
 *   <li>Build edge list for scanline filling</li>
 *   <li>Rasterize at supersampled resolution</li>
 *   <li>Downsample and blend to target surface</li>
 * </ol>
 * 
 * @see me.mdbell.awtea.font.FontRenderer
 * @see me.mdbell.awtea.font.RasterFontRenderer
 */
public final class GlyphRasterizer {

	private static final int SUPERSAMPLE = 4; // 2, 3, or 4; 4 looks nice

	// more segments = smoother curves
	private static final int QUAD_FLATTEN_STEPS = 24;

	private static final int MAX_GLYPH_PIXELS = 4096 * 4096;

	private static volatile int maxGlyphCacheEntries = 256;

	private static final Map<GlyphKey, CachedGlyph> CACHE =
		new LinkedHashMap<>(128, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<GlyphKey, CachedGlyph> eldest) {
				return size() > maxGlyphCacheEntries;
			}
		};

	private GlyphRasterizer() {}

	public static void setMaxGlyphCacheEntries(int maxEntries) {
		if (maxEntries <= 0) throw new IllegalArgumentException("maxEntries must be > 0");
		maxGlyphCacheEntries = maxEntries;
	}

	public static void clearCache() {
		synchronized (CACHE) {
			CACHE.clear();
		}
	}

	public static int measureString(TrueTypeFont font, String str, float sizePx) {
		return processString(font, str, null, sizePx, 0, 0, 0, 1,
				(font1, img, gid, size, x, y, argb, ss) -> getAdvanceWidthUnits(font1, gid));
	}

	public static void drawString(TrueTypeFont font, String str, RasterTarget dest, float sizePx, int x,
								  int y, int argb) {
		drawString(font, str, dest, sizePx, x, y, argb, SUPERSAMPLE);
	}

	public static void drawString(TrueTypeFont font, String str, RasterTarget dest, float sizePx, int x,
								  int y, int argb, int supersample) {
		processString(font, str, dest, sizePx, x, y, argb, supersample, GlyphRasterizer::drawGlyphByGid);
	}

	private static int processString(TrueTypeFont font, String str, RasterTarget dest, float sizePx, int x,
									 int y, int argb, int supersample, GlyphProcessor func) {
		if (str == null || str.isEmpty()) {
			return 0;
		}

		double scale = sizePx / font.getUnitsPerEm();

		int prevGid = -1;
		double penX = x;

		for (int i = 0; i < str.length(); ) {
			int cp = str.codePointAt(i);
			int advanceChars = Character.charCount(cp);
			// Peek next codepoint for a possible variation selector
			int variationSelector = 0;
			if (i + advanceChars < str.length()) {
				int nextCp = str.codePointAt(i + advanceChars);
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

			// Kerning in font units
			int kernUnits = (prevGid >= 0) ? font.getKerningUnits(prevGid, gid) : 0;
			double kernPx = kernUnits * scale;
			penX += kernPx;

			int pxX = (int) Math.round(penX);
			double advancePx = func.processGlyph(font, dest, gid, sizePx, pxX, y, argb, supersample) * scale;
			penX += advancePx;

			prevGid = gid;
			i += advanceChars;
		}
		return (int) Math.round(penX - x);
	}

	private static double drawGlyphByGid(TrueTypeFont font, RasterTarget img, int gid, float sizePx,
										 int x, int y, int argb, int supersample) {
		drawGlyph(
			font,
			gid,
			img,
			sizePx,
			x,
			y,
			argb,
			supersample
		);

		return getAdvanceWidthUnits(font, gid);
	}

	private static double getAdvanceWidthUnits(TrueTypeFont font, int gid) {
		if (gid <= 0) {
			return (font.getUnitsPerEm() * 0.6);
		}
		return font.getAdvanceWidthUnits(gid);
	}

	public static void drawGlyph(TrueTypeFont font,
								 int glyphId,
								 RasterTarget dest,
								 float sizePx,
								 int originX,
								 int baselineY,
								 int argb) {

		drawGlyph(font, glyphId, dest, sizePx, originX, baselineY, argb, SUPERSAMPLE);
	}

	public static void drawGlyph(TrueTypeFont font,
								  int glyphId,
								  RasterTarget dest,
								  float sizePx,
								  int originX,
								  int baselineY,
								  int argb,
								  int supersample) {

		if (supersample <= 0) {
			throw new IllegalArgumentException("supersample must be >= 1");
		}

		Glyph glyph = requireGlyph(font, glyphId, dest, sizePx, originX, baselineY, argb);
		if (glyph == null) {
			return;
		}

		// 1) Get or build cached supersampled alpha mask for this glyph/size
		CachedGlyph cached = getOrCreateCachedGlyph(font, glyphId, glyph, sizePx, supersample);
		if (cached == null) {
			return;
		}

		// 2) Compute supersampled origin for THIS draw (x,y) from the cached base
		int ssX0 = cached.ssX0 + originX * supersample;
		int ssY0 = cached.ssY0 + baselineY * supersample;

		// 3) Downsample & blend into dest
		downsampleAndBlend(dest,
			cached.alphaMask,
			cached.ssWidth,
			cached.ssHeight,
			ssX0,
			ssY0,
			supersample,
			argb);
	}

	private static CachedGlyph getOrCreateCachedGlyph(TrueTypeFont font,
													  int glyphId,
													  Glyph glyph,
													  float sizePx,
													  int supersample) {

		GlyphKey key = new GlyphKey(font, glyphId, sizePx, supersample);

		synchronized (CACHE) {
			CachedGlyph cached = CACHE.get(key);
			if (cached != null) {
				return cached;
			}

			float unitsPerEm = font.getUnitsPerEm();
			float scalePx    = sizePx / unitsPerEm;
			float ssScale    = scalePx * supersample;

			// Canonical draw origin: originX = 0, baselineY = 0
			int originX = 0;
			int baselineY = 0;

			// Glyph bbox in font units
			int xMinUnits = glyph.getXMin();
			int yMinUnits = glyph.getYMin();
			int xMaxUnits = glyph.getXMax();
			int yMaxUnits = glyph.getYMax();

			// Convert bbox to *pixel* coords for originX=0, baselineY=0
			float xMinPx = originX + xMinUnits * scalePx;
			float xMaxPx = originX + xMaxUnits * scalePx;

			// baselineY = 0, y up in font → y down in pixels
			float yMaxPx = baselineY - yMinUnits * scalePx;
			float yMinPx = baselineY - yMaxUnits * scalePx;

			// Supersampled bbox
			int ssX0 = (int) Math.floor(xMinPx * supersample);
			int ssY0 = (int) Math.floor(yMinPx * supersample);
			int ssX1 = (int) Math.ceil(xMaxPx * supersample);
			int ssY1 = (int) Math.ceil(yMaxPx * supersample);

			int ssWidth  = ssX1 - ssX0;
			int ssHeight = ssY1 - ssY0;

			if (ssWidth <= 0 || ssHeight <= 0) {
				return null;
			}

			long pixels = (long) ssWidth * (long) ssHeight;
			if (pixels <= 0 || pixels > MAX_GLYPH_PIXELS) {
				return null;
			}

			byte[] alphaMask = new byte[(int) pixels];

			// Build the supersampled alpha for originX=0, baselineY=0
			rasterizeGlyphToAlpha(
				glyph,
				ssScale,
				supersample,
				originX,
				baselineY,
				ssX0,
				ssY0,
				alphaMask,
				ssWidth,
				ssHeight
			);

			cached = new CachedGlyph(ssX0, ssY0, ssWidth, ssHeight, alphaMask);
			CACHE.put(key, cached);
			return cached;
		}
	}

	private static void rasterizeGlyphToAlpha(Glyph glyph,
											  float ssScale,
											  int supersample,
											  int originX, int baselineY,
											  int ssX0, int ssY0,
											  byte[] alphaMask,
											  int ssWidth, int ssHeight) {

		if (glyph == null || glyph.numberOfContours <= 0) {
			return;
		}

		// Build path in font units
		GlyphPath path = GlyphPathBuilder.buildPath(glyph);

		// Build edges in *supersampled* coordinate space
		List<Edge> edges = buildEdges(path, (x, y, out) -> {
			out[0] = originX * supersample + x * ssScale;
			out[1] = baselineY * supersample - y * ssScale;
		});

		if (edges.isEmpty()) {
			return;
		}

		// Scanline fill into alphaMask
		fillEdgesToAlpha(edges, alphaMask, ssWidth, ssHeight, ssX0, ssY0);
	}

	private static void setAlphaSample(byte[] alphaMask,
									   int ssWidth,
									   int ssX0,
									   int ssY0,
									   int xSS,
									   int ySS) {
		int relX = xSS - ssX0;
		int relY = ySS - ssY0;
		int idx = relY * ssWidth + relX;
		alphaMask[idx] = (byte) 0xFF;
	}

	private static void downsampleAndBlend(RasterTarget dest,
										   byte[] alphaMask,
										   int ssWidth,
										   int ssHeight,
										   int ssX0,
										   int ssY0,
										   int supersample,
										   int glyphARGB) {

		int destWidth  = dest.getWidth();
		int destHeight = dest.getHeight();

		int samplesPerPixel = supersample * supersample;

		int destX0 = ssX0 / supersample;
		int destY0 = ssY0 / supersample;
		int destX1 = (ssX0 + ssWidth  + supersample - 1) / supersample;
		int destY1 = (ssY0 + ssHeight + supersample - 1) / supersample;

		destX0 = Math.max(destX0, 0);
		destY0 = Math.max(destY0, 0);
		destX1 = Math.min(destX1, destWidth);
		destY1 = Math.min(destY1, destHeight);

		int glyphA = (glyphARGB >>> 24) & 0xFF;
		int glyphR = (glyphARGB >>> 16) & 0xFF;
		int glyphG = (glyphARGB >>>  8) & 0xFF;
		int glyphB = (glyphARGB       ) & 0xFF;

		for (int dy = destY0; dy < destY1; dy++) {
			for (int dx = destX0; dx < destX1; dx++) {

				int ssStartX = dx * supersample;
				int ssStartY = dy * supersample;

				int sumAlpha = 0;

				for (int sy = 0; sy < supersample; sy++) {
					int ssY = ssStartY + sy;
					int relY = ssY - ssY0;
					if (relY < 0 || relY >= ssHeight) continue;

					int rowOffset = relY * ssWidth;

					for (int sx = 0; sx < supersample; sx++) {
						int ssX = ssStartX + sx;
						int relX = ssX - ssX0;
						if (relX < 0 || relX >= ssWidth) continue;

						int idx = rowOffset + relX;
						sumAlpha += (alphaMask[idx] & 0xFF);
					}
				}

				if (sumAlpha == 0) {
					continue; // no coverage → nothing to draw
				}

				// Coverage 0..255
				int coverage = sumAlpha / samplesPerPixel;
				if (coverage <= 0) {
					continue;
				}

				// Combined alpha = glyph alpha * coverage / 255
				int a = (glyphA * coverage) / 255;
				if (a == 0) {
					continue;
				}

				// Blend: src = existing pixel, dst = glyph color
				int src = dest.getRGB(dx, dy);

				int srcA = (src >>> 24) & 0xFF;
				int srcR = (src >>> 16) & 0xFF;
				int srcG = (src >>>  8) & 0xFF;
				int srcB = (src       ) & 0xFF;

				int outA = a + ((srcA * (255 - a)) / 255);
				int outR = (glyphR * a + srcR * (255 - a)) / 255;
				int outG = (glyphG * a + srcG * (255 - a)) / 255;
				int outB = (glyphB * a + srcB * (255 - a)) / 255;

				int outARGB = (outA << 24) | (outR << 16) | (outG << 8) | outB;
				dest.setRGB(dx, dy, outARGB);
			}
		}
	}

	private static void drawMissingGlyphBox(RasterTarget dest,
											float sizePx,
											int originX,
											int baselineY,
											int argb) {
		// Box size ~0.8em; tweak to taste
		int boxSize = Math.round(sizePx * 0.8f);

		int left = originX;
		int top  = baselineY - boxSize; // sit on baseline like a glyph

		int right = left + boxSize - 1;
		int bottom = baselineY - 1;

		int imgW = dest.getWidth();
		int imgH = dest.getHeight();

		// Clamp to image bounds
		left   = Math.max(left, 0);
		top    = Math.max(top, 0);
		right  = Math.min(right, imgW - 1);
		bottom = Math.min(bottom, imgH - 1);

		if (left > right || top > bottom) {
			return;
		}

		// Simple outline box (non-AA is fine for a placeholder)
		for (int x = left; x <= right; x++) {
			dest.setRGB(x, top,    argb);
			dest.setRGB(x, bottom, argb);
		}
		for (int y = top; y <= bottom; y++) {
			dest.setRGB(left,  y, argb);
			dest.setRGB(right, y, argb);
		}
	}

	private static List<Edge> buildEdges(GlyphPath path, CoordTransform transform) {
		List<Edge> edges = new ArrayList<>();

		float startX = 0, startY = 0;
		float currX = 0, currY = 0;
		boolean haveCurr = false;

		float[] p = new float[2];
		float[] c = new float[2];

		for (GlyphPath.Cmd cmd : path.getCommands()) {
			switch (cmd.type) {
				case MOVE_TO: {
					transform.apply(cmd.x1, cmd.y1, p);
					currX = startX = p[0];
					currY = startY = p[1];
					haveCurr = true;
					break;
				}
				case LINE_TO: {
					if (!haveCurr) {
						transform.apply(cmd.x1, cmd.y1, p);
						currX = startX = p[0];
						currY = startY = p[1];
						haveCurr = true;
					} else {
						transform.apply(cmd.x1, cmd.y1, p);
						float x1 = p[0], y1 = p[1];
						if (currX != x1 || currY != y1) {
							edges.add(new Edge(currX, currY, x1, y1));
						}
						currX = x1;
						currY = y1;
					}
					break;
				}
				case QUAD_TO: {
					if (!haveCurr) {
						transform.apply(cmd.x1, cmd.y1, p);
						currX = startX = p[0];
						currY = startY = p[1];
						haveCurr = true;
					} else {
						transform.apply(cmd.x2, cmd.y2, c);
						float cx = c[0], cy = c[1];
						transform.apply(cmd.x1, cmd.y1, p);
						float x1 = p[0], y1 = p[1];
						flattenQuad(edges, currX, currY, cx, cy, x1, y1);
						currX = x1;
						currY = y1;
					}
					break;
				}
				case CLOSE: {
					if (haveCurr && (currX != startX || currY != startY)) {
						edges.add(new Edge(currX, currY, startX, startY));
					}
					haveCurr = false;
					break;
				}
			}
		}

		return edges;
	}

	/**
	 * Flatten a quadratic bezier into line segments.
	 * Simple fixed-step subdivision; good enough for first pass.
	 */
	private static void flattenQuad(List<Edge> edges,
									float x0, float y0,
									float cx, float cy,
									float x1, float y1) {
		float prevX = x0;
		float prevY = y0;

		for (int i = 1; i <= QUAD_FLATTEN_STEPS; i++) {
			float t = i / (float) QUAD_FLATTEN_STEPS;
			float u = 1.0f - t;

			// Quadratic interpolation
			float xt = u * u * x0 + 2 * u * t * cx + t * t * x1;
			float yt = u * u * y0 + 2 * u * t * cy + t * t * y1;

			if (!(prevX == xt && prevY == yt)) {
				edges.add(new Edge(prevX, prevY, xt, yt));
			}
			prevX = xt;
			prevY = yt;
		}
	}

	private static void fillEdgesToAlpha(List<Edge> edges,
										 byte[] alphaMask,
										 int ssWidth, int ssHeight,
										 int ssX0, int ssY0) {
		if (ssWidth <= 0 || ssHeight <= 0) return;

		fillEdgesGeneric(edges,
			ssY0, ssY0 + ssHeight,
			(y, xStart, xEnd) -> {
				int xs = Math.max(ssX0, xStart);
				int xe = Math.min(ssX0 + ssWidth - 1, xEnd);
				for (int x = xs; x <= xe; x++) {
					setAlphaSample(alphaMask, ssWidth, ssX0, ssY0, x, y);
				}
			});
	}


	private static void fillEdgesGeneric(List<Edge> edges,
										 int yMinInclusive,
										 int yMaxExclusive,
										 SpanConsumer spanConsumer) {
		if (edges.isEmpty()) return;

		float minY = Float.POSITIVE_INFINITY;
		float maxY = Float.NEGATIVE_INFINITY;
		for (Edge e : edges) {
			if (e.y0 < minY) minY = e.y0;
			if (e.y1 < minY) minY = e.y1;
			if (e.y0 > maxY) maxY = e.y0;
			if (e.y1 > maxY) maxY = e.y1;
		}
		if (!Float.isFinite(minY) || !Float.isFinite(maxY)) return;

		int yStart = Math.max(yMinInclusive, (int) Math.floor(minY));
		int yEndExclusiveClamped = Math.min(yMaxExclusive, (int) Math.ceil(maxY) + 1);

		List<EdgeIntersection> intersections = new ArrayList<>(edges.size());

		for (int y = yStart; y < yEndExclusiveClamped; y++) {
			float scanY = y + 0.5f;
			intersections.clear();

			// Collect intersections (x, windingDelta) with this scanline
			for (Edge e : edges) {
				if (e.y0 == e.y1) continue; // horizontal edges don't contribute

				float y0 = e.y0;
				float y1 = e.y1;
				float yMinEdge = Math.min(y0, y1);
				float yMaxEdge = Math.max(y0, y1);

				// [yMin, yMax) rule to avoid double-counting vertices
				if (scanY < yMinEdge || scanY >= yMaxEdge) {
					continue;
				}

				float t = (scanY - e.y0) / (e.y1 - e.y0);
				float x = e.x0 + t * (e.x1 - e.x0);

				// In screen coords (y down), "downwards" edge gets +1, "upwards" -1.
				// Global sign doesn't matter; only non-zero vs zero does.
				int windingDelta = (e.y1 > e.y0) ? +1 : -1;
				intersections.add(new EdgeIntersection(x, windingDelta));
			}

			if (intersections.isEmpty()) continue;

			intersections.sort(Comparator.comparingDouble(i -> i.x));

			int winding = 0;
			EdgeIntersection prev = null;

			// Non-zero winding: fill between consecutive intersections where winding != 0
			for (EdgeIntersection it : intersections) {
				if (prev != null && winding != 0) {
					float x0 = prev.x;
					float x1 = it.x;
					if (x1 > x0) {
						int xStart = (int) Math.ceil(x0);
						int xEnd = (int) Math.floor(x1);
						if (xEnd >= xStart) {
							spanConsumer.fillSpan(y, xStart, xEnd);
						}
					}
				}

				// Update winding *after* using prev->current segment
				winding += it.windingDelta;
				prev = it;
			}
		}
	}

	/**
	 * Load glyph; if missing, draw missing-glyph box and return null.
	 * @return loaded Glyph, or null if missing/empty
	 */
	private static Glyph requireGlyph(TrueTypeFont font,
									  int glyphId,
									  RasterTarget dest,
									  float sizePx,
									  int originX,
									  int baselineY,
									  int argb) {
		Glyph glyph = font.loadGlyph(glyphId);
		if (glyphId == 0 || glyph == null) {
			drawMissingGlyphBox(dest, sizePx, originX, baselineY, argb);
			return null;
		}
		if (glyph.isEmpty()) {
			return null;
		}
		return glyph;
	}

	/**
	 * A target to rasterize into.
	 */
	public interface RasterTarget {
		/**
		 * Get width of the target in pixels.
		 * @return width in pixels
		 */
		int getWidth();

		/**
		 * Get height of the target in pixels.
		 * @return height in pixels
		 */
		int getHeight();
		/**
		 * Set pixel at (x,y) to given ARGB color.
		 * @param x X coordinate
		 * @param y Y coordinate
		 * @param argb ARGB color
		 */
		void setRGB(int x, int y, int argb);

		/**
		 * Get pixel ARGB color at (x,y).
		 * @param x X coordinate
		 * @param y Y coordinate
		 * @return ARGB color
		 */
		int getRGB(int x, int y);
	}

	/**
	 * Process a glyph by its gid.
	 */
	@FunctionalInterface
	private interface GlyphProcessor {
		/**
		 * Draws the glyph with given gid at (x,y) in img, using font at sizePx.
		 * Returns the advance width in font units.
		 * (This is used to generalize over different drawing methods, or for measuring only.)
		 *
		 * @param font TrueTypeFont to draw from
		 * @param img RasterTarget to draw into
		 * @param gid glyph ID to draw
		 * @param sizePx font size in pixels
		 * @param x Where to draw (originX)
		 * @param y Where to draw (baselineY)
		 * @param argb color to draw with
		 * @param supersample supersampling factor
		 * @return advance width in font units
		 */
		double processGlyph(TrueTypeFont font, RasterTarget img, int gid, float sizePx,
							int x, int y, int argb, int supersample);
	}

	/**
	 * Transform font coords to target coords.
	 */
	@FunctionalInterface
	private interface CoordTransform {
		void apply(float xFont, float yFont, float[] out); // out[0] = x, out[1] = y
	}

	/**
	 * Consume a horizontal span on a given scanline.
	 */
	@FunctionalInterface
	private interface SpanConsumer {
		void fillSpan(int y, int xStart, int xEnd);
	}

	/**
	 * Edge intersection with scanline.
	 */
	@Data
	private static final class EdgeIntersection {
		final float x;
		final int windingDelta;
	}

	@Data
	private static final class Edge {
		final float x0, y0, x1, y1;
	}

	@ToString
	private static final class GlyphKey {
		final TrueTypeFont font;
		final int glyphId;
		final float sizePx;
		final int supersample;
		private final int hash;

		GlyphKey(TrueTypeFont font, int glyphId, float sizePx, int supersample) {
			this.font = font;
			this.glyphId = glyphId;
			this.sizePx = sizePx;
			this.supersample = supersample;

			int h = System.identityHashCode(font);
			h = 31 * h + glyphId;
			h = 31 * h + Float.floatToIntBits(sizePx);
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
				&& this.supersample == other.supersample;
		}

		@Override
		public int hashCode() {
			return hash;
		}
	}

	private static final class CachedGlyph {
		final int ssX0;      // supersampled origin (for originX=0, baselineY=0)
		final int ssY0;
		final int ssWidth;
		final int ssHeight;
		final byte[] alphaMask;

		CachedGlyph(int ssX0, int ssY0, int ssWidth, int ssHeight, byte[] alphaMask) {
			this.ssX0 = ssX0;
			this.ssY0 = ssY0;
			this.ssWidth = ssWidth;
			this.ssHeight = ssHeight;
			this.alphaMask = alphaMask;
		}
	}
}
