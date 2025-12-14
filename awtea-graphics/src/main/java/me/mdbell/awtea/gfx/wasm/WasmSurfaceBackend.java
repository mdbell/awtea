package me.mdbell.awtea.gfx.wasm;

import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.gfx.SurfaceBackend;
import me.mdbell.awtea.util.logging.LogLevel;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.typedarrays.Int8Array;

public class WasmSurfaceBackend implements SurfaceBackend {

    private static final String WASM_MODULE_PATH = System.getProperty("me.mdbell.awtea.wasm.module_path",
            "build/wasm/awt_raster.wasm");

    final WasmAwtRasterizerExports exports;

    final SurfaceLRUCache surfaceCache;

    private static final Logger wasmLogger = LoggerFactory.getLogger("wasm.rasterizer");

    public WasmSurfaceBackend() {
        this.exports = WasmAwtLoader.load(WASM_MODULE_PATH, this::logFromWasm).await();
        this.surfaceCache = new SurfaceLRUCache(this, getSurfaceCacheSize());
    }

    private void logFromWasm(int level, int messagePtr, int messageLen) {
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
        wasmLogger.log(logLevel, message);
    }

    public WasmSurface createSurface(int width, int height, int pixelFormat) {

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height must be positive");
        }

        if (!Surface.isValidPixelFormat(pixelFormat)) {
            throw new IllegalArgumentException("Invalid pixel format: " + pixelFormat);
        }

        int surfaceId = exports.findFreeSurfaceId();
        if (surfaceId < 0) {
            throw new IllegalStateException("createSurface failed: " + surfaceId);
        }
        return new WasmSurface(this, surfaceId, width, height, pixelFormat);
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
            return createSurface(width, height, Surface.FORMAT_INT_ARGB);
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
