package me.mdbell.awtea.gfx.wasm;

/**
 * Diagnostics API for WASM surface/context system.
 * Provides runtime information about active surfaces, contexts, and resource usage.
 */
public class WasmDiagnostics {
    
    private final WasmAwtRasterizerExports exports;
    
    // Memory tracking data - updated via callback from WASM
    private volatile int allocatedBytes = 0;
    private volatile int allocationCount = 0;
    private volatile int peakBytes = 0;
    
    WasmDiagnostics(WasmAwtRasterizerExports exports) {
        this.exports = exports;
    }
    
    /**
     * Internal method called by WasmSurfaceBackend to update memory stats.
     * @param allocatedBytes Current allocated memory in bytes
     * @param allocationCount Current number of allocations
     * @param peakBytes Peak allocated memory in bytes
     */
    void updateMemoryStats(int allocatedBytes, int allocationCount, int peakBytes) {
        this.allocatedBytes = allocatedBytes;
        this.allocationCount = allocationCount;
        this.peakBytes = peakBytes;
    }
    
    /**
     * Get the number of currently active (allocated) surfaces.
     * @return Number of surfaces with non-zero pixel data
     */
    public int getActiveSurfaceCount() {
        return exports.getActiveSurfaceCount();
    }
    
    /**
     * Get the number of currently active contexts.
     * @return Number of contexts currently referencing surfaces
     */
    public int getActiveContextCount() {
        return exports.getActiveContextCount();
    }
    
    /**
     * Get the reference count for a specific surface.
     * The reference count indicates how many contexts are currently using this surface.
     * 
     * @param surfaceId The surface ID to query
     * @return Reference count, or -1 if surface ID is invalid
     */
    public int getSurfaceRefCount(int surfaceId) {
        return exports.getSurfaceRefCount(surfaceId);
    }
    
    /**
     * Get the maximum number of surfaces supported by the WASM module.
     * @return Maximum surface capacity (typically 1024)
     */
    public int getMaxSurfaces() {
        return exports.getMaxSurfaces();
    }
    
    /**
     * Get the maximum number of contexts supported by the WASM module.
     * @return Maximum context capacity (typically 2048)
     */
    public int getMaxContexts() {
        return exports.getMaxContexts();
    }
    
    /**
     * Calculate the percentage of surface capacity currently in use.
     * @return Usage percentage (0.0 to 1.0)
     */
    public double getSurfaceUtilization() {
        int active = getActiveSurfaceCount();
        int max = getMaxSurfaces();
        return max > 0 ? (double) active / max : 0.0;
    }
    
    /**
     * Calculate the percentage of context capacity currently in use.
     * @return Usage percentage (0.0 to 1.0)
     */
    public double getContextUtilization() {
        int active = getActiveContextCount();
        int max = getMaxContexts();
        return max > 0 ? (double) active / max : 0.0;
    }
    
    /**
     * Check if the system is approaching surface capacity.
     * @param threshold Warning threshold (0.0 to 1.0, default 0.8 = 80%)
     * @return true if utilization exceeds threshold
     */
    public boolean isSurfaceCapacityWarning(double threshold) {
        return getSurfaceUtilization() >= threshold;
    }
    
    /**
     * Check if the system is approaching context capacity.
     * @param threshold Warning threshold (0.0 to 1.0, default 0.8 = 80%)
     * @return true if utilization exceeds threshold
     */
    public boolean isContextCapacityWarning(double threshold) {
        return getContextUtilization() >= threshold;
    }
    
    /**
     * Get the current allocated memory in bytes (tracked by WASM malloc).
     * @return Currently allocated memory in bytes
     */
    public int getAllocatedBytes() {
        return allocatedBytes;
    }
    
    /**
     * Get the current number of active allocations (tracked by WASM malloc).
     * @return Number of active allocations
     */
    public int getAllocationCount() {
        return allocationCount;
    }
    
    /**
     * Get the peak allocated memory in bytes (tracked by WASM malloc).
     * @return Peak allocated memory in bytes since initialization
     */
    public int getPeakBytes() {
        return peakBytes;
    }
    
    /**
     * Get allocated memory in kilobytes.
     * @return Currently allocated memory in KB
     */
    public double getAllocatedKB() {
        return allocatedBytes / 1024.0;
    }
    
    /**
     * Get allocated memory in megabytes.
     * @return Currently allocated memory in MB
     */
    public double getAllocatedMB() {
        return allocatedBytes / (1024.0 * 1024.0);
    }
    
    /**
     * Get peak memory in kilobytes.
     * @return Peak allocated memory in KB
     */
    public double getPeakKB() {
        return peakBytes / 1024.0;
    }
    
    /**
     * Get peak memory in megabytes.
     * @return Peak allocated memory in MB
     */
    public double getPeakMB() {
        return peakBytes / (1024.0 * 1024.0);
    }
    
    /**
     * Get a formatted diagnostic report as a string.
     * @return Human-readable diagnostic information
     */
    public String getReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("WASM Surface/Context Diagnostics:\n");
        sb.append(String.format("  Surfaces: %d / %d (%.1f%%)\n", 
                getActiveSurfaceCount(), getMaxSurfaces(), getSurfaceUtilization() * 100));
        sb.append(String.format("  Contexts: %d / %d (%.1f%%)\n", 
                getActiveContextCount(), getMaxContexts(), getContextUtilization() * 100));
        sb.append(String.format("  Memory: %.2f KB allocated (%.2f KB peak) in %d allocations\n",
                getAllocatedKB(), getPeakKB(), getAllocationCount()));
        
        if (isSurfaceCapacityWarning(0.8)) {
            sb.append("  WARNING: Surface capacity exceeds 80%\n");
        }
        if (isContextCapacityWarning(0.8)) {
            sb.append("  WARNING: Context capacity exceeds 80%\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Get a compact single-line report suitable for UI display.
     * @return Compact diagnostic string
     */
    public String getCompactReport() {
        return String.format("WASM: %d/%d surf, %d/%d ctx, %.1fKB mem",
                getActiveSurfaceCount(), getMaxSurfaces(),
                getActiveContextCount(), getMaxContexts(),
                getAllocatedKB());
    }
    
    @Override
    public String toString() {
        return getReport();
    }
}
