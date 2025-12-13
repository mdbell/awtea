package me.mdbell.awtea.font.tables;

import me.mdbell.awtea.font.ByteReader;
import me.mdbell.awtea.font.GlyfFlags;
import me.mdbell.awtea.font.Glyph;

public final class GlyfTable implements GlyfFlags {
	private final ByteReader glyfReader;  // scoped to whole glyf table
	private final LocaTable loca;
	private final MaxpTable maxp;

	public GlyfTable(ByteReader glyfReader, LocaTable loca, MaxpTable maxp) {
		this.glyfReader = glyfReader;
		this.loca = loca;
		this.maxp = maxp;
	}

	public Glyph loadGlyph(int glyphId) {
		return loadGlyphInternal(glyphId, 0);
	}

	private Glyph loadGlyphInternal(int glyphId, int depth) {
		if (glyphId < 0 || glyphId >= maxp.getNumGlyphs()) {
			return null;
		}

		int offset = loca.getGlyphOffset(glyphId);
		int length = loca.getGlyphLength(glyphId);

		if (length <= 0) {
			// empty glyph (e.g., space)
			return new Glyph(glyphId, 0, (short)0, (short)0, (short)0, (short)0,
				null, null, null, null);
		}

		ByteReader r = glyfReader.forkRelative(offset);

		int numberOfContours = r.readInt16();
		short xMin = r.readInt16();
		short yMin = r.readInt16();
		short xMax = r.readInt16();
		short yMax = r.readInt16();

		if (numberOfContours < 0) {
			return loadCompositeGlyph(glyphId, xMin, yMin, xMax, yMax, r, depth);
		}

		if (numberOfContours == 0) {
			// No contours, but still record bounds
			return new Glyph(glyphId, 0, xMin, yMin, xMax, yMax,
				new int[0], new int[0], new int[0], new boolean[0]);
		}

		// --- endPtsOfContours ---
		int[] endPts = new int[numberOfContours];
		for (int i = 0; i < numberOfContours; i++) {
			endPts[i] = r.readUInt16();
		}

		// --- instructions (ignore content) ---
		int instructionLength = r.readUInt16();
		r.skip(instructionLength);

		int numPoints = endPts[numberOfContours - 1] + 1;

		// --- flags, with repeats ---
		int[] flags = new int[numPoints];
		int pointIndex = 0;
		while (pointIndex < numPoints) {
			int flag = r.readUInt8();
			flags[pointIndex++] = flag;

			if ((flag & REPEAT_FLAG) != 0) {
				int repeatCount = r.readUInt8();
				for (int j = 0; j < repeatCount && pointIndex < numPoints; j++) {
					flags[pointIndex++] = flag;
				}
			}
		}

		int[] x = new int[numPoints];
		int[] y = new int[numPoints];
		boolean[] onCurve = new boolean[numPoints];

		// --- X coordinates ---
		int currentX = 0;
		for (int i = 0; i < numPoints; i++) {
			int flag = flags[i];
			onCurve[i] = (flag & ON_CURVE) != 0;

			if ((flag & X_SHORT_VECTOR) != 0) {
				int dx = r.readUInt8();
				if ((flag & X_IS_SAME_OR_POS) != 0) {
					currentX += dx;
				} else {
					currentX -= dx;
				}
			} else {
				if ((flag & X_IS_SAME_OR_POS) != 0) {
					// Same as previous, delta = 0, no data
				} else {
					int dx = r.readInt16();
					currentX += dx;
				}
			}

			x[i] = currentX;
		}

		// --- Y coordinates ---
		int currentY = 0;
		for (int i = 0; i < numPoints; i++) {
			int flag = flags[i];

			if ((flag & Y_SHORT_VECTOR) != 0) {
				int dy = r.readUInt8();
				if ((flag & Y_IS_SAME_OR_POS) != 0) {
					currentY += dy;
				} else {
					currentY -= dy;
				}
			} else {
				if ((flag & Y_IS_SAME_OR_POS) != 0) {
					// Same as previous, delta = 0, no data
				} else {
					int dy = r.readInt16();
					currentY += dy;
				}
			}

			y[i] = currentY;
		}

		return new Glyph(glyphId, numberOfContours, xMin, yMin, xMax, yMax,
			endPts, x, y, onCurve);
	}

	private Glyph loadCompositeGlyph(int glyphId,
									 short xMin, short yMin,
									 short xMax, short yMax,
									 ByteReader r,
									 int depth) {
		// Prevent pathological recursion
		if (depth > 8) {
			return null;
		}

		// These will hold the flattened outline
		java.util.List<Integer> allX = new java.util.ArrayList<>();
		java.util.List<Integer> allY = new java.util.ArrayList<>();
		java.util.List<Boolean> allOnCurve = new java.util.ArrayList<>();
		java.util.List<Integer> endPts = new java.util.ArrayList<>();

		int contourOffset = 0;

		boolean moreComponents;
		do {
			int flags = r.readUInt16();
			int componentGlyphIndex = r.readUInt16();

			// Arguments: either words or bytes
			int arg1, arg2;
			if ((flags & ARG_1_AND_2_ARE_WORDS) != 0) {
				arg1 = r.readInt16();
				arg2 = r.readInt16();
			} else {
				arg1 = (byte) r.readUInt8(); // sign-extend
				arg2 = (byte) r.readUInt8();
			}

			int dx = 0, dy = 0;
			if ((flags & ARGS_ARE_XY_VALUES) != 0) {
				// args are x,y offsets
				dx = arg1;
				dy = arg2;
			} else {
				// args are point indices; for now, ignore & treat as no offset
				// (good enough for many simple fonts)
			}

			// Handle transforms – for now, we support only translation.
			// If you want to support scaling later, parse and use these:
			float m00 = 1f, m01 = 0f;
			float m10 = 0f, m11 = 1f;

			if ((flags & WE_HAVE_A_SCALE) != 0) {
				// single scale value
				short scale = r.readInt16();
				float s = scale / 16384.0f;
				m00 = m11 = s;
			} else if ((flags & WE_HAVE_AN_X_AND_Y_SCALE) != 0) {
				short xScale = r.readInt16();
				short yScale = r.readInt16();
				m00 = xScale / 16384.0f;
				m11 = yScale / 16384.0f;
			} else if ((flags & WE_HAVE_A_TWO_BY_TWO) != 0) {
				short m00s = r.readInt16();
				short m01s = r.readInt16();
				short m10s = r.readInt16();
				short m11s = r.readInt16();
				m00 = m00s / 16384.0f;
				m01 = m01s / 16384.0f;
				m10 = m10s / 16384.0f;
				m11 = m11s / 16384.0f;
			}

			// Recursively load the component glyph
			Glyph component = loadGlyphInternal(componentGlyphIndex, depth);
			if (component != null && component.numberOfContours > 0 &&
				component.x != null && component.y != null && component.onCurve != null) {

				int numPoints = component.x.length;

				for (int i = 0; i < numPoints; i++) {
					int cx = component.x[i];
					int cy = component.y[i];

					// Apply 2x2 transform + translation (if we care about scaling)
					int tx = Math.round(cx * m00 + cy * m01) + dx;
					int ty = Math.round(cx * m10 + cy * m11) + dy;

					allX.add(tx);
					allY.add(ty);
					allOnCurve.add(component.onCurve[i]);
				}

				// Merge end points with offset
				for (int i = 0; i < component.endPtsOfContours.length; i++) {
					int endPt = component.endPtsOfContours[i] + contourOffset;
					endPts.add(endPt);
				}

				contourOffset += numPoints;
			}

			moreComponents = (flags & MORE_COMPONENTS) != 0;

			// Skip any instructions if present (we don't hint)
			if (!moreComponents && (flags & WE_HAVE_INSTRUCTIONS) != 0) {
				int instructionLength = r.readUInt16();
				r.skip(instructionLength);
			}

		} while (moreComponents);

		int contourCount = endPts.size();
		if (contourCount == 0) {
			// Could not resolve anything, treat as empty
			return new Glyph(glyphId, 0, xMin, yMin, xMax, yMax,
				new int[0], new int[0], new int[0], new boolean[0]);
		}

		int[] endPtsArr = new int[contourCount];
		for (int i = 0; i < contourCount; i++) {
			endPtsArr[i] = endPts.get(i);
		}

		int numPoints = allX.size();
		int[] xArr = new int[numPoints];
		int[] yArr = new int[numPoints];
		boolean[] onCurveArr = new boolean[numPoints];

		for (int i = 0; i < numPoints; i++) {
			xArr[i] = allX.get(i);
			yArr[i] = allY.get(i);
			onCurveArr[i] = allOnCurve.get(i);
		}

		return new Glyph(glyphId, contourCount, xMin, yMin, xMax, yMax,
			endPtsArr, xArr, yArr, onCurveArr);
	}

}

