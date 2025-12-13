package me.mdbell.awtea.classlib.java.awt.geom;

import lombok.SneakyThrows;
import me.mdbell.awtea.classlib.java.awt.TRectangle;
import me.mdbell.awtea.classlib.java.awt.TShape;

import java.awt.geom.Rectangle2D;

public abstract class TRectangularShape implements TShape, Cloneable {

	protected TRectangularShape() {

	}

	public abstract double getX();
	public abstract double getY();
	public abstract double getWidth();
	public abstract double getHeight();

	public double getMinX() {
		return getX();
	}

	public double getMinY() {
		return getY();
	}

	public double getMaxX() {
		return getX() + getWidth();
	}

	public double getMaxY() {
		return getY() + getHeight();
	}

	public double getCenterX() {
		return getX() + getWidth() / 2.0;
	}

	public double getCenterY() {
		return getY() + getHeight() / 2.0;
	}

	public Rectangle2D getFrame() {
		return new Rectangle2D.Double(getX(), getY(), getWidth(), getHeight());
	}

	public abstract boolean isEmpty();

	public abstract void setFrame(double x, double y, double w, double h);

	public void setFrame(TPoint2D loc, TDimension2D size) {
		setFrame(loc.getX(), loc.getY(), size.getWidth(), size.getHeight());
	}

	public void setFrame(TRectangle2D r) {
		setFrame(r.getX(), r.getY(), r.getWidth(), r.getHeight());
	}

	public void setFrameFromDiagonal(double x1, double y1, double x2, double y2) {
		double x = Math.min(x1, x2);
		double y = Math.min(y1, y2);
		double w = Math.abs(x2 - x1);
		double h = Math.abs(y2 - y1);
		setFrame(x, y, w, h);
	}

	public void setFrameFromDiagonal(TPoint2D p1, TPoint2D p2) {
		setFrameFromDiagonal(p1.getX(), p1.getY(), p2.getX(), p2.getY());
	}

	public void setFrameFromCenter(double centerX, double centerY, double cornerX, double cornerY) {
		double x = Math.min(centerX, cornerX);
		double y = Math.min(centerY, cornerY);
		double w = Math.abs(cornerX - centerX) * 2;
		double h = Math.abs(cornerY - centerY) * 2;
		setFrame(x, y, w, h);
	}

	public void setFrameFromCenter(TPoint2D center, TPoint2D corner) {
		setFrameFromCenter(center.getX(), center.getY(), corner.getX(), corner.getY());
	}

	public boolean contains(TPoint2D p) {
		return contains(p.getX(), p.getY());
	}

	public boolean intersects(TRectangle2D r) {
		return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
	}

	public boolean contains(TRectangle2D r) {
		return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
	}

	public TRectangle getBounds() {
		double width = getWidth();
		double height = getHeight();
		if (width < 0 || height < 0) {
			return new TRectangle();
		}
		return new TRectangle((int)Math.floor(getX()), (int)Math.floor(getY()),
				(int)Math.ceil(getWidth()), (int)Math.ceil(getHeight()));
	}

	@SneakyThrows
	public Object clone() {
		return super.clone();
	}
}
