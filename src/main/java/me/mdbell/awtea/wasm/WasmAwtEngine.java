package me.mdbell.awtea.wasm;

public class WasmAwtEngine {

    private final WasmAwtRasterizerExports exports;

    private WasmAwtEngine() {
        this.exports = WasmAwtLoader.load("awt_rasterizer.wasm").await();
    }

    public WasmSurface createSurface(int width, int height, int pixelFormat) {

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height must be positive");
        }

        int surfaceId = exports.findFreeSurfaceId();
        if (surfaceId < 0) {
            throw new IllegalStateException("createSurface failed: " + surfaceId);
        }
        return new WasmSurface(exports, surfaceId, width, height, pixelFormat);
    }

    public SurfaceCommandBuffer createCommandBuffer(int maxCommands) {
        if (maxCommands <= 0) {
            throw new IllegalArgumentException("maxCommands must be positive");
        }
        return new SurfaceCommandBuffer(exports, maxCommands);
    }

    public static WasmAwtEngine get() {
        return new WasmAwtEngine();
    }
}
