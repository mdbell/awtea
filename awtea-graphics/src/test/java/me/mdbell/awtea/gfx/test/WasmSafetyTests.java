package me.mdbell.awtea.gfx.test;

import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.gfx.wasm.WasmDiagnostics;
import me.mdbell.awtea.gfx.wasm.WasmRasterizer;
import me.mdbell.awtea.gfx.wasm.WasmSurface;
import me.mdbell.awtea.gfx.wasm.WasmSurfaceBackend;
import me.mdbell.awtea.test.*;

import static me.mdbell.awtea.test.Assert.*;

/**
 * Tests for WASM surface/context safety features.
 */
public class WasmSafetyTests {

    private WasmSurfaceBackend backend;
    private WasmDiagnostics diagnostics;

    public WasmSafetyTests() {
        backend = new WasmSurfaceBackend();
        diagnostics = backend.getDiagnostics();
    }

    /**
     * Test that destroy() is idempotent on surfaces.
     */
    @Test
    public void testSurfaceDestroyIdempotent() {
        WasmSurface surface = backend.createSurface(100, 100, Surface.FORMAT_INT_ARGB);
        surface.setPoolable(false); // we should not be pooling a surface we explicity want to destroy

        // First destroy should succeed
        surface.destroy();

        // Second destroy should be safe (idempotent)
        surface.destroy();

        // Third destroy should also be safe
        surface.destroy();

        // Surface ID should be -1 after destroy
        assertEquals(surface.getId(), -1, "Surface ID should be -1 after destroy");
    }

    /**
     * Test that dispose() is idempotent on rasterizers.
     */
    @Test
    public void testRasterizerDisposeIdempotent() {
        WasmSurface surface = backend.createSurface(100, 100, Surface.FORMAT_INT_ARGB);
        WasmRasterizer rasterizer = (WasmRasterizer) surface.createRasterizer();

        // First dispose should succeed
        rasterizer.dispose();

        // Second dispose should be safe (idempotent)
        rasterizer.dispose();

        // Third dispose should also be safe
        rasterizer.dispose();

        // Clean up surface
        surface.setPoolable(false);
        surface.destroy();
    }

    /**
     * Test diagnostics API for counting active surfaces.
     */
    @Test
    public void testDiagnosticsActiveSurfaceCount() {
        int initialCount = diagnostics.getActiveSurfaceCount();

        WasmSurface surface1 = backend.createSurface(100, 100, Surface.FORMAT_INT_ARGB);
        assertEquals(diagnostics.getActiveSurfaceCount(), initialCount + 1,
                "Should have 1 more active surface");

        WasmSurface surface2 = backend.createSurface(200, 200, Surface.FORMAT_INT_RGB);
        assertEquals(diagnostics.getActiveSurfaceCount(), initialCount + 2,
                "Should have 2 more active surfaces");

        surface1.setPoolable(false);
        surface1.destroy();
        assertEquals(diagnostics.getActiveSurfaceCount(), initialCount + 1,
                "Should have 1 more active surface after first destroy");

        surface2.setPoolable(false);
        surface2.destroy();
        assertEquals(diagnostics.getActiveSurfaceCount(), initialCount,
                "Should be back to initial count");
    }

    /**
     * Test diagnostics API for counting active contexts.
     */
    @Test
    public void testDiagnosticsActiveContextCount() {
        int initialCount = diagnostics.getActiveContextCount();

        WasmSurface surface = backend.createSurface(100, 100, Surface.FORMAT_INT_ARGB);
        WasmRasterizer rast1 = (WasmRasterizer) surface.createRasterizer();
        assertEquals(diagnostics.getActiveContextCount(), initialCount + 1,
                "Should have 1 more active context");

        WasmRasterizer rast2 = (WasmRasterizer) surface.createRasterizer();
        assertEquals(diagnostics.getActiveContextCount(), initialCount + 2,
                "Should have 2 more active contexts");

        rast1.dispose();
        assertEquals(diagnostics.getActiveContextCount(), initialCount + 1,
                "Should have 1 more active context after first dispose");

        rast2.dispose();
        assertEquals(diagnostics.getActiveContextCount(), initialCount,
                "Should be back to initial count");

        surface.setPoolable(false);
        surface.destroy();
    }

    /**
     * Test diagnostics API for surface reference counting.
     */
    @Test
    public void testDiagnosticsSurfaceRefCount() {
        WasmSurface surface = backend.createSurface(100, 100, Surface.FORMAT_INT_ARGB);
        int surfaceId = surface.getId();

        // No contexts created yet, ref count should be 0
        assertEquals(diagnostics.getSurfaceRefCount(surfaceId), 0,
                "Initial ref count should be 0");

        // Create first rasterizer (context), ref count should be 1
        WasmRasterizer rast1 = (WasmRasterizer) surface.createRasterizer();
        assertEquals(diagnostics.getSurfaceRefCount(surfaceId), 1,
                "Ref count should be 1 after first rasterizer");

        // Create second rasterizer (context), ref count should be 2
        WasmRasterizer rast2 = (WasmRasterizer) surface.createRasterizer();
        assertEquals(diagnostics.getSurfaceRefCount(surfaceId), 2,
                "Ref count should be 2 after second rasterizer");

        // Dispose first rasterizer, ref count should be 1
        rast1.dispose();
        assertEquals(diagnostics.getSurfaceRefCount(surfaceId), 1,
                "Ref count should be 1 after first dispose");

        // Dispose second rasterizer, ref count should be 0
        rast2.dispose();
        assertEquals(diagnostics.getSurfaceRefCount(surfaceId), 0,
                "Ref count should be 0 after all disposed");

        surface.setPoolable(false);
        surface.destroy();
    }

    /**
     * Test diagnostics capacity warning thresholds.
     */
    @Test
    public void testDiagnosticsCapacityWarning() {
        // At low utilization, no warnings should be triggered
        assertFalse(diagnostics.isSurfaceCapacityWarning(0.9),
                "Should not trigger warning at low utilization");
        assertFalse(diagnostics.isContextCapacityWarning(0.9),
                "Should not trigger warning at low utilization");

        // Utilization values should be between 0 and 1
        double surfaceUtil = diagnostics.getSurfaceUtilization();
        assertTrue(surfaceUtil >= 0.0 && surfaceUtil <= 1.0,
                "Surface utilization should be in [0,1]");

        double contextUtil = diagnostics.getContextUtilization();
        assertTrue(contextUtil >= 0.0 && contextUtil <= 1.0,
                "Context utilization should be in [0,1]");
    }

    /**
     * Test diagnostics report generation.
     */
    @Test
    public void testDiagnosticsReport() {
        String report = diagnostics.getReport();
        assertNotNull(report, "Report should not be null");
        assertTrue(report.contains("Surfaces:"), "Report should contain surface info");
        assertTrue(report.contains("Contexts:"), "Report should contain context info");
    }

    /**
     * Test max capacity values.
     */
    @Test
    public void testDiagnosticsMaxCapacities() {
        int maxSurfaces = diagnostics.getMaxSurfaces();
        assertTrue(maxSurfaces > 0, "Max surfaces should be positive");
        assertEquals(maxSurfaces, 1024, "Max surfaces should be 1024");

        int maxContexts = diagnostics.getMaxContexts();
        assertTrue(maxContexts > 0, "Max contexts should be positive");
        assertEquals(maxContexts, 2048, "Max contexts should be 2048");
    }

    /**
     * Test that invalid surface ID returns -1 for ref count.
     */
    @Test
    public void testDiagnosticsInvalidSurfaceId() {
        int refCount = diagnostics.getSurfaceRefCount(9999);
        assertEquals(refCount, -1, "Invalid surface ID should return -1");
    }

    /**
     * Test creating rasterizer after surface destroy throws exception.
     */
    @Test
    public void testCreateRasterizerAfterDestroy() {
        WasmSurface surface = backend.createSurface(100, 100, Surface.FORMAT_INT_ARGB);
        surface.setPoolable(false);
        surface.destroy();

        boolean exceptionThrown = false;
        try {
            surface.createRasterizer();
        } catch (IllegalStateException e) {
            exceptionThrown = true;
        }

        assertTrue(exceptionThrown, "Should throw exception when creating rasterizer on destroyed surface");
    }

    /**
     * Test resize on destroyed surface throws exception.
     */
    @Test
    public void testResizeAfterDestroy() {
        WasmSurface surface = backend.createSurface(100, 100, Surface.FORMAT_INT_ARGB);
        surface.setPoolable(false);
        surface.destroy();

        boolean exceptionThrown = false;
        try {
            surface.resize(200, 200);
        } catch (IllegalStateException e) {
            exceptionThrown = true;
        }

        assertTrue(exceptionThrown, "Should throw exception when resizing destroyed surface");
    }

    /**
     * Test getPixelData on destroyed surface throws exception.
     */
    @Test
    public void testGetPixelDataAfterDestroy() {
        WasmSurface surface = backend.createSurface(100, 100, Surface.FORMAT_INT_ARGB);
        surface.setPoolable(false);
        surface.destroy();

        boolean exceptionThrown = false;
        try {
            surface.getPixelData();
        } catch (IllegalStateException e) {
            exceptionThrown = true;
        }

        assertTrue(exceptionThrown, "Should throw exception when getting pixel data on destroyed surface");
    }

    /**
     * Test that getWidth/getHeight return 0 after destroy.
     */
    @Test
    public void testDimensionsAfterDestroy() {
        WasmSurface surface = backend.createSurface(100, 100, Surface.FORMAT_INT_ARGB);
        surface.setPoolable(false);
        surface.destroy();

        assertEquals(surface.getWidth(), 0, "Width should be 0 after destroy");
        assertEquals(surface.getHeight(), 0, "Height should be 0 after destroy");
    }
}
