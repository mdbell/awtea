package me.mdbell.awtea.font;

import lombok.Getter;

@Getter
public final class Glyph {
	public final int glyphId;
	public final int numberOfContours;   // -1 for empty, <0 for composite (unsupported for now)
	public final short xMin, yMin, xMax, yMax;

	// For simple glyphs only:
	public final int[] endPtsOfContours; // length = numberOfContours
	public final int[] x;                // length = numPoints
	public final int[] y;
	public final boolean[] onCurve;      // same length as x/y

	public Glyph(int glyphId,
				 int numberOfContours,
				 short xMin, short yMin, short xMax, short yMax,
				 int[] endPtsOfContours,
				 int[] x, int[] y, boolean[] onCurve) {
		this.glyphId = glyphId;
		this.numberOfContours = numberOfContours;
		this.xMin = xMin;
		this.yMin = yMin;
		this.xMax = xMax;
		this.yMax = yMax;
		this.endPtsOfContours = endPtsOfContours;
		this.x = x;
		this.y = y;
		this.onCurve = onCurve;
	}

	public boolean isEmpty() {
		return numberOfContours == 0 && (x == null || x.length == 0);
	}

	public boolean isComposite() {
		return numberOfContours < 0;
	}
}

