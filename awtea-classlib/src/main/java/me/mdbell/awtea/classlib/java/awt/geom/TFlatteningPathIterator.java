package me.mdbell.awtea.classlib.java.awt.geom;

import java.util.Arrays;

public class TFlatteningPathIterator implements TPathIterator {

	private final TPathIterator source;
	private final double flatnessSquared;
	private final int limit;

	private boolean done;

	// current segment type (only MOVETO, LINETO, CLOSE will be exposed)
	private int segType;
	// current segment endpoint coords (x,y)
	private final double[] segCoords = new double[2];

	// underlying segment coords buffer from source
	private final double[] srcCoords = new double[6];

	// current point and current subpath start (for CLOSE)
	private double curx, cury;
	private double movx, movy;

	// buffer of flattened points for the current curve
	// stores [x0, y0, x1, y1, ...] endpoints AFTER the curve start
	private double[] buf;
	private int bufIndex; // index into buf (0..bufCount-1)
	private int bufCount; // number of used doubles in buf

	public TFlatteningPathIterator(TPathIterator src, double flatness) {
		this(src, flatness, 10);
	}

	public TFlatteningPathIterator(TPathIterator src, double flatness, int limit) {
		if (flatness < 0) {
			throw new IllegalArgumentException("flatness must be >= 0");
		}
		if (limit < 0) {
			throw new IllegalArgumentException("limit must be >= 0");
		}

		this.source = src;
		this.flatnessSquared = flatness * flatness;
		this.limit = limit;

		if (src.isDone()) {
			done = true;
		} else {
			advance(); // prime first segment
		}
	}

	public double getFlatness() {
		return Math.sqrt(flatnessSquared);
	}

	public int getRecursionLimit() {
		return limit;
	}

	@Override
	public int getWindingRule() {
		return source.getWindingRule();
	}

	@Override
	public boolean isDone() {
		return done;
	}

	@Override
	public void next() {
		if (done) {
			return;
		}
		advance();
	}

	@Override
	public int currentSegment(double[] coords) {
		if (done) {
			throw new IllegalStateException("Iterator out of bounds");
		}
		int type = segType;
		if (type != TPathIterator.SEG_CLOSE) {
			coords[0] = segCoords[0];
			coords[1] = segCoords[1];
		}
		return type;
	}

	@Override
	public int currentSegment(float[] coords) {
		if (done) {
			throw new IllegalStateException("Iterator out of bounds");
		}
		int type = segType;
		if (type != TPathIterator.SEG_CLOSE) {
			coords[0] = (float) segCoords[0];
			coords[1] = (float) segCoords[1];
		}
		return type;
	}

	// --------- Core iteration logic ---------

	/** Advance to the next flattened segment. */
	private void advance() {
		// 1) If we still have buffered points from a flattened curve, emit a LINETO from there.
		if (bufIndex < bufCount) {
			segType = TPathIterator.SEG_LINETO;
			segCoords[0] = buf[bufIndex++];
			segCoords[1] = buf[bufIndex++];
			curx = segCoords[0];
			cury = segCoords[1];

			if (bufIndex >= bufCount) {
				bufIndex = bufCount = 0;
			}
			return;
		}

		// 2) Consume the next segment from the source iterator

		if(source.isDone()) {
			done = true;
			return;
		}

		int type = source.currentSegment(srcCoords);

		switch (type) {
			case TPathIterator.SEG_MOVETO: {
				movx = curx = srcCoords[0];
				movy = cury = srcCoords[1];
				segType = TPathIterator.SEG_MOVETO;
				segCoords[0] = curx;
				segCoords[1] = cury;
				source.next();
				return;
			}

			case TPathIterator.SEG_LINETO: {
				curx = srcCoords[0];
				cury = srcCoords[1];
				segType = TPathIterator.SEG_LINETO;
				segCoords[0] = curx;
				segCoords[1] = cury;
				source.next();
				return;
			}

			case TPathIterator.SEG_CLOSE: {
				// Close the current subpath
				curx = movx;
				cury = movy;
				segType = TPathIterator.SEG_CLOSE;
				// coords unused for CLOSE
				source.next();
				return;
			}

			case TPathIterator.SEG_QUADTO: {
				// Flatten quadratic curve from (curx, cury) to (x2, y2) with control (x1, y1)
				double x0 = curx;
				double y0 = cury;
				double x1 = srcCoords[0];
				double y1 = srcCoords[1];
				double x2 = srcCoords[2];
				double y2 = srcCoords[3];

				bufIndex = bufCount = 0;
				flattenQuad(x0, y0, x1, y1, x2, y2, 0);

				// The buffer now has a polyline of endpoints after (x0, y0)
				if (bufCount > 0) {
					bufIndex = 0;
					segType = TPathIterator.SEG_LINETO;
					segCoords[0] = buf[bufIndex++];
					segCoords[1] = buf[bufIndex++];
					curx = segCoords[0];
					cury = segCoords[1];
				}
				source.next();
				return;
			}

			case TPathIterator.SEG_CUBICTO: {
				// Flatten cubic curve from (curx, cury) to (x3, y3) with controls (x1, y1), (x2, y2)
				double x0 = curx;
				double y0 = cury;
				double x1 = srcCoords[0];
				double y1 = srcCoords[1];
				double x2 = srcCoords[2];
				double y2 = srcCoords[3];
				double x3 = srcCoords[4];
				double y3 = srcCoords[5];

				bufIndex = bufCount = 0;
				flattenCubic(x0, y0, x1, y1, x2, y2, x3, y3, 0);

				if (bufCount > 0) {
					bufIndex = 0;
					segType = TPathIterator.SEG_LINETO;
					segCoords[0] = buf[bufIndex++];
					segCoords[1] = buf[bufIndex++];
					curx = segCoords[0];
					cury = segCoords[1];
				}
				source.next();
				return;
			}

			default:
				throw new IllegalStateException("Unknown segment type: " + type);
		}
	}

	// --------- Flattening helpers ---------

	private void ensureBufCapacity(int extraPairs) {
		int needed = bufCount + 2 * extraPairs;
		if (buf == null) {
			buf = new double[Math.max(needed, 16)];
		} else if (needed > buf.length) {
			int newLen = buf.length * 2;
			while (newLen < needed) {
				newLen *= 2;
			}
			buf = Arrays.copyOf(buf, newLen);
		}
	}

	private void addPoint(double x, double y) {
		ensureBufCapacity(1);
		buf[bufCount++] = x;
		buf[bufCount++] = y;
	}

	// ---- Quadratic flattening ----

	private void flattenQuad(double x0, double y0,
							 double cx, double cy,
							 double x1, double y1,
							 int level) {
		if (level >= limit) {
			// recursion limit reached, accept endpoint
			addPoint(x1, y1);
			return;
		}

		if (quadFlatnessSq(x0, y0, cx, cy, x1, y1) <= flatnessSquared) {
			// segment is flat enough: approximate with straight line to endpoint
			addPoint(x1, y1);
			return;
		}

		// Subdivide
		double x01 = (x0 + cx) * 0.5;
		double y01 = (y0 + cy) * 0.5;
		double x12 = (cx + x1) * 0.5;
		double y12 = (cy + y1) * 0.5;
		double x012 = (x01 + x12) * 0.5;
		double y012 = (y01 + y12) * 0.5;

		// First half: (x0, y0, x01, y01, x012, y012)
		flattenQuad(x0, y0, x01, y01, x012, y012, level + 1);
		// Second half: (x012, y012, x12, y12, x1, y1)
		flattenQuad(x012, y012, x12, y12, x1, y1, level + 1);
	}

	private double quadFlatnessSq(double x0, double y0,
								  double cx, double cy,
								  double x1, double y1) {
		// Use distance from control point to the line from (x0, y0) -> (x1, y1)
		return pointLineDistSq(cx, cy, x0, y0, x1, y1);
	}

	// ---- Cubic flattening ----

	private void flattenCubic(double x0, double y0,
							  double x1, double y1,
							  double x2, double y2,
							  double x3, double y3,
							  int level) {
		if (level >= limit) {
			addPoint(x3, y3);
			return;
		}

		if (cubicFlatnessSq(x0, y0, x1, y1, x2, y2, x3, y3) <= flatnessSquared) {
			addPoint(x3, y3);
			return;
		}

		// Subdivide cubic at t=0.5 using De Casteljau
		double x01 = (x0 + x1) * 0.5;
		double y01 = (y0 + y1) * 0.5;
		double x12 = (x1 + x2) * 0.5;
		double y12 = (y1 + y2) * 0.5;
		double x23 = (x2 + x3) * 0.5;
		double y23 = (y2 + y3) * 0.5;

		double x012 = (x01 + x12) * 0.5;
		double y012 = (y01 + y12) * 0.5;
		double x123 = (x12 + x23) * 0.5;
		double y123 = (y12 + y23) * 0.5;

		double x0123 = (x012 + x123) * 0.5;
		double y0123 = (y012 + y123) * 0.5;

		// First half: (x0, y0, x01, y01, x012, y012, x0123, y0123)
		flattenCubic(x0, y0, x01, y01, x012, y012, x0123, y0123, level + 1);
		// Second half: (x0123, y0123, x123, y123, x23, y23, x3, y3)
		flattenCubic(x0123, y0123, x123, y123, x23, y23, x3, y3, level + 1);
	}

	private double cubicFlatnessSq(double x0, double y0,
								   double x1, double y1,
								   double x2, double y2,
								   double x3, double y3) {
		// Max distance of control points from line between endpoints
		double d1 = pointLineDistSq(x1, y1, x0, y0, x3, y3);
		double d2 = pointLineDistSq(x2, y2, x0, y0, x3, y3);
		return Math.max(d1, d2);
	}

	// ---- Geometry helper ----

	private double pointLineDistSq(double px, double py,
								   double x1, double y1,
								   double x2, double y2) {
		double dx = x2 - x1;
		double dy = y2 - y1;
		if (dx == 0 && dy == 0) {
			// line is a point
			dx = px - x1;
			dy = py - y1;
			return dx * dx + dy * dy;
		}
		double t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
		double projx = x1 + t * dx;
		double projy = y1 + t * dy;
		double ddx = px - projx;
		double ddy = py - projy;
		return ddx * ddx + ddy * ddy;
	}
}
