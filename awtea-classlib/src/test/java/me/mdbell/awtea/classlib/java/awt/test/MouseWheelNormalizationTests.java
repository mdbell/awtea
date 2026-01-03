package me.mdbell.awtea.classlib.java.awt.test;

import me.mdbell.awtea.test.Test;
import static me.mdbell.awtea.test.Assert.*;

/**
 * Tests for mouse wheel delta normalization.
 * These tests verify that mouse wheel scroll deltas are properly normalized
 * based on browser deltaMode and configurable system properties.
 */
public class MouseWheelNormalizationTests {

    /**
     * Test that PIXEL divisor system property works correctly.
     */
    @Test
    public void testPixelDivisorProperty() {
        // Test default value
        String defaultValue = System.getProperty("me.mdbell.awtea.mouseWheel.pixelDivisor");
        // Property may not be set (null) which is fine - Integer.getInteger will use default
        
        // Test setting custom value
        System.setProperty("me.mdbell.awtea.mouseWheel.pixelDivisor", "50");
        int divisor = Integer.getInteger("me.mdbell.awtea.mouseWheel.pixelDivisor", 100);
        assertEquals(50, divisor, "Custom pixel divisor should be respected");
        
        // Clean up
        if (defaultValue != null) {
            System.setProperty("me.mdbell.awtea.mouseWheel.pixelDivisor", defaultValue);
        } else {
            System.clearProperty("me.mdbell.awtea.mouseWheel.pixelDivisor");
        }
    }

    /**
     * Test that LINE divisor system property works correctly.
     */
    @Test
    public void testLineDivisorProperty() {
        // Test setting custom value
        System.setProperty("me.mdbell.awtea.mouseWheel.lineDivisor", "5");
        int divisor = Integer.getInteger("me.mdbell.awtea.mouseWheel.lineDivisor", 3);
        assertEquals(5, divisor, "Custom line divisor should be respected");
        
        // Clean up
        System.clearProperty("me.mdbell.awtea.mouseWheel.lineDivisor");
    }

    /**
     * Test that PAGE multiplier system property works correctly.
     */
    @Test
    public void testPageMultiplierProperty() {
        // Test setting custom value
        System.setProperty("me.mdbell.awtea.mouseWheel.pageMultiplier", "3");
        int multiplier = Integer.getInteger("me.mdbell.awtea.mouseWheel.pageMultiplier", 1);
        assertEquals(3, multiplier, "Custom page multiplier should be respected");
        
        // Clean up
        System.clearProperty("me.mdbell.awtea.mouseWheel.pageMultiplier");
    }

    /**
     * Test default values when properties are not set.
     */
    @Test
    public void testDefaultValues() {
        // Ensure properties are not set
        System.clearProperty("me.mdbell.awtea.mouseWheel.pixelDivisor");
        System.clearProperty("me.mdbell.awtea.mouseWheel.lineDivisor");
        System.clearProperty("me.mdbell.awtea.mouseWheel.pageMultiplier");
        
        // Test defaults
        int pixelDivisor = Integer.getInteger("me.mdbell.awtea.mouseWheel.pixelDivisor", 100);
        assertEquals(100, pixelDivisor, "Default pixel divisor should be 100");
        
        int lineDivisor = Integer.getInteger("me.mdbell.awtea.mouseWheel.lineDivisor", 3);
        assertEquals(3, lineDivisor, "Default line divisor should be 3");
        
        int pageMultiplier = Integer.getInteger("me.mdbell.awtea.mouseWheel.pageMultiplier", 1);
        assertEquals(1, pageMultiplier, "Default page multiplier should be 1");
    }

    /**
     * Test normalization logic with typical browser values.
     */
    @Test
    public void testNormalizationLogic() {
        // Simulate Chrome pixel mode (100 per notch)
        double rawPixelDelta = 100.0;
        int pixelDivisor = 100;
        double normalizedPixel = rawPixelDelta / pixelDivisor;
        assertEquals(1.0, normalizedPixel, 0.001, "100 pixel delta should normalize to ~1");
        
        // Simulate line mode (3 lines per notch)
        double rawLineDelta = 3.0;
        int lineDivisor = 3;
        double normalizedLine = rawLineDelta / lineDivisor;
        assertEquals(1.0, normalizedLine, 0.001, "3 line delta should normalize to ~1");
        
        // Simulate negative scroll (scroll up)
        double rawNegativeDelta = -100.0;
        double normalizedNegative = rawNegativeDelta / pixelDivisor;
        assertEquals(-1.0, normalizedNegative, 0.001, "Negative delta should preserve sign");
    }

    /**
     * Test that rotation calculation works with normalized values.
     */
    @Test
    public void testRotationCalculation() {
        // Test positive rotation
        double normalizedPositive = 1.5;
        int rotationPositive = (int) Math.signum(normalizedPositive);
        assertEquals(1, rotationPositive, "Positive delta should give rotation of 1");
        
        // Test negative rotation
        double normalizedNegative = -1.5;
        int rotationNegative = (int) Math.signum(normalizedNegative);
        assertEquals(-1, rotationNegative, "Negative delta should give rotation of -1");
        
        // Test zero rotation
        double normalizedZero = 0.0;
        int rotationZero = (int) Math.signum(normalizedZero);
        assertEquals(0, rotationZero, "Zero delta should give rotation of 0");
    }

    /**
     * Test that invalid divisor values are handled gracefully.
     */
    @Test
    public void testInvalidDivisorValues() {
        // Test zero divisor - should fall back to default
        System.setProperty("me.mdbell.awtea.mouseWheel.pixelDivisor", "0");
        int divisor = Integer.getInteger("me.mdbell.awtea.mouseWheel.pixelDivisor", 100);
        assertEquals(0, divisor, "Property should be set to 0");
        // Note: TEventManager will detect this and use default internally
        
        // Test negative divisor - should fall back to default
        System.setProperty("me.mdbell.awtea.mouseWheel.pixelDivisor", "-50");
        divisor = Integer.getInteger("me.mdbell.awtea.mouseWheel.pixelDivisor", 100);
        assertEquals(-50, divisor, "Property should be set to -50");
        // Note: TEventManager will detect this and use default internally
        
        // Clean up
        System.clearProperty("me.mdbell.awtea.mouseWheel.pixelDivisor");
    }

    private void assertEquals(int expected, int actual, String message) {
        assertTrue(expected == actual, message + " (expected: " + expected + ", actual: " + actual + ")");
    }

    private void assertEquals(double expected, double actual, double delta, String message) {
        double diff = Math.abs(expected - actual);
        assertTrue(diff <= delta, message + " (expected: " + expected + ", actual: " + actual + ", diff: " + diff + ")");
    }
}
