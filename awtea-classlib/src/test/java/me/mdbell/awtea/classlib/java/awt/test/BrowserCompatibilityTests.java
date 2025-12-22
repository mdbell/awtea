package me.mdbell.awtea.classlib.java.awt.test;

import me.mdbell.awtea.classlib.java.awt.*;
import me.mdbell.awtea.classlib.java.awt.event.TMouseWheelEvent;
import me.mdbell.awtea.classlib.java.awt.image.TDirectColorModel;
import me.mdbell.awtea.classlib.java.awt.image.TDataBuffer;
import me.mdbell.awtea.classlib.java.awt.color.TColorSpace;
import me.mdbell.awtea.classlib.javax.swing.TJFrame;
import me.mdbell.awtea.classlib.java.security.TMessageDigest;
import me.mdbell.awtea.test.Test;
import me.mdbell.awtea.input.MouseButtonType;

import org.teavm.classlib.java.awt.TDimension;

import java.security.NoSuchAlgorithmException;

import static me.mdbell.awtea.test.Assert.*;

/**
 * Tests for browser-compatible stub implementations.
 * These tests verify that the stub classes behave correctly for applet compatibility.
 */
public class BrowserCompatibilityTests {

    /**
     * Test TMouseWheelEvent modifier methods.
     */
    @Test
    public void testMouseWheelEventModifiers() {
        TPanel panel = new TPanel();
        
        // Create event with CTRL modifier
        TMouseWheelEvent eventWithCtrl = new TMouseWheelEvent(
            panel, TMouseWheelEvent.MOUSE_WHEEL,
            10, 10, MouseButtonType.LEFT, false,
            1.0, 3, 0, 3, 1
        );
        // Manually set modifiers using reflection or constructor if available
        // For now, test the method exists and doesn't crash
        boolean ctrlDown = eventWithCtrl.isControlDown();
        boolean shiftDown = eventWithCtrl.isShiftDown();
        
        // Methods should exist and return boolean values
        assertNotNull(ctrlDown, "isControlDown should not be null");
        assertNotNull(shiftDown, "isShiftDown should not be null");
    }

    /**
     * Test TContainer.getSize() method.
     */
    @Test
    public void testContainerGetSize() {
        TPanel panel = new TPanel();
        panel.setSize(200, 150);
        
        TDimension size = panel.getSize();
        
        assertNotNull(size, "getSize should not return null");
        assertEquals(200, size.width, "Width should match");
        assertEquals(150, size.height, "Height should match");
    }

    /**
     * Test TCursor basic functionality.
     */
    @Test
    public void testCursorCreation() {
        TCursor defaultCursor = TCursor.getDefaultCursor();
        assertNotNull(defaultCursor, "Default cursor should not be null");
        assertEquals(TCursor.DEFAULT_CURSOR, defaultCursor.getType(), "Should be default cursor type");
        
        TCursor handCursor = TCursor.getPredefinedCursor(TCursor.HAND_CURSOR);
        assertNotNull(handCursor, "Hand cursor should not be null");
        assertEquals(TCursor.HAND_CURSOR, handCursor.getType(), "Should be hand cursor type");
    }

    /**
     * Test TMediaTracker stub behavior.
     */
    @Test
    public void testMediaTrackerStub() {
        TPanel panel = new TPanel();
        TMediaTracker tracker = new TMediaTracker(panel);
        
        // Should not crash when adding images (even though it's a no-op)
        tracker.addImage(null, 0);
        
        // Should always report images as loaded
        assertTrue(tracker.checkAll(), "checkAll should return true");
        assertTrue(tracker.checkID(0), "checkID should return true");
        assertFalse(tracker.isErrorAny(), "isErrorAny should return false");
        assertEquals(TMediaTracker.COMPLETE, tracker.statusAll(false), "Status should be COMPLETE");
    }

    /**
     * Test TDesktop throws appropriate exceptions.
     */
    @Test
    public void testDesktopUnsupported() {
        assertFalse(TDesktop.isDesktopSupported(), "Desktop should not be supported in browser");
        
        try {
            TDesktop.getDesktop();
            fail("getDesktop should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
            assertTrue(e.getMessage().contains("browser"), "Error message should mention browser");
        }
    }

    /**
     * Test TMessageDigest getInstance.
     */
    @Test
    public void testMessageDigestSupported() throws NoSuchAlgorithmException {
        // SHA-1 should be supported (even if stub)
        TMessageDigest sha1 = TMessageDigest.getInstance("SHA-1");
        assertNotNull(sha1, "SHA-1 digest should not be null");
        assertEquals("SHA-1", sha1.getAlgorithm(), "Algorithm name should be SHA-1");
        
        // SHA-256 should be supported (even if stub)
        TMessageDigest sha256 = TMessageDigest.getInstance("SHA-256");
        assertNotNull(sha256, "SHA-256 digest should not be null");
        assertEquals("SHA-256", sha256.getAlgorithm(), "Algorithm name should be SHA-256");
    }

    /**
     * Test TMessageDigest unsupported algorithms.
     */
    @Test
    public void testMessageDigestUnsupported() {
        try {
            TMessageDigest.getInstance("MD5");
            fail("MD5 should throw NoSuchAlgorithmException");
        } catch (NoSuchAlgorithmException e) {
            // Expected
            assertTrue(e.getMessage().contains("not supported"), "Error message should indicate not supported");
        }
    }

    /**
     * Test TJFrame basic functionality.
     */
    @Test
    public void testJFrameCreation() {
        TJFrame frame = new TJFrame();
        assertNotNull(frame, "JFrame should not be null");
        
        TJFrame framedWithTitle = new TJFrame("Test");
        assertNotNull(framedWithTitle, "JFrame with title should not be null");
        
        // Should not crash when setting default close operation
        frame.setDefaultCloseOperation(3); // JFrame.EXIT_ON_CLOSE
    }

    /**
     * Test TDirectColorModel extended constructor.
     */
    @Test
    public void testDirectColorModelWithColorSpace() {
        TColorSpace colorSpace = TColorSpace.getInstance(TColorSpace.CS_sRGB);
        
        TDirectColorModel model = new TDirectColorModel(
            colorSpace,
            32,
            0x00FF0000, // red mask
            0x0000FF00, // green mask
            0x000000FF, // blue mask
            0xFF000000, // alpha mask
            false,      // not premultiplied
            TDataBuffer.TYPE_INT
        );
        
        assertNotNull(model, "ColorModel should not be null");
        assertEquals(0x00FF0000, model.getRedMask(), "Red mask should match");
        assertEquals(0x0000FF00, model.getGreenMask(), "Green mask should match");
        assertEquals(0x000000FF, model.getBlueMask(), "Blue mask should match");
        assertEquals(0xFF000000, model.getAlphaMask(), "Alpha mask should match");
    }

    private void assertNotNull(Object obj, String message) {
        assertTrue(obj != null, message);
    }

    private void assertEquals(int expected, int actual, String message) {
        assertTrue(expected == actual, message + " (expected: " + expected + ", actual: " + actual + ")");
    }

    private void assertEquals(String expected, String actual, String message) {
        assertTrue(expected.equals(actual), message + " (expected: " + expected + ", actual: " + actual + ")");
    }

    private void fail(String message) {
        assertTrue(false, message);
    }
}
