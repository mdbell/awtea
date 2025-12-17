package me.mdbell.awtea.classlib.java.awt;

/**
 * TeaVM implementation of java.awt.BasicStroke.
 * The BasicStroke class defines a basic set of rendering attributes for the outlines of graphics primitives.
 * 
 * @see java.awt.BasicStroke
 */
public class TBasicStroke implements TStroke {
    
    /**
     * Joins path segments by extending their outside edges until they meet.
     */
    public static final int JOIN_MITER = 0;
    
    /**
     * Joins path segments by rounding off the corner at a radius of half the line width.
     */
    public static final int JOIN_ROUND = 1;
    
    /**
     * Joins path segments by connecting the outer corners of their wide outlines with a straight segment.
     */
    public static final int JOIN_BEVEL = 2;
    
    /**
     * Ends unclosed subpaths and dash segments with no added decoration.
     */
    public static final int CAP_BUTT = 0;
    
    /**
     * Ends unclosed subpaths and dash segments with a round decoration.
     */
    public static final int CAP_ROUND = 1;
    
    /**
     * Ends unclosed subpaths and dash segments with a square projection.
     */
    public static final int CAP_SQUARE = 2;
    
    private final float width;
    private final int cap;
    private final int join;
    private final float miterLimit;
    private final float[] dash;
    private final float dashPhase;
    
    /**
     * Constructs a new BasicStroke with defaults for all attributes.
     */
    public TBasicStroke() {
        this(1.0f, CAP_SQUARE, JOIN_MITER, 10.0f, null, 0.0f);
    }
    
    /**
     * Constructs a solid BasicStroke with the specified line width.
     * 
     * @param width the width of the BasicStroke
     */
    public TBasicStroke(float width) {
        this(width, CAP_SQUARE, JOIN_MITER, 10.0f, null, 0.0f);
    }
    
    /**
     * Constructs a solid BasicStroke with the specified attributes.
     * 
     * @param width the width of the BasicStroke
     * @param cap the decoration of the ends of a BasicStroke
     * @param join the decoration applied where path segments meet
     */
    public TBasicStroke(float width, int cap, int join) {
        this(width, cap, join, 10.0f, null, 0.0f);
    }
    
    /**
     * Constructs a solid BasicStroke with the specified attributes.
     * 
     * @param width the width of the BasicStroke
     * @param cap the decoration of the ends of a BasicStroke
     * @param join the decoration applied where path segments meet
     * @param miterLimit the limit to trim the miter join
     */
    public TBasicStroke(float width, int cap, int join, float miterLimit) {
        this(width, cap, join, miterLimit, null, 0.0f);
    }
    
    /**
     * Constructs a new BasicStroke with the specified attributes.
     * 
     * @param width the width of the BasicStroke
     * @param cap the decoration of the ends of a BasicStroke
     * @param join the decoration applied where path segments meet
     * @param miterLimit the limit to trim the miter join
     * @param dash the array representing the dashing pattern
     * @param dashPhase the offset to start the dashing pattern
     */
    public TBasicStroke(float width, int cap, int join, float miterLimit, float[] dash, float dashPhase) {
        if (width < 0.0f) {
            throw new IllegalArgumentException("negative width");
        }
        if (cap != CAP_BUTT && cap != CAP_ROUND && cap != CAP_SQUARE) {
            throw new IllegalArgumentException("illegal end cap value");
        }
        if (join != JOIN_MITER && join != JOIN_ROUND && join != JOIN_BEVEL) {
            throw new IllegalArgumentException("illegal line join value");
        }
        if (miterLimit < 1.0f) {
            throw new IllegalArgumentException("miter limit < 1");
        }
        if (dash != null) {
            if (dashPhase < 0.0f) {
                throw new IllegalArgumentException("negative dash phase");
            }
            boolean allZero = true;
            for (float d : dash) {
                if (d > 0.0) {
                    allZero = false;
                } else if (d < 0.0) {
                    throw new IllegalArgumentException("negative dash length");
                }
            }
            if (allZero) {
                throw new IllegalArgumentException("dash lengths all zero");
            }
        }
        this.width = width;
        this.cap = cap;
        this.join = join;
        this.miterLimit = miterLimit;
        this.dash = dash != null ? dash.clone() : null;
        this.dashPhase = dashPhase;
    }
    
    @Override
    public TShape createStrokedShape(TShape s) {
        // TODO: Implement stroke shape creation
        // @see https://docs.oracle.com/javase/8/docs/api/java/awt/BasicStroke.html#createStrokedShape-java.awt.Shape-
        throw new UnsupportedOperationException("TBasicStroke.createStrokedShape() not yet implemented");
    }
    
    /**
     * Returns the line width.
     * 
     * @return the line width of this BasicStroke
     */
    public float getLineWidth() {
        return width;
    }
    
    /**
     * Returns the end cap style.
     * 
     * @return the end cap style of this BasicStroke
     */
    public int getEndCap() {
        return cap;
    }
    
    /**
     * Returns the line join style.
     * 
     * @return the line join style of this BasicStroke
     */
    public int getLineJoin() {
        return join;
    }
    
    /**
     * Returns the miter limit.
     * 
     * @return the miter limit of this BasicStroke
     */
    public float getMiterLimit() {
        return miterLimit;
    }
    
    /**
     * Returns the array representing the lengths of the dash segments.
     * 
     * @return the dash array
     */
    public float[] getDashArray() {
        return dash != null ? dash.clone() : null;
    }
    
    /**
     * Returns the current dash phase.
     * 
     * @return the dash phase as a float value
     */
    public float getDashPhase() {
        return dashPhase;
    }
}
