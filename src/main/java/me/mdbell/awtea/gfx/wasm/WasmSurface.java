package me.mdbell.awtea.gfx.wasm;

import lombok.Getter;
import me.mdbell.awtea.gfx.Rasterizer;
import me.mdbell.awtea.gfx.Surface;
import org.teavm.jso.canvas.ImageData;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int32Array;
import org.teavm.jso.typedarrays.Uint8ClampedArray;

public final class WasmSurface implements Surface {
    private final WasmAwtRasterizerExports exports;
    private int surfaceId;
    private final ArrayBuffer memoryBuffer;
    //    private final int layer = 0; // presently unused
    private final int pixelFormat;

    // Note: commands are 8 * 4 bytes each (see TSurfaceCommand), so 1024 commands = 32KB
    //       512 = 16KB
    private static final int MAX_COMMANDS = 512;

    @Getter
    private int pixelsPtr = 0;
    private int width = 0;
    private int height = 0;
    @Getter
    private int stride = 0;

    private Uint8ClampedArray pixelsView = null;

    public WasmSurface(WasmAwtRasterizerExports exports, int surfaceId,
                       int width, int height, int pixelFormat) {
        this.exports = exports;
        this.surfaceId = surfaceId;
        this.pixelFormat = pixelFormat;
        this.memoryBuffer = exports.getMemory().getBuffer();

        // layer is presently unused, so set to 0 - future versions may use it
        resize(width, height);
    }

    public SurfaceCommandBuffer createBuffer() {
        return createBuffer(1024);
    }

    public SurfaceCommandBuffer createBuffer(int maxCommands) {
        //TODO: pass this instead of surfaceId, so we can prevent the buffer
        // from being used after the surface is destroyed
        return new SurfaceCommandBuffer(this.surfaceId, exports, maxCommands);
    }

    @Override
    public Rasterizer createRasterizer() {
        return new WasmRasterizer(this);
    }

    @Override
    public void resize(int width, int height) {
        if (surfaceId == -1) {
            throw new IllegalStateException("Surface has been destroyed");
        }

        int rc = exports.resetSurface(surfaceId, 0, width, height, pixelFormat);
        if (rc != 0) {
            throw new IllegalStateException("resetSurface failed: " + rc);
        }

        // we want to prevent as many calls into wasm as possible, so cache these values
        // instead of retrieving them each time
        // TODO: profile, see if this is worthwhile

        this.width = exports.getSurfaceWidth(this.surfaceId);
        this.height = exports.getSurfaceHeight(this.surfaceId);
        this.pixelsPtr = exports.getSurfacePixelsPtr(this.surfaceId);
        this.stride = exports.getSurfaceStride(this.surfaceId);

        pixelsView = new Uint8ClampedArray(this.memoryBuffer, this.pixelsPtr, this.stride * height);
    }

    @Override
    public int getWidth() {
        if (surfaceId == -1) {
            return 0;
        }
        return width;
    }

    @Override
    public int getHeight() {
        if (surfaceId == -1) {
            return 0;
        }
        return height;
    }

    @Override
    public Uint8ClampedArray getPixelData() {
        if (surfaceId == -1) {
            throw new IllegalStateException("Surface has been destroyed");
        }
        return pixelsView;
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
        return new Int32Array(memoryBuffer, ptr, lengthInInts);
    }

    public ImageData asImageData() {
        int width = getWidth();
        int height = getHeight();
        int stride = getStride();
        int pixelsPtr = getPixelsPtr();

        // Create ImageData
        Uint8ClampedArray wasmPixels = new Uint8ClampedArray(memoryBuffer, pixelsPtr, stride * height);

        return new ImageData(wasmPixels, width, height);
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
            exports.resetSurface(this.surfaceId, 0, 0, 0, 0);
            surfaceId = -1;

        }
    }

    public int getId() {
        return surfaceId;
    }
}
