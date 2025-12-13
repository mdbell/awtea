package me.mdbell.awtea.classlib.java.awt.geom;


import java.util.NoSuchElementException;

class TRectIterator implements TPathIterator{
	private final double x, y, w, h;
	private final TAffineTransform transform;
	private int index = 0;

	public TRectIterator(TRectangle2D rect, TAffineTransform at) {
		this.x = rect.getX();
		this.y = rect.getY();
		this.w = rect.getWidth();
		this.h = rect.getHeight();
		this.transform = at;
		if(w < 0 || h < 0) {
			index = 6;
		}
	}

	@Override
	public int getWindingRule() {
		return WIND_NON_ZERO;
	}

	@Override
	public boolean isDone() {
		return this.index > 5;
	}

	@Override
	public void next() {
		this.index++;
	}

	@Override
	public int currentSegment(double[] coords) {
		if (isDone()) {
			throw new NoSuchElementException("OOB");
		}
		if (index == 5) {
			return SEG_CLOSE;
		}
		coords[0] = x;
		coords[1] = y;
		if (index == 1 || index == 2) {
			coords[0] += w;
		}
		if (index == 2 || index == 3) {
			coords[1] += h;
		}
		if (transform != null) {
			transform.transform(coords, 0, coords, 0, 1);
		}
		return (index == 0 ? SEG_MOVETO : SEG_LINETO);
	}

	@Override
	public int currentSegment(float[] coords) {
		if (isDone()) {
			throw new NoSuchElementException("OOB");
		}
		if (index == 5) {
			return SEG_CLOSE;
		}
		coords[0] = (float) x;
		coords[1] = (float) y;
		if (index == 1 || index == 2) {
			coords[0] += (float) w;
		}
		if (index == 2 || index == 3) {
			coords[1] += h;
		}
		if (transform != null) {
			transform.transform(coords, 0, coords, 0, 1);
		}
		return (index == 0 ? SEG_MOVETO : SEG_LINETO);
	}
}
