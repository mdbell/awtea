package me.mdbell.awtea.classlib.java.awt.test;

import me.mdbell.awtea.classlib.java.awt.*;
import me.mdbell.awtea.test.Test;

import java.awt.Color;

import org.teavm.classlib.java.awt.TDimension;

import static me.mdbell.awtea.test.Assert.*;

/**
 * Tests for TWindow functionality, especially the pack() method.
 * These tests verify that pack() properly sizes windows based on their content.
 */
public class TWindowPackTests {

    /**
     * Test that pack() correctly sizes a window based on layout manager's preferred
     * size.
     */
    @Test
    public void testPackWithBorderLayout() {
        // Create a test frame
        TestFrame frame = new TestFrame();

        // Add a component with known preferred size
        TPanel panel = new TPanel();
        panel.setPreferredSize(new TDimension(300, 200));
        frame.add(panel);

        // Call pack
        frame.pack();

        // Verify the frame is sized to accommodate the panel
        // pack() should use the layout manager's preferred size
        assertTrue(frame.getWidth() >= 300, "Frame width should be at least 300");
        assertTrue(frame.getHeight() >= 200, "Frame height should be at least 200");
    }

    /**
     * Test that pack() handles empty containers.
     */
    @Test
    public void testPackEmptyWindow() {
        TestFrame frame = new TestFrame();

        // Pack without any children
        frame.pack();

        // Should not crash and should have some minimal size
        assertTrue(frame.getWidth() > 0, "Frame width should be positive");
        assertTrue(frame.getHeight() > 0, "Frame height should be positive");
    }

    /**
     * Test that pack() respects layout manager calculations.
     */
    @Test
    public void testPackWithMultipleComponents() {
        TestFrame frame = new TestFrame();
        frame.setLayout(new TFlowLayout());

        // Add multiple components
        TPanel panel1 = new TPanel();
        panel1.setPreferredSize(new TDimension(100, 50));
        frame.add(panel1);

        TPanel panel2 = new TPanel();
        panel2.setPreferredSize(new TDimension(100, 50));
        frame.add(panel2);

        // Call pack
        frame.pack();

        // The frame should be sized to fit both panels
        // FlowLayout arranges them horizontally with gaps
        assertTrue(frame.getWidth() >= 200, "Frame should be wide enough for both panels");
        assertTrue(frame.getHeight() >= 50, "Frame should be tall enough for panels");
    }

    /**
     * Test frame class that doesn't require actual rendering infrastructure.
     * This allows pack() logic to be tested in isolation.
     */
    private static class TestFrame extends TWindow {

        public TestFrame() {
            super();
            // Don't create peers or surfaces - we're only testing pack() logic
        }

        @Override
        public TGraphics getSurfaceGraphics() {
            // Return null for tests - pack() shouldn't need graphics
            return null;
        }

        @Override
        public void setVisible(boolean b) {
            // No-op for tests
        }

        @Override
        public void repaint() {
            // No-op for tests
        }
    }
}
