package me.mdbell.awtea.classlib.java.awt;

import lombok.RequiredArgsConstructor;
import me.mdbell.awtea.classlib.java.awt.geom.TAffineTransform;
import me.mdbell.awtea.classlib.java.awt.geom.TPathIterator;
import me.mdbell.awtea.classlib.java.awt.geom.TPoint2D;

/**
 * Simple PathIterator implementation for rectangles.
 */
@RequiredArgsConstructor
class RectanglePathIterator implements TPathIterator {
	private final TRectangle rect;
	private final TAffineTransform transform;
	private int index = 0;

	@Override
	public int getWindingRule() {
		return WIND_NON_ZERO;
	}

	@Override
	public boolean isDone() {
		return index > 4;
	}

	@Override
	public void next() {
		index++;
	}

	@Override
	public int currentSegment(float[] coords) {
		if (isDone()) {
			throw new IllegalStateException("Iterator is done");
		}

		double x = rect.x;
		double y = rect.y;
		double w = rect.width;
		double h = rect.height;

		switch (index) {
			case 0:
				coords[0] = (float) x;
				coords[1] = (float) y;
				if (transform != null) {
					transform.transform(new TPoint2D.Double(coords[0], coords[1]),
						new TPoint2D.Double(coords[0], coords[1]));
				}
				return SEG_MOVETO;
			case 1:
				coords[0] = (float) (x + w);
				coords[1] = (float) y;
				if (transform != null) {
					transform.transform(new TPoint2D.Double(coords[0], coords[1]),
						new TPoint2D.Double(coords[0], coords[1]));
				}
				return SEG_LINETO;
			case 2:
				coords[0] = (float) (x + w);
				coords[1] = (float) (y + h);
				if (transform != null) {
					transform.transform(new TPoint2D.Double(coords[0], coords[1]),
						new TPoint2D.Double(coords[0], coords[1]));
				}
				return SEG_LINETO;
			case 3:
				coords[0] = (float) x;
				coords[1] = (float) (y + h);
				if (transform != null) {
					transform.transform(new TPoint2D.Double(coords[0], coords[1]),
						new TPoint2D.Double(coords[0], coords[1]));
				}
				return SEG_LINETO;
			case 4:
				return SEG_CLOSE;
			default:
				throw new IllegalStateException("Invalid index: " + index);
		}
	}

	@Override
	public int currentSegment(double[] coords) {
		float[] fcoords = new float[6];
		int type = currentSegment(fcoords);
		coords[0] = fcoords[0];
		coords[1] = fcoords[1];
		return type;
	}
}
