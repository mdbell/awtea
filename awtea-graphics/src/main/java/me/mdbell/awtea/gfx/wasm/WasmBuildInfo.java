package me.mdbell.awtea.gfx.wasm;

import org.teavm.jso.typedarrays.Int8Array;

/**
 * Build information for the WASM rasterizer module.
 * This information is cached at initialization time and provides
 * version, build timestamp, and debug flag information.
 */
public class WasmBuildInfo {
    
    private final String version;
    private final String buildDate;
    private final String buildTime;
    private final int buildFlags;
    private final String buildFlagsDescription;
    private final int stackInfoPtr;
    private final int stackInfoCount;
    
    // Build flag constants (must match awt_build_info.h)
    public static final int BUILD_FLAG_DEBUG = 1 << 0;
    public static final int BUILD_FLAG_STACK_TRACKING = 1 << 1;
    public static final int BUILD_FLAG_ASSERTIONS = 1 << 2;
    public static final int BUILD_FLAG_LOGGING = 1 << 3;
    public static final int BUILD_FLAG_MEMORY_TRACKING = 1 << 4;
    
    /**
     * Read and cache build information from the WASM module.
     * Should be called once during initialization.
     * 
     * @param exports The WASM module exports
     */
    WasmBuildInfo(WasmAwtRasterizerExports exports) {
        // Read version
        int versionPtr = exports.getBuildVersionPtr();
        this.version = readNullTerminatedString(exports, versionPtr);
        
        // Read build date
        int datePtr = exports.getBuildDatePtr();
        this.buildDate = readNullTerminatedString(exports, datePtr);
        
        // Read build time
        int timePtr = exports.getBuildTimePtr();
        this.buildTime = readNullTerminatedString(exports, timePtr);
        
        // Read build flags
        this.buildFlags = exports.getBuildFlags();
        
        // Read build flags description
        int flagsStrPtr = exports.getBuildFlagsStringPtr();
        this.buildFlagsDescription = readNullTerminatedString(exports, flagsStrPtr);
        
        // Cache stack info pointers (safe to query even after crash)
        this.stackInfoPtr = exports.getStackInfoPtr();
        this.stackInfoCount = exports.getStackInfoCount();
    }
    
    /**
     * Helper to read a null-terminated string from WASM memory.
     */
    private String readNullTerminatedString(WasmAwtRasterizerExports exports, int ptr) {
        if (ptr == 0) {
            return "";
        }
        
        // Find the length by scanning for null terminator
        WasmMemory memory = exports.getMemory();
        Int8Array view = new Int8Array(memory.getBuffer());
        int len = 0;
        int maxLen = 1024; // Safety limit
        while (len < maxLen && view.get(ptr + len) != 0) {
            len++;
        }
        
        // Read the string bytes
        Int8Array strBytes = new Int8Array(memory.getBuffer(), ptr, len);
        return new String(strBytes.copyToJavaArray());
    }
    
    /**
     * Get the build version string.
     * @return Version string (e.g., "0.1.0-dev" or "0.1.0")
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * Get the build date.
     * @return Date string (e.g., "Dec 21 2025")
     */
    public String getBuildDate() {
        return buildDate;
    }
    
    /**
     * Get the build time.
     * @return Time string (e.g., "17:16:15")
     */
    public String getBuildTime() {
        return buildTime;
    }
    
    /**
     * Get the bit-packed build flags.
     * @return 32-bit integer with build flags
     */
    public int getBuildFlags() {
        return buildFlags;
    }
    
    /**
     * Get the human-readable build flags description.
     * @return Description string (e.g., "DEBUG +STACK +ASSERT +LOG +MEMTRACK")
     */
    public String getBuildFlagsDescription() {
        return buildFlagsDescription;
    }
    
    /**
     * Get the cached stack info pointer (safe to access after crash).
     * @return Pointer to stack buffer, or 0 if stack tracking disabled
     */
    public int getStackInfoPtr() {
        return stackInfoPtr;
    }
    
    /**
     * Get the cached stack info count (safe to access after crash).
     * @return Maximum stack depth, or 0 if stack tracking disabled
     */
    public int getStackInfoCount() {
        return stackInfoCount;
    }
    
    /**
     * Check if this is a debug build.
     * @return true if BUILD_FLAG_DEBUG is set
     */
    public boolean isDebugBuild() {
        return (buildFlags & BUILD_FLAG_DEBUG) != 0;
    }
    
    /**
     * Check if stack tracking is enabled.
     * @return true if BUILD_FLAG_STACK_TRACKING is set
     */
    public boolean hasStackTracking() {
        return (buildFlags & BUILD_FLAG_STACK_TRACKING) != 0;
    }
    
    /**
     * Check if assertions are enabled.
     * @return true if BUILD_FLAG_ASSERTIONS is set
     */
    public boolean hasAssertions() {
        return (buildFlags & BUILD_FLAG_ASSERTIONS) != 0;
    }
    
    /**
     * Check if logging is enabled.
     * @return true if BUILD_FLAG_LOGGING is set
     */
    public boolean hasLogging() {
        return (buildFlags & BUILD_FLAG_LOGGING) != 0;
    }
    
    /**
     * Check if memory tracking is enabled.
     * @return true if BUILD_FLAG_MEMORY_TRACKING is set
     */
    public boolean hasMemoryTracking() {
        return (buildFlags & BUILD_FLAG_MEMORY_TRACKING) != 0;
    }
    
    /**
     * Get a formatted report of build information.
     * @return Multi-line build information string
     */
    public String getReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("WASM Rasterizer Build Information:\n");
        sb.append(String.format("  Version: %s\n", version));
        sb.append(String.format("  Built: %s at %s\n", buildDate, buildTime));
        sb.append(String.format("  Flags: 0x%08X\n", buildFlags));
        sb.append(String.format("  Description: %s\n", buildFlagsDescription));
        
        if (hasStackTracking()) {
            sb.append(String.format("  Stack Buffer: 0x%08X (%d frames)\n", stackInfoPtr, stackInfoCount));
        }
        
        return sb.toString();
    }
    
    /**
     * Get a compact single-line report.
     * @return Compact build information string
     */
    public String getCompactReport() {
        return String.format("WASM: %s (%s)", version, buildFlagsDescription);
    }
    
    @Override
    public String toString() {
        return getReport();
    }
}
