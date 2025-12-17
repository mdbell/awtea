package me.mdbell.awtea.classlib.java.awt.font;

import me.mdbell.awtea.classlib.java.awt.geom.TAffineTransform;

/**
 * TeaVM implementation of java.awt.font.FontRenderContext.
 * The FontRenderContext class contains information about a graphics context that is used to
 * measure text correctly. FontRenderContext includes information about font rendering hints
 * and a transform that is applied to the text when it is rendered.
 * 
 * @see java.awt.font.FontRenderContext
 */
public class TFontRenderContext {
    
    private final TAffineTransform transform;
    private final boolean isAntiAliased;
    private final boolean usesFractionalMetrics;
    
    /**
     * Constructs a new FontRenderContext object.
     */
    protected TFontRenderContext() {
        this(null, false, false);
    }
    
    /**
     * Constructs a FontRenderContext object from an optional AffineTransform
     * and two boolean values that determine if the newly constructed object
     * has anti-aliasing or fractional metrics.
     * 
     * @param tx the AffineTransform to use (null means identity)
     * @param isAntiAliased determines if the newly constructed object has anti-aliasing
     * @param usesFractionalMetrics determines if the newly constructed object uses fractional metrics
     * @see java.awt.font.FontRenderContext#FontRenderContext(java.awt.geom.AffineTransform, boolean, boolean)
     */
    public TFontRenderContext(TAffineTransform tx, boolean isAntiAliased, boolean usesFractionalMetrics) {
        this.transform = tx != null ? new TAffineTransform(tx) : new TAffineTransform();
        this.isAntiAliased = isAntiAliased;
        this.usesFractionalMetrics = usesFractionalMetrics;
    }
    
    /**
     * Gets the transform that is used to scale typographical points to pixels in this FontRenderContext.
     * 
     * @return the AffineTransform of this FontRenderContext
     * @see java.awt.font.FontRenderContext#getTransform()
     */
    public TAffineTransform getTransform() {
        return new TAffineTransform(transform);
    }
    
    /**
     * Returns a boolean which indicates whether or not some form of antialiasing is specified.
     * 
     * @return true if anti-aliasing is used; false otherwise
     * @see java.awt.font.FontRenderContext#isAntiAliased()
     */
    public boolean isAntiAliased() {
        return isAntiAliased;
    }
    
    /**
     * Returns a boolean which indicates whether or not this FontRenderContext uses fractional metrics mode.
     * 
     * @return true if fractional metrics are used; false otherwise
     * @see java.awt.font.FontRenderContext#usesFractionalMetrics()
     */
    public boolean usesFractionalMetrics() {
        return usesFractionalMetrics;
    }
    
    /**
     * Return true if obj is an instance of FontRenderContext and has the same transform,
     * antialiasing, and fractional metrics values as this.
     * 
     * @param obj the object to test for equality
     * @return true if the specified object is equal to this FontRenderContext
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof TFontRenderContext)) {
            return false;
        }
        TFontRenderContext other = (TFontRenderContext) obj;
        return isAntiAliased == other.isAntiAliased &&
               usesFractionalMetrics == other.usesFractionalMetrics &&
               transform.equals(other.transform);
    }
    
    @Override
    public int hashCode() {
        int hash = transform.hashCode();
        if (isAntiAliased) {
            hash ^= 1;
        }
        if (usesFractionalMetrics) {
            hash ^= 2;
        }
        return hash;
    }
}
