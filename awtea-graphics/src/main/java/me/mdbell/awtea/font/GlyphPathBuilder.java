package me.mdbell.awtea.font;

import java.util.ArrayList;
import java.util.List;

public final class GlyphPathBuilder {

	private static final class Pt {
		final float x, y;
		final boolean onCurve;
		Pt(float x, float y, boolean onCurve) {
			this.x = x;
			this.y = y;
			this.onCurve = onCurve;
		}
	}

	public static GlyphPath buildPath(Glyph glyph) {
		GlyphPath path = new GlyphPath();

		if (glyph == null || glyph.numberOfContours <= 0 || glyph.x == null) {
			return path; // empty
		}

		int contourStart = 0;
		for (int c = 0; c < glyph.numberOfContours; c++) {
			int contourEnd = glyph.endPtsOfContours[c]; // inclusive
			buildContour(path, glyph, contourStart, contourEnd);
			contourStart = contourEnd + 1;
		}

		return path;
	}

	private static void buildContour(GlyphPath path, Glyph g, int start, int end) {
		int num = end - start + 1;
		if (num <= 0) return;

		// 1) Collect original points into a list with wrap-around
		List<Pt> pts = new ArrayList<>(num + 1);
		for (int i = start; i <= end; i++) {
			pts.add(new Pt(g.x[i], g.y[i], g.onCurve[i]));
		}

		// This contour is closed; wrap around for implicit logic
		// We'll handle wrap with indices mod pts.size() later.

		// 2) Insert implicit on-curve points between consecutive off-curve points
		List<Pt> ext = new ArrayList<>();
		int n = pts.size();
		for (int i = 0; i < n; i++) {
			Pt p = pts.get(i);
			Pt next = pts.get((i + 1) % n);

			ext.add(p);

			if (!p.onCurve && !next.onCurve) {
				// Insert implicit on-curve at midpoint
				float mx = (p.x + next.x) * 0.5f;
				float my = (p.y + next.y) * 0.5f;
				ext.add(new Pt(mx, my, true));
			}
		}

		// 3) If first point is off-curve, insert implicit on-curve between last and first
		if (!ext.get(0).onCurve) {
			Pt last = ext.get(ext.size() - 1);
			Pt first = ext.get(0);
			float mx = (last.x + first.x) * 0.5f;
			float my = (last.y + first.y) * 0.5f;
			// Insert at front
			ext.add(0, new Pt(mx, my, true));
		}

		// Now ext should start with an on-curve point
		// and we should never have two consecutive off-curve points.

		// 4) Walk and emit commands
		int m = ext.size();
		Pt firstOn = ext.get(0);
		path.moveTo(firstOn.x, firstOn.y);

		int i = 1;
		while (i < m) {
			Pt curr = ext.get(i);

			if (curr.onCurve) {
				// Straight line
				path.lineTo(curr.x, curr.y);
				i++;
			} else {
				// Off-curve: must be followed by an on-curve (due to our processing)
				Pt control = curr;
				Pt endPt = ext.get((i + 1) % m);
				path.quadTo(control.x, control.y, endPt.x, endPt.y);
				i += 2;
			}
		}

		path.closePath();
	}
}
