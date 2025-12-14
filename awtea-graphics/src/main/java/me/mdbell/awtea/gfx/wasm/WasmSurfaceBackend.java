package me.mdbell.awtea.gfx.wasm;

import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.gfx.SurfaceBackend;
import me.mdbell.awtea.util.logging.LogLevel;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import java.awt.image.BufferedImage;

import org.teavm.jso.browser.Performance;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.typedarrays.Int8Array;

public class WasmSurfaceBackend implements SurfaceBackend {

    private static final Logger log = LoggerFactory.getLogger(WasmSurfaceBackend.class);

    private static final String WASM_MODULE_PATH = System.getProperty("me.mdbell.awtea.wasm.module_path",
            "build/wasm/awt_raster.wasm");

    final WasmAwtRasterizerExports exports;

    final SurfaceLRUCache surfaceCache;
    
    private final WasmSurfacePool surfacePool;

    public WasmSurfaceBackend() {

        WasmAwtRasterizerImportsEnv env = JSObjects.create();

        env.setLogCallback(this::handleWasmLog);
        env.setTimingCallback(Performance::now);
        env.setMemoryCallback(this::reportMemoryUsage);
        env.setAbortCallback(this::handleAbort);
        env.setAssertionCallback(this::handleAssertionFailure);

        this.exports = WasmAwtLoader.load(
                WASM_MODULE_PATH,
                env
        ).await();
        // Initialize the surface system (sets all contexts to free state)
        this.exports.initSurfaceSystem();
        this.surfaceCache = new SurfaceLRUCache(this, getSurfaceCacheSize());
        this.surfacePool = new WasmSurfacePool(this);
    }

    private void handleAbort() {
        log.error("WASM module aborted execution");
        throw new IllegalStateException("WASM module aborted execution");
    }

    private void handleWasmLog(int level, int messagePtr, int messageLen) {
        Int8Array arr = new Int8Array(
                exports.getMemory().getBuffer(),
                messagePtr,
                messageLen
        );
        String message = new String(arr.copyToJavaArray());
        LogLevel logLevel = LogLevel.INFO;
        switch (level) {
            case 0:
                logLevel = LogLevel.ERROR;
                break;
            case 1:
                logLevel = LogLevel.WARN;
                break;
            case 3:
                logLevel = LogLevel.DEBUG;
                break;
            default:
                break;
        }
        log.log(logLevel, message);
    }

    private void reportMemoryUsage(int allocatedBytes, int allocatedCount, int peakBytes) {
        log.debug("WASM Memory Usage - Allocated: {} bytes in {} allocations, Peak: {} bytes",
                allocatedBytes, allocatedCount, peakBytes);
    }

    private void handleAssertionFailure(int exprPtr, int exprLen, int filePtr, int fileLen, int line) {
        Int8Array exprArr = new Int8Array(
                exports.getMemory().getBuffer(),
                exprPtr,
                exprLen
        );
        Int8Array fileArr = new Int8Array(
                exports.getMemory().getBuffer(),
                filePtr,
                fileLen
        );
        String expr = new String(exprArr.copyToJavaArray());
        String file = new String(fileArr.copyToJavaArray());

        log.error("WASM assertion failed: {} at {}:{}", expr, file, line);
    }

    public WasmSurface createSurface(int width, int height, int pixelFormat) {

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height must be positive");
        }

        if (!Surface.isValidPixelFormat(pixelFormat)) {
            throw new IllegalArgumentException("Invalid pixel format: " + pixelFormat);
        }

        // Use pool to acquire or create surface
        return surfacePool.acquire(width, height, pixelFormat);
    }
    
    /**
     * Release a surface back to the pool for potential reuse.
     * Should be called instead of destroy() when the surface is no longer needed.
     * 
     * @param surface The surface to release
     */
    public void releaseSurface(WasmSurface surface) {
        surfacePool.release(surface);
    }
    
    /**
     * Get the surface pool for this backend.
     * Useful for accessing pool statistics and management operations.
     * 
     * @return The WasmSurfacePool instance
     */
    public WasmSurfacePool getSurfacePool() {
        return surfacePool;
    }

    @Override
    public Surface createCompatibleSurface(int width, int height, int bufferedImageType) {
        int format = Surface.fromBufferedImageType(bufferedImageType);
        if (format == -1) {
            return null;
        }
        return createSurface(width, height, format);
    }

    @Override
    public Surface createCompatibleSurface(Object cm, Object raster,
                                           boolean isRasterPremultiplied, int bufferedImageType) {
        // Not supported in Wasm backend - we need to allocate surface memory in the WASM module
        return null;
    }

    @Override
    public Surface createFontRenderSurface(int width, int height) {
        // Use ARGB format for text rendering to support alpha transparency
        try {
            return createSurface(width, height, BufferedImage.TYPE_INT_ARGB);
        } catch (Exception e) {
            // If WASM surface creation fails, return null to allow fallback to software
            return null;
        }
    }

    private static int getSurfaceCacheSize() {
        String prop = System.getProperty("me.mdbell.awtea.wasm.surface_cache_size");
        if (prop != null) {
            try {
                return Integer.parseInt(prop);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return 100;
    }
}
