package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.geom.TAffineTransform;
import me.mdbell.awtea.classlib.java.awt.geom.TPathIterator;
import me.mdbell.awtea.classlib.java.awt.geom.TPoint2D;
import me.mdbell.awtea.classlib.java.awt.geom.TRectangle2D;

/**
 * TeaVM implementation of java.awt.Polygon.
 * The Polygon class encapsulates a description of a closed, two-dimensional region within a coordinate space.
 * 
 * @see java.awt.Polygon
 */
public class TPolygon implements TShape, java.io.Serializable {
    
    /**
     * The total number of points in this polygon.
     */
    public int npoints;
    
    /**
     * The array of X coordinates.
     */
    public int[] xpoints;
    
    /**
     * The array of Y coordinates.
     */
    public int[] ypoints;
    
    /**
     * Creates an empty polygon.
     */
    public TPolygon() {
        xpoints = new int[4];
        ypoints = new int[4];
    }
    
    /**
     * Constructs and initializes a Polygon from the specified parameters.
     * 
     * @param xpoints an array of X coordinates
     * @param ypoints an array of Y coordinates
     * @param npoints the total number of points in the Polygon
     */
    public TPolygon(int[] xpoints, int[] ypoints, int npoints) {
        if (npoints > xpoints.length || npoints > ypoints.length) {
            throw new IndexOutOfBoundsException("npoints > xpoints.length || npoints > ypoints.length");
        }
        if (npoints < 0) {
            throw new NegativeArraySizeException("npoints < 0");
        }
        this.npoints = npoints;
        this.xpoints = new int[npoints];
        this.ypoints = new int[npoints];
        System.arraycopy(xpoints, 0, this.xpoints, 0, npoints);
        System.arraycopy(ypoints, 0, this.ypoints, 0, npoints);
    }
    
    /**
     * Resets this Polygon object to an empty polygon.
     */
    public void reset() {
        npoints = 0;
    }
    
    /**
     * Appends the specified coordinates to this Polygon.
     * 
     * @param x the X coordinate
     * @param y the Y coordinate
     */
    public void addPoint(int x, int y) {
        if (npoints >= xpoints.length) {
            int newLength = npoints * 2;
            int[] newXPoints = new int[newLength];
            int[] newYPoints = new int[newLength];
            System.arraycopy(xpoints, 0, newXPoints, 0, npoints);
            System.arraycopy(ypoints, 0, newYPoints, 0, npoints);
            xpoints = newXPoints;
            ypoints = newYPoints;
        }
        xpoints[npoints] = x;
        ypoints[npoints] = y;
        npoints++;
    }
    
    @Override
    public TRectangle getBounds() {
        // TODO: Implement bounds calculation
        // @see https://docs.oracle.com/javase/8/docs/api/java/awt/Polygon.html#getBounds--
        throw new UnsupportedOperationException("TPolygon.getBounds() not yet implemented");
    }
    
    @Override
    public TRectangle2D getBounds2D() {
        TRectangle bounds = getBounds();
        if (bounds == null) {
            return null;
        }
        return new TRectangle2D.Double(bounds.x, bounds.y, bounds.width, bounds.height);
    }
    
    @Override
    public boolean contains(double x, double y) {
        // TODO: Implement point-in-polygon test
        // @see https://docs.oracle.com/javase/8/docs/api/java/awt/Polygon.html#contains-double-double-
        throw new UnsupportedOperationException("TPolygon.contains() not yet implemented");
    }
    
    @Override
    public boolean contains(TPoint2D p) {
        return contains(p.getX(), p.getY());
    }
    
    @Override
    public boolean intersects(double x, double y, double w, double h) {
        // TODO: Implement intersection test
        // @see https://docs.oracle.com/javase/8/docs/api/java/awt/Polygon.html#intersects-double-double-double-double-
        throw new UnsupportedOperationException("TPolygon.intersects() not yet implemented");
    }
    
    @Override
    public boolean intersects(TRectangle2D r) {
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }
    
    @Override
    public boolean contains(double x, double y, double w, double h) {
        // TODO: Implement contains rectangle test
        // @see https://docs.oracle.com/javase/8/docs/api/java/awt/Polygon.html#contains-double-double-double-double-
        throw new UnsupportedOperationException("TPolygon.contains(rectangle) not yet implemented");
    }
    
    @Override
    public boolean contains(TRectangle2D r) {
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }
    
    @Override
    public TPathIterator getPathIterator(TAffineTransform at) {
        // TODO: Implement path iterator
        // @see https://docs.oracle.com/javase/8/docs/api/java/awt/Polygon.html#getPathIterator-java.awt.geom.AffineTransform-
        throw new UnsupportedOperationException("TPolygon.getPathIterator() not yet implemented");
    }
    
    @Override
    public TPathIterator getPathIterator(TAffineTransform at, double flatness) {
        return getPathIterator(at);
    }
}
