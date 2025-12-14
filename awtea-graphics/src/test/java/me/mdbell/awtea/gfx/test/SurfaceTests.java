package me.mdbell.awtea.gfx.test;

import me.mdbell.awtea.gfx.Surface;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * JUnit tests for Surface functionality that can be compiled to JavaScript
 * via TeaVM and executed in Deno.
 */
public class SurfaceTests {

    /**
     * Test that pixel format constants are defined with expected values.
     * This validates the basic constants match between Java and C/TypeScript.
     */
    @Test
    public void testPixelFormatConstants() {
        assertEquals("FORMAT_INT_ARGB should be 0", 0, Surface.FORMAT_INT_ARGB);
        assertEquals("FORMAT_INT_RGB should be 1", 1, Surface.FORMAT_INT_RGB);
        assertEquals("FORMAT_INT_RGBA should be 2", 2, Surface.FORMAT_INT_RGBA);
        assertEquals("FORMAT_INT_ABGR should be 3", 3, Surface.FORMAT_INT_ABGR);
        assertEquals("FORMAT_INT_BGR should be 4", 4, Surface.FORMAT_INT_BGR);
    }

    /**
     * Test pixel format range validation.
     * Ensures that the format bounds are correctly defined.
     */
    @Test
    public void testPixelFormatRange() {
        assertEquals("MIN_FORMAT should be FORMAT_INT_ARGB", 
            Surface.FORMAT_INT_ARGB, Surface.MIN_FORMAT);
        assertEquals("MAX_FORMAT should be FORMAT_INT_BGR", 
            Surface.FORMAT_INT_BGR, Surface.MAX_FORMAT);
    }

    /**
     * Test the isValidPixelFormat method with valid and invalid formats.
     * This ensures the validation logic works correctly.
     */
    @Test
    public void testPixelFormatValidation() {
        // Valid formats
        assertTrue("FORMAT_INT_ARGB should be valid", 
            Surface.isValidPixelFormat(Surface.FORMAT_INT_ARGB));
        assertTrue("FORMAT_INT_RGB should be valid", 
            Surface.isValidPixelFormat(Surface.FORMAT_INT_RGB));
        assertTrue("FORMAT_INT_RGBA should be valid", 
            Surface.isValidPixelFormat(Surface.FORMAT_INT_RGBA));
        assertTrue("FORMAT_INT_ABGR should be valid", 
            Surface.isValidPixelFormat(Surface.FORMAT_INT_ABGR));
        assertTrue("FORMAT_INT_BGR should be valid", 
            Surface.isValidPixelFormat(Surface.FORMAT_INT_BGR));
        
        // Invalid formats
        assertFalse("Negative format should be invalid", 
            Surface.isValidPixelFormat(-1));
        assertFalse("Format 5 should be invalid", 
            Surface.isValidPixelFormat(5));
        assertFalse("Large format value should be invalid", 
            Surface.isValidPixelFormat(100));
    }

    /**
     * Test enum-like constant values match expected sequential order.
     * This test validates that the enum generation is correct and consistent.
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
            assertEquals("Format at index " + i + " should have sequential value", 
                expectedValue + i, allFormats[i]);
        }
    }

    /**
     * Test that the format range is continuous (no gaps).
     * This ensures all values between MIN and MAX are valid.
     */
    @Test
    public void testFormatRangeContinuous() {
        for (int format = Surface.MIN_FORMAT; format <= Surface.MAX_FORMAT; format++) {
            assertTrue("Format " + format + " should be valid within range", 
                Surface.isValidPixelFormat(format));
        }
    }
}
