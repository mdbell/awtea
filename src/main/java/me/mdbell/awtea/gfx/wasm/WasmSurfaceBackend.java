package me.mdbell.awtea.gfx.wasm;

import me.mdbell.awtea.classlib.java.awt.image.TColorModel;
import me.mdbell.awtea.classlib.java.awt.image.TWritableRaster;
import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.gfx.SurfaceBackend;

public class WasmSurfaceBackend implements SurfaceBackend {

    private static final String WASM_MODULE_PATH = System.getProperty("me.mdbell.awtea.wasm.module_path",
            "build/wasm/awt_raster.wasm");

    private final WasmAwtRasterizerExports exports;

    private WasmSurfaceBackend() {
        this.exports = WasmAwtLoader.load(WASM_MODULE_PATH).await();
    }

    public WasmSurface createSurface(int width, int height, WasmPixelFormat pixelFormat) {

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height must be positive");
        }

        int surfaceId = exports.findFreeSurfaceId();
        if (surfaceId < 0) {
            throw new IllegalStateException("createSurface failed: " + surfaceId);
        }
        return new WasmSurface(exports, surfaceId, width, height, pixelFormat.ordinal());
    }

    public static WasmSurfaceBackend get() {
        return new WasmSurfaceBackend();
    }

    @Override
    public Surface createCompatibleSurface(int width, int height, int bufferedImageType) {
        WasmPixelFormat format = WasmPixelFormat.fromBufferedImageType(bufferedImageType);
        if (format != null) {
            return createSurface(width, height, format);
        }
        return null;
    }

    @Override
    public Surface createCompatibleSurface(TColorModel cm, TWritableRaster raster,
                                           boolean isRasterPremultiplied) {
        // Not supported in Wasm backend - we need to allocate surface memory in the WASM module
        return null;
    }
}
