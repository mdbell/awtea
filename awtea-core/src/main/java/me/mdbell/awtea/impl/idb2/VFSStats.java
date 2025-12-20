package me.mdbell.awtea.impl.idb2;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance monitoring and statistics for VFS operations.
 */
public class VFSStats {
    
    private final AtomicLong reads = new AtomicLong(0);
    private final AtomicLong writes = new AtomicLong(0);
    private final AtomicLong bytesRead = new AtomicLong(0);
    private final AtomicLong bytesWritten = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong dbOperations = new AtomicLong(0);
    private final AtomicLong errors = new AtomicLong(0);
    
    private long startTime = System.currentTimeMillis();
    
    public void recordRead(int bytes) {
        reads.incrementAndGet();
        bytesRead.addAndGet(bytes);
    }
    
    public void recordWrite(int bytes) {
        writes.incrementAndGet();
        bytesWritten.addAndGet(bytes);
    }
    
    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }
    
    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }
    
    public void recordDbOperation() {
        dbOperations.incrementAndGet();
    }
    
    public void recordError() {
        errors.incrementAndGet();
    }
    
    public long getReads() {
        return reads.get();
    }
    
    public long getWrites() {
        return writes.get();
    }
    
    public long getBytesRead() {
        return bytesRead.get();
    }
    
    public long getBytesWritten() {
        return bytesWritten.get();
    }
    
    public long getCacheHits() {
        return cacheHits.get();
    }
    
    public long getCacheMisses() {
        return cacheMisses.get();
    }
    
    public double getCacheHitRatio() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        return total == 0 ? 0.0 : (double) hits / total;
    }
    
    public long getDbOperations() {
        return dbOperations.get();
    }
    
    public long getErrors() {
        return errors.get();
    }
    
    public long getUptime() {
        return System.currentTimeMillis() - startTime;
    }
    
    public void reset() {
        reads.set(0);
        writes.set(0);
        bytesRead.set(0);
        bytesWritten.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        dbOperations.set(0);
        errors.set(0);
        startTime = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return String.format(
            "VFSStats[reads=%d, writes=%d, bytesRead=%d, bytesWritten=%d, " +
            "cacheHits=%d, cacheMisses=%d, hitRatio=%.2f%%, dbOps=%d, errors=%d, uptime=%dms]",
            reads.get(), writes.get(), bytesRead.get(), bytesWritten.get(),
            cacheHits.get(), cacheMisses.get(), getCacheHitRatio() * 100,
            dbOperations.get(), errors.get(), getUptime()
        );
    }
}
