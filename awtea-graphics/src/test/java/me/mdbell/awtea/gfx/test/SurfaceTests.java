package me.mdbell.awtea.gfx.test;

import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.test.*;

import static me.mdbell.awtea.test.Assert.*;

/**
 * Tests for Surface functionality that can be compiled to JavaScript
 * via TeaVM and executed in Deno.
 */
public class SurfaceTests {

    /**
     * Test that pixel format constants are defined with expected values.
     * This validates the basic constants match between Java and C/TypeScript.
     */
    @Test
    public void testPixelFormatConstants() {
        assertEquals(0, Surface.FORMAT_INT_ARGB, "FORMAT_INT_ARGB should be 0");
        assertEquals(1, Surface.FORMAT_INT_RGB, "FORMAT_INT_RGB should be 1");
        assertEquals(2, Surface.FORMAT_INT_RGBA, "FORMAT_INT_RGBA should be 2");
        assertEquals(3, Surface.FORMAT_INT_ABGR, "FORMAT_INT_ABGR should be 3");
        assertEquals(4, Surface.FORMAT_INT_BGR, "FORMAT_INT_BGR should be 4");
    }

    /**
     * Test pixel format range validation.
     * Ensures that the format bounds are correctly defined.
     */
    @Test
    public void testPixelFormatRange() {
        assertEquals(Surface.FORMAT_INT_ARGB, Surface.MIN_FORMAT, "MIN_FORMAT should be FORMAT_INT_ARGB");
        assertEquals(Surface.FORMAT_INT_BGR, Surface.MAX_FORMAT, "MAX_FORMAT should be FORMAT_INT_BGR");
    }

    /**
     * Test the isValidPixelFormat method with valid and invalid formats.
     * This ensures the validation logic works correctly.
     */
    @Test
    public void testPixelFormatValidation() {
        // Valid formats
        assertTrue(Surface.isValidPixelFormat(Surface.FORMAT_INT_ARGB), "FORMAT_INT_ARGB should be valid");
        assertTrue(Surface.isValidPixelFormat(Surface.FORMAT_INT_RGB), "FORMAT_INT_RGB should be valid");
        assertTrue(Surface.isValidPixelFormat(Surface.FORMAT_INT_RGBA), "FORMAT_INT_RGBA should be valid");
        assertTrue(Surface.isValidPixelFormat(Surface.FORMAT_INT_ABGR), "FORMAT_INT_ABGR should be valid");
        assertTrue(Surface.isValidPixelFormat(Surface.FORMAT_INT_BGR), "FORMAT_INT_BGR should be valid");

        // Invalid formats
        assertFalse(Surface.isValidPixelFormat(-1), "Negative format should be invalid");
        assertFalse(Surface.isValidPixelFormat(5), "Format 5 should be invalid");
        assertFalse(Surface.isValidPixelFormat(100), "Large format value should be invalid");
    }

    /**
     * Test enum-like constant values match expected sequential order.
     * This test validates that the enum generation is correct and consistent.
     * 
     * Note: The format array is hardcoded here intentionally to ensure test
     * stability. If new formats are added to Surface, this test should be
     * updated to include them, which serves as a reminder to verify enum
     * synchronization across C, Java, and TypeScript.
     */
    @Test
    public void testEnumSequentialValues() {
        // Verify that the constants are sequential from MIN to MAX
        int expectedValue = Surface.MIN_FORMAT;
        int[] allFormats = {
                Surface.FORMAT_INT_ARGB,
                Surface.FORMAT_INT_RGB,
                Surface.FORMAT_INT_RGBA,
                Surface.FORMAT_INT_ABGR,
                Surface.FORMAT_INT_BGR
        };

        for (int i = 0; i < allFormats.length; i++) {
            assertEquals(expectedValue + i, allFormats[i], "Format at index " + i + " should have sequential value");
        }
    }

    /**
     * Test that the format range is continuous (no gaps).
     * This ensures all values between MIN and MAX are valid.
     */
    @Test
    public void testFormatRangeContinuous() {
        for (int format = Surface.MIN_FORMAT; format <= Surface.MAX_FORMAT; format++) {
            assertTrue(Surface.isValidPixelFormat(format), "Format " + format + " should be valid within range");
        }
    }
}
