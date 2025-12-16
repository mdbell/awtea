package me.mdbell.awtea.gfx.test;

import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.gfx.wasm.WasmSurface;
import me.mdbell.awtea.gfx.wasm.WasmSurfaceBackend;
import me.mdbell.awtea.gfx.wasm.WasmSurfacePool;
import me.mdbell.awtea.test.*;

import static me.mdbell.awtea.test.Assert.*;

/**
 * Tests for WasmSurfacePool functionality.
 * These tests validate the surface pooling mechanism for WASM surfaces.
 */
public class WasmSurfacePoolTests {

    private WasmSurfaceBackend backend;
    private WasmSurfacePool pool;

    public WasmSurfacePoolTests() {
        backend = new WasmSurfaceBackend();
        pool = backend.getSurfacePool();
    }

    /**
     * Test that acquiring a surface creates a new one when pool is empty.
     */
    @Test
    public void testAcquireCreatesNewSurface() {
        WasmSurface surface = pool.acquire(100, 100, Surface.FORMAT_INT_ARGB);

        assertNotNull(surface, "Acquired surface should not be null");
        assertEquals(surface.getWidth(), 100, "Surface width should match");
        assertEquals(surface.getHeight(), 100, "Surface height should match");
        assertEquals(surface.getFormat(), Surface.FORMAT_INT_ARGB, "Surface format should match");

        WasmSurfacePool.PoolStats stats = pool.getStats();
        assertEquals(stats.acquireRequests, 1L, "Should have 1 acquire request");
        assertEquals(0, stats.poolHits, "Should have 0 pool hits");
        assertEquals(1, stats.poolMisses, "Should have 1 pool miss");

        surface.setPoolable(false);
        surface.destroy();
    }

    /**
     * Test that releasing and reacquiring a surface reuses it from the pool.
     */
    @Test
    public void testReleaseAndReacquire() {
        WasmSurface surface1 = pool.acquire(100, 100, Surface.FORMAT_INT_ARGB);
        int surfaceId1 = surface1.getId();

        pool.release(surface1);

        WasmSurface surface2 = pool.acquire(100, 100, Surface.FORMAT_INT_ARGB);
        int surfaceId2 = surface2.getId();

        assertEquals(surfaceId1, surfaceId2, "Should reuse the same surface ID");

        WasmSurfacePool.PoolStats stats = pool.getStats();
        assertEquals(stats.acquireRequests, 2, "Should have 2 acquire requests");
        assertEquals(stats.poolHits, 1, "Should have 1 pool hit");
        assertEquals(stats.poolMisses, 1, "Should have 1 pool miss");

        surface2.setPoolable(false);
        surface2.destroy();
    }

    /**
     * Test that different dimensions create different pool keys.
     */
    @Test
    public void testDifferentDimensionsCreateDifferentKeys() {
        WasmSurface surface1 = pool.acquire(100, 100, Surface.FORMAT_INT_ARGB);
        int surfaceId1 = surface1.getId();
        pool.release(surface1);

        WasmSurface surface2 = pool.acquire(200, 200, Surface.FORMAT_INT_ARGB);
        int surfaceId2 = surface2.getId();

        // Different dimensions should result in a pool miss
        WasmSurfacePool.PoolStats stats = pool.getStats();
        assertEquals(2, stats.poolMisses, "Both should be pool misses (different keys)");

        // Surfaces may or may not have different IDs (depends on internal allocation)
        // so we don't assert on ID equality/inequality

        pool.release(surface2);
    }

    /**
     * Test that different formats create different pool keys.
     */
    @Test
    public void testDifferentFormatsCreateDifferentKeys() {
        WasmSurface surface1 = pool.acquire(100, 100, Surface.FORMAT_INT_ARGB);
        int surfaceId1 = surface1.getId();
        pool.release(surface1);

        WasmSurface surface2 = pool.acquire(100, 100, Surface.FORMAT_INT_RGB);
        int surfaceId2 = surface2.getId();

        // Different formats should result in a pool miss
        WasmSurfacePool.PoolStats stats = pool.getStats();
        assertEquals(2, stats.poolMisses, "Both should be pool misses (different keys)");

        // Surfaces may or may not have different IDs (depends on internal allocation)
        // so we don't assert on ID equality/inequality

        pool.release(surface2);
    }

    /**
     * Test that pool respects per-key limits.
     */
    @Test
    public void testPerKeyLimit() {
        // Get max per key (default is 4)
        int maxPerKey = 4;

        // Create and release more surfaces than the limit
        WasmSurface[] surfaces = new WasmSurface[maxPerKey + 2];
        for (int i = 0; i < surfaces.length; i++) {
            surfaces[i] = pool.acquire(100, 100, Surface.FORMAT_INT_ARGB);
        }

        // Release all
        for (WasmSurface surface : surfaces) {
            pool.release(surface);
        }

        WasmSurfacePool.PoolStats stats = pool.getStats();
        assertTrue(stats.currentPoolSize <= maxPerKey,
                "Pool size should not exceed max per key for single key");
        assertTrue(stats.destroyCount >= 2,
                "Should have destroyed at least 2 surfaces due to limit");
    }

    /**
     * Test that clearing the pool destroys all surfaces.
     */
    @Test
    public void testClear() {
        // Add some surfaces to the pool
        for (int i = 0; i < 5; i++) {
            WasmSurface surface = pool.acquire(100, 100, Surface.FORMAT_INT_ARGB);
            pool.release(surface);
        }

        WasmSurfacePool.PoolStats statsBefore = pool.getStats();
        assertTrue(statsBefore.currentPoolSize > 0, "Pool should have surfaces before clear");

        pool.clear();

        WasmSurfacePool.PoolStats statsAfter = pool.getStats();
        assertEquals(0, statsAfter.currentPoolSize, "Pool should be empty after clear");
    }

    /**
     * Test that trimming reduces pool size.
     */
    @Test
    public void testTrim() {
        // Add multiple surfaces for the same key
        WasmSurface surface1 = pool.acquire(100, 100, Surface.FORMAT_INT_ARGB);
        WasmSurface surface2 = pool.acquire(100, 100, Surface.FORMAT_INT_ARGB);

        pool.release(surface1);
        pool.release(surface2);

        WasmSurfacePool.PoolStats statsBefore = pool.getStats();
        int sizeBefore = statsBefore.currentPoolSize;

        int trimmed = pool.trim();

        WasmSurfacePool.PoolStats statsAfter = pool.getStats();
        assertTrue(statsAfter.currentPoolSize < sizeBefore,
                "Pool size should be reduced after trim");
        assertTrue(trimmed > 0, "Trim should have removed at least one surface");
    }

    /**
     * Test statistics tracking.
     */
    @Test
    public void testStatistics() {
        // Perform various operations
        WasmSurface s1 = pool.acquire(100, 100, Surface.FORMAT_INT_ARGB);
        pool.release(s1);

        WasmSurface s2 = pool.acquire(100, 100, Surface.FORMAT_INT_ARGB); // hit
        WasmSurface s3 = pool.acquire(200, 200, Surface.FORMAT_INT_ARGB); // miss

        WasmSurfacePool.PoolStats stats = pool.getStats();

        assertEquals(stats.acquireRequests, 3, "Should have 3 acquire requests");
        assertEquals(stats.poolHits, 1, "Should have 1 pool hit");
        assertEquals(stats.poolMisses, 2, "Should have 2 pool misses");
        assertEquals(stats.releaseCount, 1, "Should have 1 release");

        double hitRate = stats.getHitRate();
        assertTrue(hitRate > 0.0 && hitRate < 1.0,
                "Hit rate should be between 0 and 1: " + hitRate);

        s2.setPoolable(false);
        s2.destroy();
        s3.setPoolable(false);
        s3.destroy();
    }

    /**
     * Test that surfaces with zero dimensions are not pooled.
     */
    @Test
    public void testZeroDimensionsNotPooled() {
        WasmSurface surface = pool.acquire(100, 100, Surface.FORMAT_INT_ARGB);

        // Resize to zero dimensions
        surface.resize(0, 0);

        long destroyCountBefore = pool.getStats().destroyCount;
        pool.release(surface);
        long destroyCountAfter = pool.getStats().destroyCount;

        assertEquals(destroyCountBefore + 1, destroyCountAfter,
                "Surface with zero dimensions should be destroyed, not pooled");

        WasmSurfacePool.PoolStats stats = pool.getStats();
        assertEquals(stats.currentPoolSize, 0, "Pool should be empty");
    }

    /**
     * Test hit rate calculation.
     */
    @Test
    public void testHitRateCalculation() {
        // Create a pattern with known hit rate
        WasmSurface s1 = pool.acquire(100, 100, Surface.FORMAT_INT_ARGB);
        pool.release(s1); // Release to pool

        // 4 more acquires of the same key = 4 hits
        for (int i = 0; i < 4; i++) {
            WasmSurface s = pool.acquire(100, 100, Surface.FORMAT_INT_ARGB);
            pool.release(s);
        }

        WasmSurfacePool.PoolStats stats = pool.getStats();

        // Total: 5 acquires, 4 hits, 1 miss
        assertEquals(stats.acquireRequests, 5, "Should have 5 acquire requests");
        assertEquals(stats.poolHits, 4, "Should have 4 pool hits");
        assertEquals(stats.poolMisses, 1, "Should have 1 pool miss");

        double expectedHitRate = 4.0 / 5.0;
        double actualHitRate = stats.getHitRate();

        assertTrue(Math.abs(expectedHitRate - actualHitRate) < 0.001,
                "Hit rate should be ~0.8, got " + actualHitRate);
    }

    /**
     * Test that pool works correctly with multiple different keys.
     */
    @Test
    public void testMultipleKeys() {
        // Create surfaces with different dimensions and formats
        WasmSurface s1 = pool.acquire(100, 100, Surface.FORMAT_INT_ARGB);
        WasmSurface s2 = pool.acquire(200, 200, Surface.FORMAT_INT_RGB);
        WasmSurface s3 = pool.acquire(100, 100, Surface.FORMAT_INT_BGR);

        pool.release(s1);
        pool.release(s2);
        pool.release(s3);

        WasmSurfacePool.PoolStats stats = pool.getStats();
        assertEquals(stats.currentPoolSize, 3, "Should have 3 surfaces in pool");
        assertEquals(stats.uniqueKeys, 3, "Should have 3 unique keys");

        // Reacquire each one
        WasmSurface s1b = pool.acquire(100, 100, Surface.FORMAT_INT_ARGB);
        WasmSurface s2b = pool.acquire(200, 200, Surface.FORMAT_INT_RGB);
        WasmSurface s3b = pool.acquire(100, 100, Surface.FORMAT_INT_BGR);

        assertEquals(s1.getId(), s1b.getId(), "Should reuse surface 1");
        assertEquals(s2.getId(), s2b.getId(), "Should reuse surface 2");
        assertEquals(s3.getId(), s3b.getId(), "Should reuse surface 3");

        s1b.setPoolable(false);
        s1b.destroy();
        s2b.setPoolable(false);
        s2b.destroy();
        s3b.setPoolable(false);
        s3b.destroy();
    }
}
