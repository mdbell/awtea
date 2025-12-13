package me.mdbell.awtea.classlib.java.awt.geom;

import lombok.*;

/**
 * @see java.awt.geom.Point2D
 */
@EqualsAndHashCode
public abstract class TPoint2D implements Cloneable{

	protected TPoint2D() {

	}

	public abstract double getX();
	public abstract double getY();

	public abstract void setLocation(double x, double y);

	public void setLocation(TPoint2D p) {
		setLocation(p.getX(), p.getY());
	}

	public double distance(double px, double py) {
		return distance(getX(), getY(), px, py);
	}

	public double distance(TPoint2D p) {
		return distance(p.getX(), p.getY());
	}

	public double distanceSq(double px, double py) {
		return distanceSq(getX(), getY(), px, py);
	}

	public double distanceSq(TPoint2D p) {
		return distanceSq(p.getX(), p.getY());
	}

	public static double distance(double x1, double y1, double x2, double y2) {
		double dx = x1 - x2;
		double dy = y1 - y2;
		return Math.sqrt(dx * dx + dy * dy);
	}

	public static double distanceSq(double x1, double y1, double x2, double y2) {
		double dx = x1 - x2;
		double dy = y1 - y2;
		return dx * dx + dy * dy;
	}

	@SneakyThrows
	@Override
	public Object clone() {
		return super.clone();
	}

	@AllArgsConstructor
	@NoArgsConstructor
	@ToString
	@EqualsAndHashCode(callSuper = false)
	public static class Float extends TPoint2D {
		protected float x;
		protected float y;

		@Override
		public double getX() {
			return x;
		}

		@Override
		public double getY() {
			return y;
		}

		@Override
		public void setLocation(double x, double y) {
			this.x = (float) x;
			this.y = (float) y;
		}
	}

	@AllArgsConstructor
	@NoArgsConstructor
	@ToString
	@EqualsAndHashCode(callSuper = false)
	public static class Double extends TPoint2D {
		public double x;
		public double y;

		@Override
		public double getX() {
			return x;
		}

		@Override
		public double getY() {
			return y;
		}

		@Override
		public void setLocation(double x, double y) {
			this.x = x;
			this.y = y;
		}
	}
}
