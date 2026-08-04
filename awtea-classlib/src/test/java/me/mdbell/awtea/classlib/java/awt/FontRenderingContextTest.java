package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.font.TFontRenderContext;
import me.mdbell.awtea.classlib.java.awt.font.TLineMetrics;
import me.mdbell.awtea.classlib.java.awt.geom.TAffineTransform;
import me.mdbell.awtea.test.Test;

import static me.mdbell.awtea.test.Assert.*;

/**
 * Tests for Font Rendering Context-aware metrics (Phase 1).
 */
public class FontRenderingContextTest {

    @Test
    public void testFontRenderContextCreation() {
        // Test default context
        TFontRenderContext frc1 = new TFontRenderContext(null, false, false);
        assertNotNull(frc1, "FontRenderContext should not be null");
        assertFalse(frc1.isAntiAliased(), "Default context should not be anti-aliased");
        assertFalse(frc1.usesFractionalMetrics(), "Default context should not use fractional metrics");

        // Test with anti-aliasing
        TFontRenderContext frc2 = new TFontRenderContext(null, true, false);
        assertTrue(frc2.isAntiAliased(), "Context should be anti-aliased");
        assertFalse(frc2.usesFractionalMetrics(), "Context should not use fractional metrics");

        // Test with fractional metrics
        TFontRenderContext frc3 = new TFontRenderContext(null, false, true);
        assertFalse(frc3.isAntiAliased(), "Context should not be anti-aliased");
        assertTrue(frc3.usesFractionalMetrics(), "Context should use fractional metrics");
    }

    @Test
    public void testFontRenderContextWithTransform() {
        TAffineTransform transform = new TAffineTransform();
        transform.scale(2.0, 2.0);
        
        TFontRenderContext frc = new TFontRenderContext(transform, true, true);
        
        assertNotNull(frc.getTransform(), "Transform should not be null");
        assertTrue(frc.isAntiAliased(), "Context should be anti-aliased");
        assertTrue(frc.usesFractionalMetrics(), "Context should use fractional metrics");
    }

    @Test
    public void testFontRenderContextEquals() {
        TFontRenderContext frc1 = new TFontRenderContext(null, true, true);
        TFontRenderContext frc2 = new TFontRenderContext(null, true, true);
        TFontRenderContext frc3 = new TFontRenderContext(null, false, true);
        
        assertTrue(frc1.equals(frc2), "Equal contexts should be equal");
        assertFalse(frc1.equals(frc3), "Different contexts should not be equal");
    }

    @Test
    public void testFontMetricsContextAware() {
        TFont font = new TFont("SansSerif", TFont.PLAIN, 12);
        
        // Create metrics with default context (no AA, no fractional metrics)
        TFontRenderContext defaultFrc = new TFontRenderContext(null, false, false);
        TFontMetrics metrics1 = new TFontMetrics(font, defaultFrc);
        
        assertNotNull(metrics1, "FontMetrics should not be null");
        assertNotNull(metrics1.getFontRenderContext(), "FontMetrics should have FontRenderContext");
        assertFalse(metrics1.getFontRenderContext().isAntiAliased(), 
                "Default metrics should not be anti-aliased");
        assertFalse(metrics1.getFontRenderContext().usesFractionalMetrics(),
                "Default metrics should not use fractional metrics");
        
        // Create metrics with fractional metrics enabled
        TFontRenderContext fracFrc = new TFontRenderContext(null, false, true);
        TFontMetrics metrics2 = new TFontMetrics(font, fracFrc);
        
        assertTrue(metrics2.getFontRenderContext().usesFractionalMetrics(),
                "Metrics should use fractional metrics");
    }

    @Test
    public void testFontMetricsBasicMethods() {
        TFont font = new TFont("SansSerif", TFont.PLAIN, 12);
        TFontMetrics metrics = new TFontMetrics(font, null);
        
        assertTrue(metrics.getAscent() > 0, "Ascent should be positive");
        assertTrue(metrics.getDescent() > 0, "Descent should be positive");
        assertTrue(metrics.getLeading() >= 0, "Leading should be non-negative");
        assertTrue(metrics.getHeight() > 0, "Height should be positive");
        
        // Height should be ascent + descent + leading
        int expectedHeight = metrics.getAscent() + metrics.getDescent() + metrics.getLeading();
        assertTrue(metrics.getHeight() == expectedHeight, 
                "Height should equal ascent + descent + leading");
    }

    @Test
    public void testFontMetricsStringWidth() {
        TFont font = new TFont("SansSerif", TFont.PLAIN, 12);
        TFontMetrics metrics = new TFontMetrics(font, null);
        
        int width1 = metrics.stringWidth("Hello");
        assertTrue(width1 > 0, "String width should be positive");
        
        int width2 = metrics.stringWidth("Hello World");
        assertTrue(width2 > width1, "Longer string should have greater width");
        
        int emptyWidth = metrics.stringWidth("");
        assertTrue(emptyWidth == 0, "Empty string should have zero width");
    }

    @Test
    public void testLineMetrics() {
        TFont font = new TFont("SansSerif", TFont.PLAIN, 12);
        TFontRenderContext frc = new TFontRenderContext(null, false, false);
        
        TLineMetrics lineMetrics = font.getLineMetrics("Hello World", frc);
        
        assertNotNull(lineMetrics, "LineMetrics should not be null");
        assertTrue(lineMetrics.getNumChars() == 11, "LineMetrics should have correct char count");
        assertTrue(lineMetrics.getAscent() > 0, "Ascent should be positive");
        assertTrue(lineMetrics.getDescent() > 0, "Descent should be positive");
        assertTrue(lineMetrics.getLeading() >= 0, "Leading should be non-negative");
        assertTrue(lineMetrics.getHeight() > 0, "Height should be positive");
    }

    @Test
    public void testLineMetricsWithSubstring() {
        TFont font = new TFont("SansSerif", TFont.PLAIN, 12);
        TFontRenderContext frc = new TFontRenderContext(null, false, false);
        
        String text = "Hello World";
        TLineMetrics lineMetrics = font.getLineMetrics(text, 0, 5, frc); // "Hello"
        
        assertNotNull(lineMetrics, "LineMetrics should not be null");
        assertTrue(lineMetrics.getNumChars() == 5, "LineMetrics should have correct char count");
    }

    @Test
    public void testLineMetricsBaseline() {
        TFont font = new TFont("SansSerif", TFont.PLAIN, 12);
        TFontRenderContext frc = new TFontRenderContext(null, false, false);
        
        TLineMetrics lineMetrics = font.getLineMetrics("Test", frc);
        
        assertTrue(lineMetrics.getBaselineIndex() == TFont.ROMAN_BASELINE, 
                "Default baseline should be ROMAN_BASELINE");
        
        float[] baselineOffsets = lineMetrics.getBaselineOffsets();
        assertNotNull(baselineOffsets, "Baseline offsets should not be null");
        assertTrue(baselineOffsets.length == 3, "Should have 3 baseline offsets");
    }

    @Test
    public void testLineMetricsUnderlineAndStrikethrough() {
        TFont font = new TFont("SansSerif", TFont.PLAIN, 12);
        TFontRenderContext frc = new TFontRenderContext(null, false, false);
        
        TLineMetrics lineMetrics = font.getLineMetrics("Test", frc);
        
        // These should return reasonable values (implementation-specific)
        float underlineOffset = lineMetrics.getUnderlineOffset();
        float underlineThickness = lineMetrics.getUnderlineThickness();
        float strikethroughOffset = lineMetrics.getStrikethroughOffset();
        float strikethroughThickness = lineMetrics.getStrikethroughThickness();
        
        assertTrue(underlineThickness > 0, "Underline thickness should be positive");
        assertTrue(strikethroughThickness > 0, "Strikethrough thickness should be positive");
    }

    @Test
    public void testFontMetricsGetStringBounds() {
        TFont font = new TFont("SansSerif", TFont.PLAIN, 12);
        TFontMetrics metrics = new TFontMetrics(font, null);
        
        me.mdbell.awtea.classlib.java.awt.geom.TRectangle2D bounds = metrics.getStringBounds("Hello");
        
        assertNotNull(bounds, "String bounds should not be null");
        assertTrue(bounds.getWidth() > 0, "Bounds width should be positive");
        assertTrue(bounds.getHeight() > 0, "Bounds height should be positive");
    }

    @Test
    public void testFontMetricsMaxAscentDescent() {
        TFont font = new TFont("SansSerif", TFont.PLAIN, 12);
        TFontMetrics metrics = new TFontMetrics(font, null);
        
        assertTrue(metrics.getMaxAscent() > 0, "Max ascent should be positive");
        assertTrue(metrics.getMaxDescent() > 0, "Max descent should be positive");
        
        // For TrueType fonts, max should typically equal regular metrics
        assertTrue(metrics.getMaxAscent() == metrics.getAscent(), 
                "Max ascent should equal ascent for TrueType");
        assertTrue(metrics.getMaxDescent() == metrics.getDescent(), 
                "Max descent should equal descent for TrueType");
    }

    @Test
    public void testToolkitGetFontMetrics() {
        TFont font = new TFont("SansSerif", TFont.PLAIN, 12);
        TToolkit toolkit = TToolkit.getDefaultToolkit();
        
        TFontMetrics metrics = toolkit.getFontMetrics(font);
        
        assertNotNull(metrics, "Toolkit should create FontMetrics");
        assertNotNull(metrics.getFontRenderContext(), 
                "Toolkit metrics should have FontRenderContext");
        
        // Toolkit metrics should have default context (no AA, no fractional)
        assertFalse(metrics.getFontRenderContext().isAntiAliased(),
                "Toolkit metrics should not be anti-aliased by default");
        assertFalse(metrics.getFontRenderContext().usesFractionalMetrics(),
                "Toolkit metrics should not use fractional metrics by default");
    }

    @Test
    public void testFontGetFontMetrics() {
        TFont font = new TFont("SansSerif", TFont.PLAIN, 12);
        
        TFontMetrics metrics = font.getFontMetrics();
        
        assertNotNull(metrics, "Font.getFontMetrics() should create FontMetrics");
    }
}
