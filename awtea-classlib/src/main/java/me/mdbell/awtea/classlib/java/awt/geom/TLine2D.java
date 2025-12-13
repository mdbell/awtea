package me.mdbell.awtea.classlib.java.awt.geom;

import lombok.SneakyThrows;
import me.mdbell.awtea.classlib.java.awt.TRectangle;
import me.mdbell.awtea.classlib.java.awt.TShape;

public abstract class TLine2D implements TShape, Cloneable{

	protected TLine2D(){

	}

	public abstract double getX1();
	public abstract double getY1();

	public abstract TPoint2D getP1();

	public abstract double getX2();
	public abstract double getY2();

	public abstract TPoint2D getP2();

	public abstract void setLine(double x1, double y1, double x2, double y2);

	public void setLine(TPoint2D p1, TPoint2D p2){
		setLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
	}

	public void setLine(TLine2D l){
		setLine(l.getX1(), l.getY1(), l.getX2(), l.getY2());
	}

	public int relativeCCW(double px, double py){
		return relativeCCW(getX1(), getY1(), getX2(), getY2(), px, py);
	}

	public int relativeCCW(TPoint2D p){
		return relativeCCW(getX1(), getY1(), getX2(), getY2(), p.getX(), p.getY());
	}

	public boolean intersectsLine(double x1, double y1, double x2, double y2) {
		return linesIntersect(x1, y1, x2, y2,
			getX1(), getY1(), getX2(), getY2());
	}

	public boolean intersectsLine(TLine2D l) {
		return linesIntersect(l.getX1(), l.getY1(), l.getX2(), l.getY2(),
			getX1(), getY1(), getX2(), getY2());
	}

	public double ptSegDistSq(double px, double py) {
		return ptSegDistSq(getX1(), getY1(), getX2(), getY2(), px, py);
	}

	public double ptSegDistSq(TPoint2D pt) {
		return ptSegDistSq(getX1(), getY1(), getX2(), getY2(),
			pt.getX(), pt.getY());
	}

	public double ptSegDist(double px, double py) {
		return ptSegDist(getX1(), getY1(), getX2(), getY2(), px, py);
	}

	public double ptSegDist(TPoint2D pt) {
		return ptSegDist(getX1(), getY1(), getX2(), getY2(),
			pt.getX(), pt.getY());
	}

	public double ptLineDistSq(double px, double py) {
		return ptLineDistSq(getX1(), getY1(), getX2(), getY2(), px, py);
	}

	public double ptLineDistSq(TPoint2D pt) {
		return ptLineDistSq(getX1(), getY1(), getX2(), getY2(),
			pt.getX(), pt.getY());
	}

	public double ptLineDist(double px, double py) {
		return ptLineDist(getX1(), getY1(), getX2(), getY2(), px, py);
	}

	public double ptLineDist(TPoint2D pt) {
		return ptLineDist(getX1(), getY1(), getX2(), getY2(),
			pt.getX(), pt.getY());
	}

	@Override
	public boolean contains(double x, double y) {
		return false;
	}

	@Override
	public boolean contains(TPoint2D p) {
		return false;
	}

	public boolean intersects(double x, double y, double w, double h) {
		return intersects(new TRectangle2D.Double(x, y, w, h));
	}

	public boolean intersects(TRectangle2D r) {
		return r.intersectsLine(getX1(), getY1(), getX2(), getY2());
	}

	public boolean contains(double x, double y, double w, double h) {
		return false;
	}

	public boolean contains(TRectangle2D r) {
		return false;
	}

	public TRectangle getBounds() {
		return getBounds2D().getBounds();
	}

	public TPathIterator getPathIterator(TAffineTransform at) {
		return new TLineIterator(this, at);
	}

	public TPathIterator getPathIterator(TAffineTransform at, double flatness) {
		return getPathIterator(at);
	}

	@SneakyThrows
	public Object clone() {
		return super.clone();
	}

	public static class Float extends TLine2D {
			    public float x1;
	    public float y1;
	    public float x2;
	    public float y2;

	    public Float() {
	    }

	    public Float(float x1, float y1, float x2, float y2) {
	        this.x1 = x1;
	        this.y1 = y1;
	        this.x2 = x2;
	        this.y2 = y2;
	    }

	    @Override
	    public double getX1() {
	        return x1;
	    }

	    @Override
	    public double getY1() {
	        return y1;
	    }

	    @Override
	    public TPoint2D getP1() {
	        return new TPoint2D.Float(x1, y1);
	    }

	    @Override
	    public double getX2() {
	        return x2;
	    }

	    @Override
	    public double getY2() {
	        return y2;
	    }

	    @Override
	    public TPoint2D getP2() {
	        return new TPoint2D.Float(x2, y2);
	    }

	    @Override
	    public void setLine(double x1, double y1, double x2, double y2) {
	        this.x1 = (float)x1;
	        this.y1 = (float)y1;
	        this.x2 = (float)x2;
	        this.y2 = (float)y2;
	    }

		@Override
		public TRectangle2D getBounds2D() {
			return new TRectangle2D.Float(
				Math.min(x1, x2),
				Math.min(y1, y2),
				Math.abs(x2 - x1),
				Math.abs(y2 - y1)
			);
		}
	}

	public static class Double extends TLine2D {
	    public double x1;
	    public double y1;
	    public double x2;
	    public double y2;

	    public Double() {
	    }

	    public Double(double x1, double y1, double x2, double y2) {
	        this.x1 = x1;
	        this.y1 = y1;
	        this.x2 = x2;
	        this.y2 = y2;
	    }

	    @Override
	    public double getX1() {
	        return x1;
	    }

	    @Override
	    public double getY1() {
	        return y1;
	    }

	    @Override
	    public TPoint2D getP1() {
	        return new TPoint2D.Double(x1, y1);
	    }

	    @Override
	    public double getX2() {
	        return x2;
	    }

	    @Override
	    public double getY2() {
	        return y2;
	    }

	    @Override
	    public TPoint2D getP2() {
	        return new TPoint2D.Double(x2, y2);
	    }

	    @Override
	    public void setLine(double x1, double y1, double x2, double y2) {
	        this.x1 = x1;
	        this.y1 = y1;
	        this.x2 = x2;
	        this.y2 = y2;
	    }

		@Override
		public TRectangle2D getBounds2D() {
			return new TRectangle2D.Double(
				Math.min(x1, x2),
				Math.min(y1, y2),
				Math.abs(x2 - x1),
				Math.abs(y2 - y1)
			);
		}
	}

	public static double ptLineDist(double x1, double y1,
									double x2, double y2,
									double px, double py) {
		return Math.sqrt(ptLineDistSq(x1, y1, x2, y2, px, py));
	}

	public static double ptLineDistSq(double x1, double y1,
									  double x2, double y2,
									  double px, double py) {
		x2 -= x1;
		y2 -= y1;
		px -= x1;
		py -= y1;
		double dotprod = px * x2 + py * y2;
		double projlenSq = dotprod * dotprod / (x2 * x2 + y2 * y2);
		double lenSq = px * px + py * py - projlenSq;
		if (lenSq < 0) {
			lenSq = 0;
		}
		return lenSq;
	}

	public static double ptSegDist(double x1, double y1,
								   double x2, double y2,
								   double px, double py)
	{
		return Math.sqrt(ptSegDistSq(x1, y1, x2, y2, px, py));
	}

	public static double ptSegDistSq(double x1, double y1,
									 double x2, double y2,
									 double px, double py)
	{
		x2 -= x1;
		y2 -= y1;
		px -= x1;
		py -= y1;
		double dotprod = px * x2 + py * y2;
		double projlenSq;
		if (dotprod <= 0.0) {
			projlenSq = 0.0;
		} else {
			px = x2 - px;
			py = y2 - py;
			dotprod = px * x2 + py * y2;
			if (dotprod <= 0.0) {

				projlenSq = 0.0;
			} else {
				projlenSq = dotprod * dotprod / (x2 * x2 + y2 * y2);
			}
		}
		double lenSq = px * px + py * py - projlenSq;
		if (lenSq < 0) {
			lenSq = 0;
		}
		return lenSq;
	}

	public static boolean linesIntersect(double x1, double y1,
										 double x2, double y2,
										 double x3, double y3,
										 double x4, double y4)
	{
		return ((relativeCCW(x1, y1, x2, y2, x3, y3) *
			relativeCCW(x1, y1, x2, y2, x4, y4) <= 0)
			&& (relativeCCW(x3, y3, x4, y4, x1, y1) *
			relativeCCW(x3, y3, x4, y4, x2, y2) <= 0));
	}

	public static int relativeCCW(double x1, double y1,
								  double x2, double y2,
								  double px, double py)
	{
		x2 -= x1;
		y2 -= y1;
		px -= x1;
		py -= y1;
		double ccw = px * y2 - py * x2;
		if (ccw == 0.0) {
			ccw = px * x2 + py * y2;
			if (ccw > 0.0) {
				px -= x2;
				py -= y2;
				ccw = px * x2 + py * y2;
				if (ccw < 0.0) {
					ccw = 0.0;
				}
			}
		}
		return java.lang.Double.compare(ccw, 0.0);
	}
}
