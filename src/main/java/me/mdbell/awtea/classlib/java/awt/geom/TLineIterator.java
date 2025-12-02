package me.mdbell.awtea.classlib.java.awt.geom;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class TLineIterator implements TPathIterator{

	private final TLine2D line;
	private final TAffineTransform transform;
	private int index = 0;

	@Override
	public int getWindingRule() {
		return WIND_NON_ZERO;
	}

	@Override
	public boolean isDone() {
		return this.index > 1;
	}

	@Override
	public void next() {
		this.index++;
	}

	@Override
	public int currentSegment(double[] coords) {
		if(this.isDone()) {
			throw new IllegalStateException("Iterator out of bounds");
		}
		int type;
		if(index == 0) {
			type = TPathIterator.SEG_MOVETO;
			coords[0] = line.getX1();
			coords[1] = line.getY1();
		} else {
			type = TPathIterator.SEG_LINETO;
			coords[0] = line.getX2();
			coords[1] = line.getY2();
		}
		if(transform != null) {
			transform.transform(coords, 0, coords, 0, 1);
		}
		return type;
	}

	@Override
	public int currentSegment(float[] coords) {
		if(this.isDone()) {
			throw new IllegalStateException("Iterator out of bounds");
		}
		int type;
		if(index == 0) {
			type = TPathIterator.SEG_MOVETO;
			coords[0] = (float) line.getX1();
			coords[1] = (float) line.getY1();
		} else {
			type = TPathIterator.SEG_LINETO;
			coords[0] = (float) line.getX2();
			coords[1] = (float) line.getY2();
		}
		if(transform != null) {
			transform.transform(coords, 0, coords, 0, 1);
		}
		return type;
	}
}
