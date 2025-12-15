package me.mdbell.awtea.gfx.wasm;

import lombok.Getter;
import lombok.Setter;
import me.mdbell.awtea.gfx.generated.Operation;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int32Array;
import org.teavm.jso.typedarrays.Uint8Array;

public final class SurfaceCommandBuffer {

    private static final Logger log = LoggerFactory.getLogger(SurfaceCommandBuffer.class);

    private final WasmAwtRasterizerExports exports;
    private final ArrayBuffer memoryBuffer;
    private final Uint8Array u8;
    private final Int32Array i32;
    @Getter
    private final int basePtr;         // wasm pointer to first command
    private final int commandSize;     // bytes per SurfaceCommand
    @Getter
    private final int maxCommands;
    @Getter
    private int count;

    @Setter
    private int contextId;

    SurfaceCommandBuffer(WasmAwtRasterizerExports exports,
                         int maxCommands) {
        this(-1, exports, maxCommands);
    }

    SurfaceCommandBuffer(int contextId, WasmAwtRasterizerExports exports,
                         int maxCommands) {
        this.contextId = contextId;
        this.exports = exports;
        this.memoryBuffer = exports.getMemory().getBuffer();
        this.u8 = new Uint8Array(memoryBuffer);
        this.i32 = new Int32Array(memoryBuffer);

        this.commandSize = exports.getSurfaceCommandSize();
        
        // If contextId is provided, get buffer from context instead of allocating
        if (contextId != -1) {
            this.basePtr = exports.getContextCommandBufferPtr(contextId);
            this.maxCommands = exports.getMaxContextCommands();
            
            if (basePtr == 0) {
                throw new IllegalStateException("Failed to get context command buffer");
            }
        } else {
            // Legacy path: allocate a temporary buffer
            this.maxCommands = maxCommands;
            this.basePtr = exports.requestCommandBuffer(maxCommands);
            if (basePtr == 0) {
                throw new IllegalStateException("Failed to allocate command buffer");
            }
        }
        
        this.count = 0;
    }

    public void reset() {
        log.trace("SurfaceCommandBuffer.reset: Resetting command buffer at ptr {}", basePtr);
        count = 0;
    }

    public void free() {
        // Only free if this is a legacy allocated buffer (contextId == -1)
        if (contextId == -1) {
            log.trace("SurfaceCommandBuffer.free: Freeing command buffer at ptr {}", basePtr);
            exports.freePixels(basePtr);
        } else {
            log.trace("SurfaceCommandBuffer.free: Skipping free for context-owned buffer (context={})", contextId);
            // Context-owned buffer is freed when the context is destroyed
        }
    }

    public void flush() {
        log.trace("SurfaceCommandBuffer.flush: Flushing {} commands from buffer at ptr {} to context {}",
                count, basePtr, contextId);
        if (contextId == -1) {
            throw new IllegalStateException("Cannot flush command buffer without associated context");
        }
        if (count == 0) {
            return; // nothing to do
        }
        int rc = exports.renderAwt(contextId, basePtr, count);
        if (rc == 0) {
            reset();
        } else {
            log.error("SurfaceCommandBuffer.flush: renderAwt failed: {}", rc);
        }
    }

    private int ensureSlot() {
        if (count >= maxCommands) {
            if (contextId == -1) {
                throw new IllegalStateException("Command buffer overflow: " + count + " / " + maxCommands);
            } else {
                // it's not ideal to flush on a non-frame boundary, but
                // it's better than raising an error.
                flush();
            }
        }
        int index = count;
        count++;
        return index;
    }

    private int cmdBaseByte(int index) {
        return basePtr + index * commandSize;
    }

    private int cmdWordBase(int baseByte) {
        return baseByte >> 2; // byte offset -> int index (wasm memory is little-endian)
    }

    // ---- helpers for specific commands ----

    public void emitSetColor(int argb, int which) {
        log.trace("SurfaceCommandBuffer.emitSetColor: argb=0x{}, which={}",
                Integer.toHexString(argb), which);
        int idx = ensureSlot();
        int baseByte = cmdBaseByte(idx);
        int wordBase = cmdWordBase(baseByte);

        // operation byte
        setOperation(baseByte, Operation.SET_COLOR);

        // x,y,width,height unused here; leave zero
        i32.set(wordBase + 1, 0); // x
        i32.set(wordBase + 2, 0); // y
        i32.set(wordBase + 3, 0); // width
        i32.set(wordBase + 4, 0); // height

        // union.set_color.argb / which
        i32.set(wordBase + 5, argb); // argb
        i32.set(wordBase + 6, which); // which index (fg/bg)
    }

    public void emitFillRect(int x, int y, int w, int h) {
        log.trace("SurfaceCommandBuffer.emitFillRect: x={}, y={}, w={}, h={}",
                x, y, w, h);
        int idx = ensureSlot();
        int baseByte = cmdBaseByte(idx);
        int wordBase = cmdWordBase(baseByte);

        setOperation(baseByte, Operation.FILL_RECT);

        i32.set(wordBase + 1, x);
        i32.set(wordBase + 2, y);
        i32.set(wordBase + 3, w);
        i32.set(wordBase + 4, h);

        // args unused
        i32.set(wordBase + 5, 0);
        i32.set(wordBase + 6, 0);
    }

    public void emitDrawRect(int x, int y, int w, int h) {
        log.trace("SurfaceCommandBuffer.emitDrawRect: x={}, y={}, w={}, h={}",
                x, y, w, h);
        int idx = ensureSlot();
        int baseByte = cmdBaseByte(idx);
        int wordBase = cmdWordBase(baseByte);

        setOperation(baseByte, Operation.DRAW_RECT);

        i32.set(wordBase + 1, x);
        i32.set(wordBase + 2, y);
        i32.set(wordBase + 3, w);
        i32.set(wordBase + 4, h);

        i32.set(wordBase + 5, 0);
        i32.set(wordBase + 6, 0);
    }

    public void emitClearRect(int x, int y, int w, int h) {
        log.trace("SurfaceCommandBuffer.emitClearRect: x={}, y={}, w={}, h={}",
                x, y, w, h);
        int idx = ensureSlot();
        int baseByte = cmdBaseByte(idx);
        int wordBase = cmdWordBase(baseByte);

        setOperation(baseByte, Operation.CLEAR_RECT);

        i32.set(wordBase + 1, x);
        i32.set(wordBase + 2, y);
        i32.set(wordBase + 3, w);
        i32.set(wordBase + 4, h);

        i32.set(wordBase + 5, 0);
        i32.set(wordBase + 6, 0);
    }

    public void emitSetClipRect(int x, int y, int w, int h) {
        log.trace("SurfaceCommandBuffer.emitSetClipRect: x={}, y={}, w={}, h={}",
                x, y, w, h);
        int idx = ensureSlot();
        int baseByte = cmdBaseByte(idx);
        int wordBase = cmdWordBase(baseByte);

        setOperation(baseByte, Operation.SET_CLIP_RECT);

        i32.set(wordBase + 1, x);
        i32.set(wordBase + 2, y);
        i32.set(wordBase + 3, w);
        i32.set(wordBase + 4, h);

        i32.set(wordBase + 5, 0);
        i32.set(wordBase + 6, 0);
    }

    public void emitBlitImage(int surfaceId, int x, int y) {
        log.trace("SurfaceCommandBuffer.emitBlitImage: surfaceId={}, x={}, y={}",
                surfaceId, x, y);
        int idx = ensureSlot();
        int baseByte = cmdBaseByte(idx);
        int wordBase = cmdWordBase(baseByte);

        setOperation(baseByte, Operation.BLIT_IMAGE);

        i32.set(wordBase + 1, x);
        i32.set(wordBase + 2, y);
        i32.set(wordBase + 3, 0);
        i32.set(wordBase + 4, 0);
        i32.set(wordBase + 5, surfaceId);
        i32.set(wordBase + 6, 0);
    }

    public void emitSetTransform(
            float m00, float m10, float m01,
            float m11, float m02, float m12) {
        log.trace("SurfaceCommandBuffer.emitSetTransform: [" +
                        "{}, {}, {}}, " +
                        "{{}, {}, {}}]",
                m00, m01, m02,
                m10, m11, m12);
        int idx = ensureSlot();
        int baseByte = cmdBaseByte(idx);
        int wordBase = cmdWordBase(baseByte);

        // operation
        setOperation(baseByte, Operation.SET_TRANSFORM);

        // reinterpret float->uint32
        int i00 = Float.floatToIntBits(m00);
        int i01 = Float.floatToIntBits(m01);
        int i02 = Float.floatToIntBits(m02);
        int i10 = Float.floatToIntBits(m10);
        int i11 = Float.floatToIntBits(m11);
        int i12 = Float.floatToIntBits(m12);

        i32.set(wordBase + 1, i00); // x
        i32.set(wordBase + 2, i01); // y
        i32.set(wordBase + 3, i02); // width
        i32.set(wordBase + 4, i10); // height
        i32.set(wordBase + 5, i11); // args[0]
        i32.set(wordBase + 6, i12); // args[1]
    }

    public void emitDrawLine(int x0, int y0, int x1, int y1) {
        log.trace("SurfaceCommandBuffer.emitDrawLine: x0={}, y0={}, x1={}, y1={}",
                x0, y0, x1, y1);
        int idx = ensureSlot();
        int baseByte = cmdBaseByte(idx);
        int wordBase = cmdWordBase(baseByte);

        setOperation(baseByte, Operation.DRAW_LINE);

        i32.set(wordBase + 1, x0);
        i32.set(wordBase + 2, y0);
        i32.set(wordBase + 3, x1);
        i32.set(wordBase + 4, y1);

        i32.set(wordBase + 5, 0);
        i32.set(wordBase + 6, 0);
    }

    public void emitSetComposite(int compositeMode, float alpha) {
        log.trace("SurfaceCommandBuffer.emitSetComposite: mode={}, alpha={}",
                compositeMode, alpha);
        int idx = ensureSlot();
        int baseByte = cmdBaseByte(idx);
        int wordBase = cmdWordBase(baseByte);

        setOperation(baseByte, Operation.SET_COMPOSITE);

        // x, y, width, height unused for SET_COMPOSITE
        i32.set(wordBase + 1, 0);
        i32.set(wordBase + 2, 0);
        i32.set(wordBase + 3, 0);
        i32.set(wordBase + 4, 0);

        // union.set_composite.mode and alpha
        i32.set(wordBase + 5, compositeMode);
        i32.set(wordBase + 6, Float.floatToIntBits(alpha));
    }

    private void setOperation(int byteIndex, Operation op) {
        u8.set(byteIndex, (short) op.ordinal());
    }

    public void emitDrawSurface(WasmSurface surface, int imgX, int imgY) {
        emitBlitImage(surface.getId(), imgX, imgY);
    }
}
