package me.mdbell.awtea.classlib.java.awt.font;

import me.mdbell.awtea.classlib.java.awt.TFont;

/**
 * Simple implementation of TLineMetrics based on TrueType font tables.
 * This implementation provides basic line metrics using font ascent, descent,
 * and line height information directly from the font tables.
 */
public class TSimpleLineMetrics extends TLineMetrics {
    
    private final int numChars;
    private final float ascent;
    private final float descent;
    private final float leading;
    private final int baselineIndex;
    private final float[] baselineOffsets;
    
    /**
     * Constructs a SimpleLineMetrics from font data.
     * 
     * @param numChars the number of characters
     * @param ascent the ascent value
     * @param descent the descent value (positive value)
     * @param leading the leading value
     */
    public TSimpleLineMetrics(int numChars, float ascent, float descent, float leading) {
        this.numChars = numChars;
        this.ascent = ascent;
        this.descent = descent;
        this.leading = leading;
        this.baselineIndex = TFont.ROMAN_BASELINE;
        
        // Initialize baseline offsets (roman, center, hanging)
        this.baselineOffsets = new float[3];
        this.baselineOffsets[TFont.ROMAN_BASELINE] = 0.0f;
        this.baselineOffsets[TFont.CENTER_BASELINE] = (ascent - descent) / 2.0f;
        this.baselineOffsets[TFont.HANGING_BASELINE] = -ascent;
    }
    
    @Override
    public int getNumChars() {
        return numChars;
    }
    
    @Override
    public float getAscent() {
        return ascent;
    }
    
    @Override
    public float getDescent() {
        return descent;
    }
    
    @Override
    public float getLeading() {
        return leading;
    }
    
    @Override
    public float getHeight() {
        return ascent + descent + leading;
    }
    
    @Override
    public int getBaselineIndex() {
        return baselineIndex;
    }
    
    @Override
    public float[] getBaselineOffsets() {
        // Return a copy to prevent modification
        return baselineOffsets.clone();
    }
    
    @Override
    public float getStrikethroughOffset() {
        // Standard strikethrough is typically at about 1/3 of ascent height
        return -ascent / 3.0f;
    }
    
    @Override
    public float getStrikethroughThickness() {
        // Standard thickness is about 1/20 of font size (estimated from ascent)
        return Math.max(1.0f, ascent / 15.0f);
    }
    
    @Override
    public float getUnderlineOffset() {
        // Standard underline is just below the baseline, about 1/10 of descent
        return descent / 10.0f;
    }
    
    @Override
    public float getUnderlineThickness() {
        // Standard thickness is about 1/20 of font size (estimated from ascent)
        return Math.max(1.0f, ascent / 20.0f);
    }
}
