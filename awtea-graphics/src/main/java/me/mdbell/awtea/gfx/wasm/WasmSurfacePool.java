package me.mdbell.awtea.gfx.wasm;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A thread-safe pool for reusing WasmSurface objects.
 * Surfaces are pooled by (width, height, pixel format) key to enable efficient
 * reuse
 * for text rendering, temporary buffers, and other high-frequency surface
 * creation scenarios.
 */
public class WasmSurfacePool {

    private static final Logger log = LoggerFactory.getLogger(WasmSurfacePool.class);

    /**
     * System property to enable/disable surface pooling.
     * Default: true
     */
    private static final String PROP_POOL_ENABLED = "me.mdbell.awtea.wasm.surface_pool_enabled";

    /**
     * System property to configure maximum surfaces per key (width, height,
     * format).
     * Default: 4
     */
    private static final String PROP_MAX_PER_KEY = "me.mdbell.awtea.wasm.surface_pool_max_per_key";

    /**
     * System property to configure maximum total surfaces in pool.
     * Default: 100
     */
    private static final String PROP_MAX_TOTAL = "me.mdbell.awtea.wasm.surface_pool_max_total";

    private final boolean enabled;
    private final int maxPerKey;
    private final int maxTotal;

    // Map from SurfaceKey to a queue of available surfaces (LRU-ordered: head is
    // most recent)
    private final Map<SurfaceKey, Deque<PooledSurface>> pool;

    // Statistics tracking
    private final AtomicLong acquireRequests = new AtomicLong(0);
    private final AtomicLong poolHits = new AtomicLong(0);
    private final AtomicLong poolMisses = new AtomicLong(0);
    private final AtomicLong releaseCount = new AtomicLong(0);
    private final AtomicLong destroyCount = new AtomicLong(0);

    private final WasmSurfaceBackend backend;

    public WasmSurfacePool(WasmSurfaceBackend backend) {
        this.backend = backend;
        this.enabled = getBooleanProperty(PROP_POOL_ENABLED, true);
        this.maxPerKey = getIntProperty(PROP_MAX_PER_KEY, 4);
        this.maxTotal = getIntProperty(PROP_MAX_TOTAL, 100);
        this.pool = new ConcurrentHashMap<>();

        if (enabled) {
            log.info("WasmSurfacePool enabled: maxPerKey={}, maxTotal={}", maxPerKey, maxTotal);
        } else {
            log.info("WasmSurfacePool disabled");
        }
    }

    /**
     * Acquire a surface from the pool or create a new one.
     * Uses LRU ordering - acquires the most recently used surface for this key.
     * 
     * @param width       The desired width
     * @param height      The desired height
     * @param pixelFormat The desired pixel format
     * @return A WasmSurface ready for use
     */
    public WasmSurface acquire(int width, int height, int pixelFormat) {
        acquireRequests.incrementAndGet();

        if (!enabled) {
            poolMisses.incrementAndGet();
            return createNewSurface(width, height, pixelFormat);
        }

        SurfaceKey key = new SurfaceKey(width, height, pixelFormat);
        Deque<PooledSurface> queue = pool.get(key);

        if (queue != null) {
            synchronized (queue) {
                PooledSurface pooled = queue.pollFirst(); // Get most recently used
                if (pooled != null) {
                    poolHits.incrementAndGet();
                    log.debug("Pool hit: {}x{} format={}, pool size={}",
                            width, height, pixelFormat, queue.size());
                    pooled.lastAccessTime = System.currentTimeMillis();
                    return pooled.surface;
                }
            }
        }
        poolMisses.incrementAndGet();
        return createNewSurface(width, height, pixelFormat);
    }

    /**
     * Release a surface back to the pool for reuse.
     * Uses LRU ordering - adds to front as most recently used.
     * If the pool is full for this key, the surface is destroyed.
     * 
     * @param surface The surface to release
     */
    public void release(WasmSurface surface) {
        if (surface == null) {
            return;
        }

        releaseCount.incrementAndGet();

        if (!enabled) {
            surface.destroyDirect();
            destroyCount.incrementAndGet();
            return;
        }

        int width = surface.getWidth();
        int height = surface.getHeight();
        int format = surface.getFormat();

        // Don't pool surfaces with zero dimensions (they are marked as freed in WASM)
        // See WasmSurface.destroyInternal() - surfaces with width & height of 0 are
        // considered free
        if (width <= 0 || height <= 0) {
            surface.destroyDirect();
            destroyCount.incrementAndGet();
            return;
        }

        SurfaceKey key = new SurfaceKey(width, height, format);
        Deque<PooledSurface> queue = pool.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (queue) {
            // Check if we're at the per-key limit
            if (queue.size() >= maxPerKey) {
                surface.destroyDirect();
                destroyCount.incrementAndGet();
                log.debug("Pool full for key {}x{} format={}, destroying surface",
                        width, height, format);
                return;
            }

            // Check total pool size
            int totalSize = getTotalPoolSize();
            if (totalSize >= maxTotal) {
                // Evict LRU surface from across all keys
                evictLRUSurface();
                // Try again
                if (getTotalPoolSize() >= maxTotal) {
                    surface.destroyDirect();
                    destroyCount.incrementAndGet();
                    log.debug("Total pool size {} >= max {} after eviction, destroying surface",
                            getTotalPoolSize(), maxTotal);
                    return;
                }
            }

            // Add to front as most recently used
            PooledSurface pooled = new PooledSurface(surface, System.currentTimeMillis());
            queue.addFirst(pooled);
            log.debug("Released surface to pool: {}x{} format={}, pool size={}",
                    width, height, format, queue.size());
        }
    }

    /**
     * Clear all surfaces from the pool, destroying them.
     */
    public void clear() {
        log.info("Clearing surface pool");
        int cleared = 0;

        for (Deque<PooledSurface> queue : pool.values()) {
            synchronized (queue) {
                while (!queue.isEmpty()) {
                    PooledSurface pooled = queue.pollFirst();
                    if (pooled != null) {
                        pooled.surface.destroyDirect();
                        cleared++;
                    }
                }
            }
        }

        pool.clear();
        destroyCount.addAndGet(cleared);
        log.info("Cleared {} surfaces from pool", cleared);
    }

    /**
     * Evict the least recently used surface from the pool (across all keys).
     * Called when total pool size exceeds maximum.
     */
    private void evictLRUSurface() {
        PooledSurface oldest = null;
        Deque<PooledSurface> oldestQueue = null;

        // Find the oldest surface across all queues
        for (Deque<PooledSurface> queue : pool.values()) {
            synchronized (queue) {
                if (!queue.isEmpty()) {
                    PooledSurface last = queue.peekLast(); // Least recently used in this queue
                    if (oldest == null || last.lastAccessTime < oldest.lastAccessTime) {
                        oldest = last;
                        oldestQueue = queue;
                    }
                }
            }
        }

        // Remove and destroy the oldest surface
        if (oldest != null && oldestQueue != null) {
            synchronized (oldestQueue) {
                oldestQueue.remove(oldest);
                oldest.surface.destroyDirect();
                destroyCount.incrementAndGet();
                log.debug("Evicted LRU surface from pool");
            }
        }
    }

    /**
     * Trim the pool to reduce memory usage.
     * Removes the oldest (LRU) surface from each key's queue if it has more than
     * one surface.
     * 
     * @return Number of surfaces removed
     */
    public int trim() {
        if (!enabled) {
            return 0;
        }

        int removed = 0;
        for (Deque<PooledSurface> queue : pool.values()) {
            synchronized (queue) {
                // Keep at least one surface per key, remove the LRU if there are more
                if (queue.size() > 1) {
                    PooledSurface pooled = queue.pollLast(); // Remove least recently used
                    if (pooled != null) {
                        pooled.surface.destroyDirect();
                        removed++;
                    }
                }
            }
        }

        if (removed > 0) {
            destroyCount.addAndGet(removed);
            log.debug("Trimmed {} surfaces from pool", removed);
        }

        return removed;
    }

    /**
     * Evict surfaces that haven't been used for a specified time period.
     * 
     * @param maxAgeMs Maximum age in milliseconds
     * @return Number of surfaces evicted
     */
    public int evictOld(long maxAgeMs) {
        if (!enabled) {
            return 0;
        }

        long now = System.currentTimeMillis();
        int evicted = 0;

        for (Deque<PooledSurface> queue : pool.values()) {
            synchronized (queue) {
                // Remove old surfaces from the back (least recently used end)
                while (!queue.isEmpty()) {
                    PooledSurface pooled = queue.peekLast();
                    if (pooled != null && (now - pooled.lastAccessTime) > maxAgeMs) {
                        queue.pollLast();
                        pooled.surface.destroyDirect();
                        evicted++;
                    } else {
                        break; // Rest are newer
                    }
                }
            }
        }

        if (evicted > 0) {
            destroyCount.addAndGet(evicted);
            log.debug("Evicted {} old surfaces from pool", evicted);
        }

        return evicted;
    }

    /**
     * Get statistics about pool usage.
     * 
     * @return PoolStats containing hit/miss rates and other metrics
     */
    public PoolStats getStats() {
        return new PoolStats(
                acquireRequests.get(),
                poolHits.get(),
                poolMisses.get(),
                releaseCount.get(),
                destroyCount.get(),
                getTotalPoolSize(),
                pool.size());
    }

    /**
     * Get the total number of surfaces currently in the pool.
     */
    private int getTotalPoolSize() {
        int total = 0;
        for (Deque<PooledSurface> queue : pool.values()) {
            synchronized (queue) {
                total += queue.size();
            }
        }
        return total;
    }

    /**
     * Wrapper for surfaces in the pool with LRU tracking.
     */
    private static class PooledSurface {
        final WasmSurface surface;
        long lastAccessTime;

        PooledSurface(WasmSurface surface, long lastAccessTime) {
            this.surface = surface;
            this.lastAccessTime = lastAccessTime;
        }
    }

    /**
     * Create a new surface (not from pool).
     */
    private WasmSurface createNewSurface(int width, int height, int pixelFormat) {
        int surfaceId = backend.exports.findFreeSurfaceId();
        if (surfaceId < 0) {
            throw new IllegalStateException("createSurface failed: no free surface ID");
        }
        return new WasmSurface(backend, surfaceId, width, height, pixelFormat);
    }

    /**
     * Key for pooling surfaces by dimensions and format.
     */
    private static class SurfaceKey {
        private final int width;
        private final int height;
        private final int pixelFormat;

        SurfaceKey(int width, int height, int pixelFormat) {
            this.width = width;
            this.height = height;
            this.pixelFormat = pixelFormat;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            SurfaceKey that = (SurfaceKey) o;
            return width == that.width &&
                    height == that.height &&
                    pixelFormat == that.pixelFormat;
        }

        @Override
        public int hashCode() {
            return Objects.hash(width, height, pixelFormat);
        }

        @Override
        public String toString() {
            return String.format("SurfaceKey{%dx%d, format=%d}", width, height, pixelFormat);
        }
    }

    /**
     * Statistics about pool usage.
     */
    public static class PoolStats {
        public final long acquireRequests;
        public final long poolHits;
        public final long poolMisses;
        public final long releaseCount;
        public final long destroyCount;
        public final int currentPoolSize;
        public final int uniqueKeys;

        PoolStats(long acquireRequests, long poolHits, long poolMisses,
                long releaseCount, long destroyCount, int currentPoolSize, int uniqueKeys) {
            this.acquireRequests = acquireRequests;
            this.poolHits = poolHits;
            this.poolMisses = poolMisses;
            this.releaseCount = releaseCount;
            this.destroyCount = destroyCount;
            this.currentPoolSize = currentPoolSize;
            this.uniqueKeys = uniqueKeys;
        }

        public double getHitRate() {
            return acquireRequests > 0 ? (double) poolHits / acquireRequests : 0.0;
        }

        public double getMissRate() {
            return acquireRequests > 0 ? (double) poolMisses / acquireRequests : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                    "PoolStats{requests=%d, hits=%d (%.1f%%), misses=%d (%.1f%%), " +
                            "releases=%d, destroys=%d, poolSize=%d, keys=%d}",
                    acquireRequests, poolHits, getHitRate() * 100,
                    poolMisses, getMissRate() * 100,
                    releaseCount, destroyCount, currentPoolSize, uniqueKeys);
        }
    }

    private static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = System.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    private static int getIntProperty(String key, int defaultValue) {
        String value = System.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid integer property {}: {}, using default {}",
                    key, value, defaultValue);
            return defaultValue;
        }
    }
}
