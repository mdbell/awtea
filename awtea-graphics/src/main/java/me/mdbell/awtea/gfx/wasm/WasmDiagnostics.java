package me.mdbell.awtea.gfx.wasm;

/**
 * Diagnostics API for WASM surface/context system.
 * Provides runtime information about active surfaces, contexts, and resource usage.
 */
public class WasmDiagnostics {
    
    private final WasmAwtRasterizerExports exports;
    
    WasmDiagnostics(WasmAwtRasterizerExports exports) {
        this.exports = exports;
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
        
        if (isSurfaceCapacityWarning(0.8)) {
            sb.append("  WARNING: Surface capacity exceeds 80%\n");
        }
        if (isContextCapacityWarning(0.8)) {
            sb.append("  WARNING: Context capacity exceeds 80%\n");
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return getReport();
    }
}
