package me.mdbell.awtea.classlib.java.awt;

import lombok.Getter;
import me.mdbell.awtea.classlib.java.awt.font.TFontRenderContext;
import me.mdbell.awtea.classlib.java.awt.font.TLineMetrics;
import me.mdbell.awtea.classlib.java.awt.font.TSimpleLineMetrics;
import me.mdbell.awtea.classlib.java.awt.geom.TRectangle2D;
import me.mdbell.awtea.font.TrueTypeFont;

import java.text.CharacterIterator;

/**
 * The FontMetrics class provides information about the rendering of a particular font
 * on a particular screen. This class is context-aware and reflects the rendering hints
 * (anti-aliasing, fractional metrics) from the Graphics or FontRenderContext it was created with.
 * 
 * @see java.awt.FontMetrics
 */
@Getter
public class TFontMetrics {

    private final TFont font;
    private final TFontRenderContext fontRenderContext;
	private final int ascent;
	private final int descent;
	private final int leading;
	private final int lineHeight;
	
	// Cached float values for precise measurements
	private final float ascentFloat;
	private final float descentFloat;
	private final float leadingFloat;

	/**
	 * Creates a new FontMetrics object for the specified font.
	 * This constructor is package-private and should only be called by Graphics or Toolkit.
	 * 
	 * @param font the font
	 */
	TFontMetrics(TFont font){
		this(font, null);
	}
	
	/**
	 * Creates a new FontMetrics object for the specified font with rendering context.
	 * This is the preferred constructor that makes metrics context-aware.
	 * 
	 * @param font the font
	 * @param frc the font render context (null creates a default context)
	 */
	TFontMetrics(TFont font, TFontRenderContext frc){
		this.font = font;
		this.fontRenderContext = (frc != null) ? frc : new TFontRenderContext(null, false, false);
		
		TrueTypeFont ttf = font.getFontPeer().getFont();

		float size = font.getSize();
		float ascentPx = ttf.getAscentPx(size);
		float descentPx = ttf.getDescentPx(size);
		float leadingPx = ttf.getLineGapPx(size);
		
		// Store float values for precise calculations
		this.ascentFloat = ascentPx;
		this.descentFloat = descentPx;
		this.leadingFloat = leadingPx;

		// Apply rounding based on fractional metrics setting
		if (this.fontRenderContext.usesFractionalMetrics()) {
			// With fractional metrics, round to nearest pixel
			this.ascent = Math.round(ascentPx);
			this.descent = Math.round(descentPx);
			this.leading = Math.round(leadingPx);
		} else {
			// Without fractional metrics, truncate (traditional behavior)
			this.ascent = (int) ascentPx;
			this.descent = (int) descentPx;
			this.leading = (int) leadingPx;
		}
		
		this.lineHeight = this.ascent + this.descent + this.leading;
	}

    public int stringWidth(String str) {
        if (str == null || str.isEmpty()) {
            return 0;
        }
        return font.getFontPeer().measureString(str, font.getSize());
    }

    /**
     * Gets the standard height of a line of text in this font.
     * This is the distance between the baseline of adjacent lines of text.
     *
     * @return the standard height of a line of text in this font
     */
    public int getHeight() {
        return ascent + descent + leading;
    }
    
    /**
     * Determines the maximum ascent of the font described by this FontMetrics object.
     * 
     * @return the maximum ascent of any character in the font
     * @see java.awt.FontMetrics#getMaxAscent()
     */
    public int getMaxAscent() {
        // For most TrueType fonts, max ascent equals the regular ascent
        // This could be enhanced to check actual glyph bounds if needed
        return ascent;
    }
    
    /**
     * Determines the maximum descent of the font described by this FontMetrics object.
     * 
     * @return the maximum descent of any character in the font
     * @see java.awt.FontMetrics#getMaxDescent()
     */
    public int getMaxDescent() {
        // For most TrueType fonts, max descent equals the regular descent
        // This could be enhanced to check actual glyph bounds if needed
        return descent;
    }
    
    /**
     * Gets the maximum advance width of any character in this font.
     * 
     * @return the maximum advance width of any character in the font, or -1 if not available
     * @see java.awt.FontMetrics#getMaxAdvance()
     */
    public int getMaxAdvance() {
        TrueTypeFont ttf = font.getFontPeer().getFont();
        float maxAdvancePx = ttf.getMaxAdvancePx(font.getSize());
        
        if (fontRenderContext.usesFractionalMetrics()) {
            return Math.round(maxAdvancePx);
        } else {
            return (int) maxAdvancePx;
        }
    }
    
    /**
     * Returns the bounds of the specified string in this font.
     * The bounds is used to layout text and includes inter-character spacing.
     * 
     * @param str the specified string
     * @return a TRectangle2D that is the bounding box of the specified string
     * @see java.awt.FontMetrics#getStringBounds(String, java.awt.Graphics)
     */
    public TRectangle2D getStringBounds(String str) {
        if (str == null || str.isEmpty()) {
            return new TRectangle2D.Float(0, 0, 0, 0);
        }
        int width = stringWidth(str);
        return new TRectangle2D.Float(0, -ascentFloat, width, ascentFloat + descentFloat);
    }
    
    /**
     * Returns the bounds of the specified string in this font with Graphics context.
     * 
     * @param str the specified string
     * @param g the Graphics context (used for additional rendering hints)
     * @return a TRectangle2D that is the bounding box of the specified string
     * @see java.awt.FontMetrics#getStringBounds(String, java.awt.Graphics)
     */
    public TRectangle2D getStringBounds(String str, TGraphics g) {
        // For now, delegate to the simpler version
        // Future enhancement: could use Graphics transform/hints
        return getStringBounds(str);
    }
    
    /**
     * Returns the bounds of the specified CharacterIterator in this font.
     * 
     * @param ci the specified CharacterIterator
     * @param beginIndex the initial offset in ci
     * @param limit the end offset in ci
     * @return a TRectangle2D that is the bounding box of the characters indexed in ci
     * @see java.awt.FontMetrics#getStringBounds(CharacterIterator, int, int, java.awt.Graphics)
     */
    public TRectangle2D getStringBounds(CharacterIterator ci, int beginIndex, int limit) {
        StringBuilder sb = new StringBuilder(limit - beginIndex);
        ci.setIndex(beginIndex);
        for (int i = beginIndex; i < limit; i++) {
            sb.append(ci.current());
            ci.next();
        }
        return getStringBounds(sb.toString());
    }
    
    /**
     * Returns the bounds of the characters indexed in the specified array.
     * 
     * @param chars an array of characters
     * @param beginIndex the initial offset in the array of characters
     * @param limit the end offset in the array of characters
     * @return a TRectangle2D that is the bounding box of the characters indexed in chars
     * @see java.awt.FontMetrics#getStringBounds(char[], int, int, java.awt.Graphics)
     */
    public TRectangle2D getStringBounds(char[] chars, int beginIndex, int limit) {
        String str = new String(chars, beginIndex, limit - beginIndex);
        return getStringBounds(str);
    }
    
    /**
     * Returns the LineMetrics object for the specified String in this font.
     * 
     * @param str the specified String
     * @return a LineMetrics object created with the specified String
     * @see java.awt.FontMetrics#getLineMetrics(String, java.awt.Graphics)
     */
    public TLineMetrics getLineMetrics(String str) {
        int numChars = (str != null) ? str.length() : 0;
        return new TSimpleLineMetrics(numChars, ascentFloat, descentFloat, leadingFloat);
    }
    
    /**
     * Returns the LineMetrics object for the specified String in this font with Graphics context.
     * 
     * @param str the specified String
     * @param g the Graphics context
     * @return a LineMetrics object created with the specified String
     * @see java.awt.FontMetrics#getLineMetrics(String, java.awt.Graphics)
     */
    public TLineMetrics getLineMetrics(String str, TGraphics g) {
        // For now, delegate to the simpler version
        return getLineMetrics(str);
    }
    
    /**
     * Returns the LineMetrics object for the specified CharacterIterator in this font.
     * 
     * @param ci the specified CharacterIterator
     * @param beginIndex the initial offset in ci
     * @param limit the end offset in ci
     * @return a LineMetrics object created with the specified CharacterIterator
     * @see java.awt.FontMetrics#getLineMetrics(CharacterIterator, int, int, java.awt.Graphics)
     */
    public TLineMetrics getLineMetrics(CharacterIterator ci, int beginIndex, int limit) {
        int numChars = limit - beginIndex;
        return new TSimpleLineMetrics(numChars, ascentFloat, descentFloat, leadingFloat);
    }
    
    /**
     * Returns the LineMetrics object for the specified character array in this font.
     * 
     * @param chars an array of characters
     * @param beginIndex the initial offset in the array of characters
     * @param limit the end offset in the array of characters
     * @return a LineMetrics object created with the specified character array
     * @see java.awt.FontMetrics#getLineMetrics(char[], int, int, java.awt.Graphics)
     */
    public TLineMetrics getLineMetrics(char[] chars, int beginIndex, int limit) {
        int numChars = limit - beginIndex;
        return new TSimpleLineMetrics(numChars, ascentFloat, descentFloat, leadingFloat);
    }
}
