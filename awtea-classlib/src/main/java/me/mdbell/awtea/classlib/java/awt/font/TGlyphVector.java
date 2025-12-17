package me.mdbell.awtea.classlib.java.awt.font;

import me.mdbell.awtea.classlib.java.awt.TFont;
import me.mdbell.awtea.classlib.java.awt.TRectangle;
import me.mdbell.awtea.classlib.java.awt.TShape;
import me.mdbell.awtea.classlib.java.awt.geom.TAffineTransform;
import me.mdbell.awtea.classlib.java.awt.geom.TPoint2D;
import me.mdbell.awtea.classlib.java.awt.geom.TRectangle2D;

/**
 * TeaVM implementation of java.awt.font.GlyphVector.
 * A GlyphVector object is a collection of glyphs containing geometric information for the placement
 * of each glyph in a transformed coordinate space which corresponds to the device on which the GlyphVector is ultimately displayed.
 * 
 * @see java.awt.font.GlyphVector
 */
public abstract class TGlyphVector {
    
    /**
     * Returns the Font associated with this GlyphVector.
     * 
     * @return the Font used to create this GlyphVector
     * @see java.awt.font.GlyphVector#getFont()
     */
    public abstract TFont getFont();
    
    /**
     * Returns the FontRenderContext associated with this GlyphVector.
     * 
     * @return the FontRenderContext used to create this GlyphVector
     * @see java.awt.font.GlyphVector#getFontRenderContext()
     */
    public abstract TFontRenderContext getFontRenderContext();
    
    /**
     * Returns the number of glyphs in this GlyphVector.
     * 
     * @return the number of glyphs in this GlyphVector
     * @see java.awt.font.GlyphVector#getNumGlyphs()
     */
    public abstract int getNumGlyphs();
    
    /**
     * Returns the glyphcode of the specified glyph.
     * 
     * @param glyphIndex the index into this GlyphVector
     * @return the glyphcode of the glyph at the specified glyphIndex
     * @see java.awt.font.GlyphVector#getGlyphCode(int)
     */
    public abstract int getGlyphCode(int glyphIndex);
    
    /**
     * Returns the position of the specified glyph relative to the origin of this GlyphVector.
     * 
     * @param glyphIndex the index into this GlyphVector
     * @return a Point2D object containing the position of the glyph
     * @see java.awt.font.GlyphVector#getGlyphPosition(int)
     */
    public abstract TPoint2D getGlyphPosition(int glyphIndex);
    
    /**
     * Sets the position of the specified glyph within this GlyphVector.
     * 
     * @param glyphIndex the index into this GlyphVector
     * @param newPos the new position of the glyph
     * @see java.awt.font.GlyphVector#setGlyphPosition(int, java.awt.geom.Point2D)
     */
    public abstract void setGlyphPosition(int glyphIndex, TPoint2D newPos);
    
    /**
     * Returns the transform of the specified glyph within this GlyphVector.
     * 
     * @param glyphIndex the index into this GlyphVector
     * @return an AffineTransform that is the transform of the glyph
     * @see java.awt.font.GlyphVector#getGlyphTransform(int)
     */
    public abstract TAffineTransform getGlyphTransform(int glyphIndex);
    
    /**
     * Sets the transform of the specified glyph within this GlyphVector.
     * 
     * @param glyphIndex the index into this GlyphVector
     * @param newTX the new transform of the glyph
     * @see java.awt.font.GlyphVector#setGlyphTransform(int, java.awt.geom.AffineTransform)
     */
    public abstract void setGlyphTransform(int glyphIndex, TAffineTransform newTX);
    
    /**
     * Returns the visual bounds of the specified glyph within the GlyphVector.
     * 
     * @param glyphIndex the index into this GlyphVector
     * @return a Shape that is the visual bounds of the glyph
     * @see java.awt.font.GlyphVector#getGlyphVisualBounds(int)
     */
    public abstract TShape getGlyphVisualBounds(int glyphIndex);
    
    /**
     * Returns the logical bounds of the specified glyph within this GlyphVector.
     * 
     * @param glyphIndex the index into this GlyphVector
     * @return a Rectangle2D that is the logical bounds of the glyph
     * @see java.awt.font.GlyphVector#getGlyphLogicalBounds(int)
     */
    public abstract TShape getGlyphLogicalBounds(int glyphIndex);
    
    /**
     * Returns the outline of the specified glyph within this GlyphVector.
     * 
     * @param glyphIndex the index into this GlyphVector
     * @return a Shape that is the outline of the glyph
     * @see java.awt.font.GlyphVector#getGlyphOutline(int)
     */
    public abstract TShape getGlyphOutline(int glyphIndex);
    
    /**
     * Returns the visual bounds of this GlyphVector.
     * 
     * @return a Rectangle2D that is the bounding box for the entire GlyphVector
     * @see java.awt.font.GlyphVector#getVisualBounds()
     */
    public abstract TRectangle2D getVisualBounds();
    
    /**
     * Returns the logical bounds of this GlyphVector.
     * 
     * @return a Rectangle2D that is the logical bounds of this GlyphVector
     * @see java.awt.font.GlyphVector#getLogicalBounds()
     */
    public abstract TRectangle2D getLogicalBounds();
    
    /**
     * Returns a Shape whose interior corresponds to the visual representation of this GlyphVector.
     * 
     * @return a Shape that is the outline of this GlyphVector
     * @see java.awt.font.GlyphVector#getOutline()
     */
    public abstract TShape getOutline();
    
    /**
     * Returns a Shape whose interior corresponds to the visual representation of this GlyphVector
     * when rendered at the specified location.
     * 
     * @param x the X coordinate at which to position the GlyphVector
     * @param y the Y coordinate at which to position the GlyphVector
     * @return a Shape that is the outline of this GlyphVector positioned at the specified coordinates
     * @see java.awt.font.GlyphVector#getOutline(float, float)
     */
    public abstract TShape getOutline(float x, float y);
    
    /**
     * Returns the pixel bounds of this GlyphVector when rendered at the specified location.
     * 
     * @param renderFRC the FontRenderContext of the Graphics
     * @param x the X coordinate at which to position the GlyphVector
     * @param y the Y coordinate at which to position the GlyphVector
     * @return a Rectangle bounding the pixels that would be affected
     * @see java.awt.font.GlyphVector#getPixelBounds(java.awt.font.FontRenderContext, float, float)
     */
    public TRectangle getPixelBounds(TFontRenderContext renderFRC, float x, float y) {
        // TODO: Implement pixel bounds calculation
        // @see https://docs.oracle.com/javase/8/docs/api/java/awt/font/GlyphVector.html#getPixelBounds-java.awt.font.FontRenderContext-float-float-
        throw new UnsupportedOperationException("TGlyphVector.getPixelBounds() not yet implemented");
    }
}
