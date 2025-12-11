package me.mdbell.awtea.wasm;

import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int32Array;

public final class WasmSurface {
    private final WasmAwtRasterizerExports exports;
    private int surfaceId;
    private final ArrayBuffer memoryBuffer;

    public WasmSurface(WasmAwtRasterizerExports exports, int surfaceId,
                       int width, int height, int pixelFormat) {
        this.exports = exports;
        this.surfaceId = surfaceId;
        this.memoryBuffer = exports.getMemory().getBuffer();

        int rc = exports.resetSurface(surfaceId, width, height, pixelFormat);
        if (rc != 0) {
            throw new IllegalStateException("resetSurface failed: " + rc);
        }
    }

    public int getPixelsPtr() {
        return exports.getSurfacePixelsPtr(surfaceId);
    }

    public int getWidth() {
        return exports.getSurfaceWidth(surfaceId);
    }

    public int getHeight() {
        return exports.getSurfaceHeight(surfaceId);
    }

    public int getStride() {
        return exports.getSurfaceStride(surfaceId);
    }

    public Int32Array getPixelsView() {

        if (surfaceId == -1) {
            throw new IllegalStateException("Surface has been destroyed");
        }

        int ptr = getPixelsPtr();
        int stride = getStride();
        int height = getHeight();

        int lengthInInts = (stride / 4) * height;

        // pointer is in 32-bit “bytes” space, so >> 2 to get Int32Array index
        int byteOffset = ptr;
        return new Int32Array(memoryBuffer, byteOffset, lengthInInts);
    }

    public void renderCommands(int cmdPtr, int cmdCount) {
        int rc = exports.renderAwt(surfaceId, cmdPtr, cmdCount);
        if (rc != 0) {
            throw new IllegalStateException("render_awt failed: " + rc);
        }
    }

    public void destroy() {
        if (surfaceId != -1) {
            // surfaces with a width & height of 0 are considered free
            exports.resetSurface(this.surfaceId, 0, 0, 0);
            surfaceId = -1;

        }
    }
}
