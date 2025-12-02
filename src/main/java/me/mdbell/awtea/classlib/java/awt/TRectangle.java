package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.geom.TAffineTransform;
import me.mdbell.awtea.classlib.java.awt.geom.TPathIterator;
import me.mdbell.awtea.classlib.java.awt.geom.TPoint2D;
import me.mdbell.awtea.classlib.java.awt.geom.TRectangle2D;


/**
 * @see java.awt.Rectangle
 */
public class TRectangle implements TShape {

    public int x, y, width, height;

	public TRectangle() {
		this(0, 0, 0, 0);
	}

    public TRectangle(int width, int height) {
        this(0, 0, width, height);
    }

    public TRectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public TRectangle getBounds() {
        return new TRectangle(x, y, width, height);
    }

    @Override
    public TRectangle2D getBounds2D() {
        return new TRectangle2D.Double(x, y, width, height);
    }

    @Override
    public boolean contains(double x, double y) {
        return x >= this.x && x < this.x + this.width &&
               y >= this.y && y < this.y + this.height;
    }

	public boolean contains(int x, int y) {
		return x >= this.x && x < this.x + this.width &&
			   y >= this.y && y < this.y + this.height;
	}

	public boolean contains(int x, int y, int w, int h) {
		return contains(x, y) &&
			   contains(x + w, y) &&
			   contains(x, y + h) &&
			   contains(x + w, y + h);
	}

    @Override
    public boolean contains(TPoint2D p) {
        return contains(p.getX(), p.getY());
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        if (this.width <= 0 || this.height <= 0) {
            return false;
        }
        return (x + w > this.x &&
                y + h > this.y &&
                x < this.x + this.width &&
                y < this.y + this.height);
    }

    @Override
    public boolean intersects(TRectangle2D r) {
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        return contains(x, y) &&
               contains(x + w, y) &&
               contains(x, y + h) &&
               contains(x + w, y + h);
    }

    @Override
    public boolean contains(TRectangle2D r) {
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    @Override
    public TPathIterator getPathIterator(TAffineTransform at) {
        // Simplified implementation - returns a rectangular path
        return new RectanglePathIterator(this, at);
    }

    @Override
    public TPathIterator getPathIterator(TAffineTransform at, double flatness) {
        return getPathIterator(at);
    }

    /**
     * Computes the intersection of this TRectangle with the specified rectangle.
     * Returns a new TRectangle that represents the intersection.
     * If the rectangles do not intersect, returns an empty rectangle.
     */
    public TRectangle intersection(int x, int y, int width, int height) {
        int tx1 = this.x;
        int ty1 = this.y;
        int tx2 = tx1 + this.width;
        int ty2 = ty1 + this.height;

        int rx1 = x;
        int ry1 = y;
        int rx2 = rx1 + width;
        int ry2 = ry1 + height;

        if (tx1 < rx1) tx1 = rx1;
        if (ty1 < ry1) ty1 = ry1;
        if (tx2 > rx2) tx2 = rx2;
        if (ty2 > ry2) ty2 = ry2;

        tx2 -= tx1;
        ty2 -= ty1;

        // If the intersection is negative, return empty rectangle
        if (tx2 < 0 || ty2 < 0) {
            return new TRectangle(0, 0, 0, 0);
        }

        return new TRectangle(tx1, ty1, tx2, ty2);
    }

	public void setRect(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public void setRect(TRectangle rect) {
		this.x = rect.x;
		this.y = rect.y;
		this.width = rect.width;
		this.height = rect.height;
	}

}
