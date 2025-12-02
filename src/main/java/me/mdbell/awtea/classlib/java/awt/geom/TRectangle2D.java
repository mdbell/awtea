package me.mdbell.awtea.classlib.java.awt.geom;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.awt.geom.Rectangle2D;

/**
 * @see java.awt.geom.Rectangle2D
 */
public abstract class TRectangle2D extends TRectangularShape {

	public static final int OUT_LEFT = 1;
	public static final int OUT_TOP = 2;
	public static final int OUT_RIGHT = 4;
	public static final int OUT_BOTTOM = 8;

	protected TRectangle2D() {

	}

	public abstract double getX();
	public abstract double getY();
	public abstract double getWidth();
	public abstract double getHeight();

	public abstract void setRect(double x, double y, double w, double h);

	public abstract int outcode(double x, double y);

	public abstract TRectangle2D createIntersection(TRectangle2D r);

	public abstract TRectangle2D createUnion(TRectangle2D r);

	public void setRect(TRectangle2D r) {
		setRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
	}

	public boolean intersectsLine(TLine2D l) {
		return intersectsLine(l.getX1(), l.getY1(), l.getX2(), l.getY2());
	}

	public boolean intersectsLine(double x1, double y1, double x2, double y2) {
		int out1, out2;
		if ((out2 = outcode(x2, y2)) == 0) {
			return true;
		}
		while ((out1 = outcode(x1, y1)) != 0) {
			if ((out1 & out2) != 0) {
				return false;
			}
			if ((out1 & (OUT_LEFT | OUT_RIGHT)) != 0) {
				double x = getX();
				if ((out1 & OUT_RIGHT) != 0) {
					x += getWidth();
				}
				y1 = y1 + (x - x1) * (y2 - y1) / (x2 - x1);
				x1 = x;
			} else {
				double y = getY();
				if ((out1 & OUT_BOTTOM) != 0) {
					y += getHeight();
				}
				x1 = x1 + (y - y1) * (x2 - x1) / (y2 - y1);
				y1 = y;
			}
		}
		return true;
	}

	public int outcode(TPoint2D p) {
		return outcode(p.getX(), p.getY());
	}

	public void setFrame(double x, double y, double w, double h) {
		setRect(x, y, w, h);
	}

	public TRectangle2D getBounds2D() {
		return (TRectangle2D) clone();
	}

	public boolean contains(double x, double y) {
	    return (x >= getX() && x < getX() + getWidth() &&
	            y >= getY() && y < getY() + getHeight());
	}

	public boolean intersects(double x, double y, double w, double h) {
	    if (w <= 0 || h <= 0 || getWidth() <= 0 || getHeight() <= 0) {
	        return false;
	    }
	    return (x + w > getX() &&
	            y + h > getY() &&
	            x < getX() + getWidth() &&
	            y < getY() + getHeight());
	}

	public boolean contains(double x, double y, double w, double h) {
	    if (w <= 0 || h <= 0) {
	        return false;
	    }
	    return (x >= getX() &&
	            y >= getY() &&
	            x + w <= getX() + getWidth() &&
	            y + h <= getY() + getHeight());
	}

	public void add(double newx, double newy) {
	    double x1 = Math.min(getX(), newx);
	    double y1 = Math.min(getY(), newy);
	    double x2 = Math.max(getX() + getWidth(), newx);
	    double y2 = Math.max(getY() + getHeight(), newy);
	    setRect(x1, y1, x2 - x1, y2 - y1);
	}

	public void add(TPoint2D pt) {
	    add(pt.getX(), pt.getY());
	}

	public void add(TRectangle2D r) {
	    double x1 = Math.min(getX(), r.getX());
	    double y1 = Math.min(getY(), r.getY());
	    double x2 = Math.max(getX() + getWidth(), r.getX() + r.getWidth());
	    double y2 = Math.max(getY() + getHeight(), r.getY() + r.getHeight());
	    setRect(x1, y1, x2 - x1, y2 - y1);
	}

	@Override
	public TPathIterator getPathIterator(TAffineTransform at) {
	    return new TRectIterator(this, at);
	}

	@Override
	public TPathIterator getPathIterator(TAffineTransform at, double flatness) {
	    return getPathIterator(at);
	}

	@Override
	public int hashCode() {
	    long bits = java.lang.Double.doubleToLongBits(getX());
	    bits += java.lang.Double.doubleToLongBits(getY()) * 37;
	    bits += java.lang.Double.doubleToLongBits(getWidth()) * 43;
	    bits += java.lang.Double.doubleToLongBits(getHeight()) * 47;
	    return (int) (bits ^ (bits >> 32));
	}

	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof Rectangle2D) {
			Rectangle2D r2d = (Rectangle2D) obj;
			return ((getX() == r2d.getX()) &&
				(getY() == r2d.getY()) &&
				(getWidth() == r2d.getWidth()) &&
				(getHeight() == r2d.getHeight()));
		}
		return false;
	}

	public static void intersect(TRectangle2D src1, TRectangle2D src2, TRectangle2D dest) {
	    double x1 = Math.max(src1.getX(), src2.getX());
	    double y1 = Math.max(src1.getY(), src2.getY());
	    double x2 = Math.min(src1.getX() + src1.getWidth(), src2.getX() + src2.getWidth());
	    double y2 = Math.min(src1.getY() + src1.getHeight(), src2.getY() + src2.getHeight());
	    dest.setRect(x1, y1, Math.max(0, x2 - x1), Math.max(0, y2 - y1));
	}

	public static void union(TRectangle2D src1, TRectangle2D src2, TRectangle2D dest) {
	    double x1 = Math.min(src1.getX(), src2.getX());
	    double y1 = Math.min(src1.getY(), src2.getY());
	    double x2 = Math.max(src1.getX() + src1.getWidth(), src2.getX() + src2.getWidth());
	    double y2 = Math.max(src1.getY() + src1.getHeight(), src2.getY() + src2.getHeight());
	    dest.setRect(x1, y1, x2 - x1, y2 - y1);
	}

	@EqualsAndHashCode(callSuper = false)
	@ToString
	public static class Double extends TRectangle2D {
	    public double x;
	    public double y;
	    public double width;
	    public double height;

	    public Double() {
	        this(0, 0, 0, 0);
	    }

	    public Double(double x, double y, double w, double h) {
	        this.x = x;
	        this.y = y;
	        this.width = w;
	        this.height = h;
	    }

	    @Override
	    public double getX() {
	        return x;
	    }

	    @Override
	    public double getY() {
	        return y;
	    }

	    @Override
	    public double getWidth() {
	        return width;
	    }

	    @Override
	    public double getHeight() {
	        return height;
	    }

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public void setRect(double x, double y, double w, double h) {
			this.x = x;
			this.y = y;
			this.width = w;
			this.height = h;
		}

		@Override
		public int outcode(double x, double y) {
			int code = 0;
			if (getWidth() <= 0) {
				code |= OUT_LEFT | OUT_RIGHT;
			} else if (x < getX()) {
				code |= OUT_LEFT;
			} else if (x > getX() + getWidth()) {
				code |= OUT_RIGHT;
			}
			if (getHeight() <= 0) {
				code |= OUT_TOP | OUT_BOTTOM;
			} else if (y < getY()) {
				code |= OUT_TOP;
			} else if (y > getY() + getHeight()) {
				code |= OUT_BOTTOM;
			}
			return code;
		}

		@Override
		public TRectangle2D createIntersection(TRectangle2D r) {
			TRectangle2D dest;
			if (r instanceof TRectangle2D.Float) {
				dest = new TRectangle2D.Float();
			} else {
				dest = new TRectangle2D.Double();
			}
			TRectangle2D.intersect(this, r, dest);
			return dest;
		}

		@Override
		public TRectangle2D createUnion(TRectangle2D r) {
			TRectangle2D dest;
			if (r instanceof TRectangle2D.Float) {
				dest = new TRectangle2D.Float();
			} else {
				dest = new TRectangle2D.Double();
			}
			TRectangle2D.union(this, r, dest);
			return dest;
		}
	}

	@EqualsAndHashCode(callSuper = false)
	@ToString
	public static class Float extends TRectangle2D {
		public float x;
		public float y;
		public float width;
		public float height;

		public Float() {
			this(0, 0, 0, 0);
		}

		public Float(float x, float y, float w, float h) {
			this.x = x;
			this.y = y;
			this.width = w;
			this.height = h;
		}

		@Override
		public double getX() {
			return x;
		}

		@Override
		public double getY() {
			return y;
		}

		@Override
		public double getWidth() {
			return width;
		}

		@Override
		public double getHeight() {
			return height;
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public void setRect(double x, double y, double w, double h) {
			this.x = (float) x;
			this.y = (float) y;
			this.width = (float) w;
			this.height = (float) h;
		}

		@Override
		public int outcode(double x, double y) {
			int code = 0;
			if (getWidth() <= 0) {
				code |= OUT_LEFT | OUT_RIGHT;
			} else if (x < getX()) {
				code |= OUT_LEFT;
			} else if (x > getX() + getWidth()) {
				code |= OUT_RIGHT;
			}
			if (getHeight() <= 0) {
				code |= OUT_TOP | OUT_BOTTOM;
			} else if (y < getY()) {
				code |= OUT_TOP;
			} else if (y > getY() + getHeight()) {
				code |= OUT_BOTTOM;
			}
			return code;
		}

		@Override
		public TRectangle2D createIntersection(TRectangle2D r) {
			TRectangle2D dest;
			if (r instanceof TRectangle2D.Float) {
				dest = new TRectangle2D.Float();
			} else {
				dest = new TRectangle2D.Double();
			}
			TRectangle2D.intersect(this, r, dest);
			return dest;
		}

		@Override
		public TRectangle2D createUnion(TRectangle2D r) {
			TRectangle2D dest;
			if (r instanceof TRectangle2D.Float) {
				dest = new TRectangle2D.Float();
			} else {
				dest = new TRectangle2D.Double();
			}
			TRectangle2D.union(this, r, dest);
			return dest;
		}
	}
}
