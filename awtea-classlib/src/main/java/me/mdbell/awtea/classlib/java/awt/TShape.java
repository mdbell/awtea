package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.geom.TAffineTransform;
import me.mdbell.awtea.classlib.java.awt.geom.TPathIterator;
import me.mdbell.awtea.classlib.java.awt.geom.TPoint2D;
import me.mdbell.awtea.classlib.java.awt.geom.TRectangle2D;

/**
 * TeaVM implementation of java.awt.Shape.
 * The Shape interface provides definitions for objects that represent some form of geometric shape.
 */
public interface TShape {

    /**
     * Returns an integer Rectangle that completely encloses the Shape.
     */
    TRectangle getBounds();

    /**
     * Returns a high precision bounding box of the Shape.
     */
    TRectangle2D getBounds2D();

    /**
     * Tests if the specified coordinates are inside the boundary of the Shape.
     */
    boolean contains(double x, double y);

    /**
     * Tests if a specified Point2D is inside the boundary of the Shape.
     */
    boolean contains(TPoint2D p);

    /**
     * Tests if the interior of the Shape intersects the interior of a specified rectangular area.
     */
    boolean intersects(double x, double y, double w, double h);

    /**
     * Tests if the interior of the Shape intersects the interior of a specified Rectangle2D.
     */
    boolean intersects(TRectangle2D r);

    /**
     * Tests if the interior of the Shape entirely contains the specified rectangular area.
     */
    boolean contains(double x, double y, double w, double h);

    /**
     * Tests if the interior of the Shape entirely contains the specified Rectangle2D.
     */
    boolean contains(TRectangle2D r);

    /**
     * Returns an iterator object that iterates along the Shape boundary and provides access
     * to the geometry of the Shape outline.
     */
    TPathIterator getPathIterator(TAffineTransform at);

    /**
     * Returns an iterator object that iterates along the Shape boundary and provides access
     * to a flattened view of the Shape outline geometry.
     */
    TPathIterator getPathIterator(TAffineTransform at, double flatness);
}
