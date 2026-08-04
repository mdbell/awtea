package me.mdbell.awtea.classlib.java.awt.font;

/**
 * TeaVM implementation of java.awt.font.LineMetrics.
 * The LineMetrics class encapsulates measurement information for a line of text.
 * This includes ascent, descent, leading, baseline, and other metrics that define
 * how a line of text should be positioned and rendered.
 * 
 * @see java.awt.font.LineMetrics
 */
public abstract class TLineMetrics {
    
    /**
     * Returns the number of characters in the measured line of text.
     * 
     * @return the number of characters in the measured line of text
     * @see java.awt.font.LineMetrics#getNumChars()
     */
    public abstract int getNumChars();
    
    /**
     * Returns the ascent of the text. The ascent is the distance from the
     * baseline to the top of most alphanumeric characters.
     * 
     * @return the ascent of the text
     * @see java.awt.font.LineMetrics#getAscent()
     */
    public abstract float getAscent();
    
    /**
     * Returns the descent of the text. The descent is the distance from the
     * baseline to the bottom of most alphanumeric characters with descenders.
     * 
     * @return the descent of the text
     * @see java.awt.font.LineMetrics#getDescent()
     */
    public abstract float getDescent();
    
    /**
     * Returns the leading of the text. The leading is the recommended distance
     * from the bottom of the descenders of one line to the top of the ascenders
     * of the next line.
     * 
     * @return the leading of the text
     * @see java.awt.font.LineMetrics#getLeading()
     */
    public abstract float getLeading();
    
    /**
     * Returns the height of the text. The height is the distance from the top
     * of the tallest character to the bottom of the lowest descender.
     * 
     * @return the height of the text
     * @see java.awt.font.LineMetrics#getHeight()
     */
    public abstract float getHeight();
    
    /**
     * Returns the baseline index of the text. The baseline is the line
     * relative to which glyphs are positioned.
     * 
     * @return the baseline index of the text (e.g., Font.ROMAN_BASELINE)
     * @see java.awt.font.LineMetrics#getBaselineIndex()
     */
    public abstract int getBaselineIndex();
    
    /**
     * Returns the baseline offsets of the text relative to the baseline
     * specified by getBaselineIndex().
     * 
     * @return an array of baseline offsets
     * @see java.awt.font.LineMetrics#getBaselineOffsets()
     */
    public abstract float[] getBaselineOffsets();
    
    /**
     * Returns the position of the strike-through line relative to the baseline.
     * 
     * @return the position of the strike-through line
     * @see java.awt.font.LineMetrics#getStrikethroughOffset()
     */
    public abstract float getStrikethroughOffset();
    
    /**
     * Returns the thickness of the strike-through line.
     * 
     * @return the thickness of the strike-through line
     * @see java.awt.font.LineMetrics#getStrikethroughThickness()
     */
    public abstract float getStrikethroughThickness();
    
    /**
     * Returns the position of the underline relative to the baseline.
     * 
     * @return the position of the underline
     * @see java.awt.font.LineMetrics#getUnderlineOffset()
     */
    public abstract float getUnderlineOffset();
    
    /**
     * Returns the thickness of the underline.
     * 
     * @return the thickness of the underline
     * @see java.awt.font.LineMetrics#getUnderlineThickness()
     */
    public abstract float getUnderlineThickness();
}
