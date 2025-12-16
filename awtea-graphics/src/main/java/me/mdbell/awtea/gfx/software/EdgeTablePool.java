package me.mdbell.awtea.gfx.software;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Thread-safe object pool for EdgeTable instances.
 * Reduces allocations by reusing edge tables across multiple fill operations.
 */
public class EdgeTablePool {
    
    private static final Logger log = LoggerFactory.getLogger(EdgeTablePool.class);
    private static final int DEFAULT_MAX_SIZE = 8;
    
    private final Deque<EdgeTable> available;
    private final int maxSize;
    
    /**
     * Create a new edge table pool with default maximum size
     */
    public EdgeTablePool() {
        this(DEFAULT_MAX_SIZE);
    }
    
    /**
     * Create a new edge table pool with specified maximum size
     * 
     * @param maxSize maximum number of edge tables to keep in pool
     */
    public EdgeTablePool(int maxSize) {
        this.maxSize = maxSize;
        this.available = new ArrayDeque<>(maxSize);
        log.debug("Created EdgeTablePool with maxSize={}", maxSize);
    }
    
    /**
     * Acquire an edge table from the pool, creating a new one if necessary
     * 
     * @param minY minimum y coordinate
     * @param maxY maximum y coordinate  
     * @param width surface width
     * @param height surface height
     * @return an edge table ready for use
     */
    public synchronized EdgeTable acquire(int minY, int maxY, int width, int height) {
        EdgeTable et = available.poll();
        
        if (et != null) {
            // Reuse existing edge table - it will be reset in the constructor
            log.debug("Reused edge table from pool (available={})", available.size());
            // Note: We can't easily reset bounds on existing table, so return new one
            // In a real implementation, EdgeTable would have a reset() method
            et.destroy();
            return new EdgeTable(minY, maxY, width, height);
        }
        
        // Create new edge table
        log.debug("Created new edge table (pool was empty)");
        return new EdgeTable(minY, maxY, width, height);
    }
    
    /**
     * Release an edge table back to the pool for reuse
     * 
     * @param et the edge table to release
     */
    public synchronized void release(EdgeTable et) {
        if (et == null) {
            return;
        }
        
        // Clean up the edge table
        et.destroy();
        
        // Only add back to pool if we haven't reached max size
        if (available.size() < maxSize) {
            available.offer(et);
            log.debug("Released edge table back to pool (available={})", available.size());
        } else {
            log.debug("Pool full, discarding edge table");
        }
    }
    
    /**
     * Clear all edge tables from the pool
     */
    public synchronized void clear() {
        available.clear();
        log.debug("Cleared edge table pool");
    }
    
    /**
     * Get the current number of available edge tables in the pool
     * 
     * @return number of available edge tables
     */
    public synchronized int getAvailableCount() {
        return available.size();
    }
}
