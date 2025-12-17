package me.mdbell.awtea.classlib.java.awt;

/**
 * TeaVM implementation of java.awt.Stroke.
 * The Stroke interface allows a Graphics2D object to obtain a Shape that is the decorated outline,
 * or stylistic representation of the outline, of the specified Shape.
 * 
 * @see java.awt.Stroke
 */
public interface TStroke {
    
    /**
     * Returns an outline Shape which encloses the area that should be painted when the Shape is stroked
     * according to the rules defined by the object implementing the Stroke interface.
     * 
     * @param p the Shape boundary to be stroked
     * @return the stroked outline Shape
     * @see java.awt.Stroke#createStrokedShape(java.awt.Shape)
     */
    TShape createStrokedShape(TShape p);
}
