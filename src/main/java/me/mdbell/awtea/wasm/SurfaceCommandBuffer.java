package me.mdbell.awtea.wasm;

import lombok.Getter;
import me.mdbell.awtea.classlib.java.awt.awtea.gfx.TSurfaceCommand;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int32Array;
import org.teavm.jso.typedarrays.Uint8Array;

public final class SurfaceCommandBuffer {
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

    public SurfaceCommandBuffer(WasmAwtRasterizerExports exports,
                                int maxCommands) {
        this.exports = exports;
        this.memoryBuffer = exports.getMemory().getBuffer();
        this.u8 = new Uint8Array(memoryBuffer);
        this.i32 = new Int32Array(memoryBuffer);

        this.commandSize = exports.getSurfaceCommandSize();
        this.maxCommands = maxCommands;

        int totalBytes = commandSize * maxCommands;
        int words = (totalBytes + 3) / 4; // round up to 32-bit words

        // abuse alloc_pixels as a generic malloc: width = words, height = 1
        this.basePtr = exports.allocPixels(words, 1);
        if (basePtr == 0) {
            throw new IllegalStateException("Failed to allocate command buffer");
        }
        this.count = 0;
    }

    public void reset() {
        count = 0;
    }

    public void free() {
        exports.freePixels(basePtr);
    }

    private int ensureSlot() {
        if (count >= maxCommands) {
            throw new IllegalStateException("Command buffer overflow: " + count + " / " + maxCommands);
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
        int idx = ensureSlot();
        int baseByte = cmdBaseByte(idx);
        int wordBase = cmdWordBase(baseByte);

        // operation byte
        setOperation(baseByte, TSurfaceCommand.Operation.SET_COLOR);

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
        int idx = ensureSlot();
        int baseByte = cmdBaseByte(idx);
        int wordBase = cmdWordBase(baseByte);

        setOperation(baseByte, TSurfaceCommand.Operation.FILL_RECT);

        i32.set(wordBase + 1, x);
        i32.set(wordBase + 2, y);
        i32.set(wordBase + 3, w);
        i32.set(wordBase + 4, h);

        // args unused
        i32.set(wordBase + 5, 0);
        i32.set(wordBase + 6, 0);
    }

    public void emitDrawRect(int x, int y, int w, int h) {
        int idx = ensureSlot();
        int baseByte = cmdBaseByte(idx);
        int wordBase = cmdWordBase(baseByte);

        setOperation(baseByte, TSurfaceCommand.Operation.DRAW_RECT);

        i32.set(wordBase + 1, x);
        i32.set(wordBase + 2, y);
        i32.set(wordBase + 3, w);
        i32.set(wordBase + 4, h);

        i32.set(wordBase + 5, 0);
        i32.set(wordBase + 6, 0);
    }

    public void emitClearRect(int x, int y, int w, int h) {
        int idx = ensureSlot();
        int baseByte = cmdBaseByte(idx);
        int wordBase = cmdWordBase(baseByte);

        setOperation(baseByte, TSurfaceCommand.Operation.CLEAR_RECT);

        i32.set(wordBase + 1, x);
        i32.set(wordBase + 2, y);
        i32.set(wordBase + 3, w);
        i32.set(wordBase + 4, h);

        i32.set(wordBase + 5, 0);
        i32.set(wordBase + 6, 0);
    }

    public void emitSetClipRect(int x, int y, int w, int h) {
        int idx = ensureSlot();
        int baseByte = cmdBaseByte(idx);
        int wordBase = cmdWordBase(baseByte);

        setOperation(baseByte, TSurfaceCommand.Operation.SET_CLIP_RECT);

        i32.set(wordBase + 1, x);
        i32.set(wordBase + 2, y);
        i32.set(wordBase + 3, w);
        i32.set(wordBase + 4, h);

        i32.set(wordBase + 5, 0);
        i32.set(wordBase + 6, 0);
    }

    public void emitBlitImage(int imageId, int x, int y) {
        int idx = ensureSlot();
        int baseByte = cmdBaseByte(idx);
        int wordBase = cmdWordBase(baseByte);

        setOperation(baseByte, TSurfaceCommand.Operation.BLIT_IMAGE);

        i32.set(wordBase + 1, x);
        i32.set(wordBase + 2, y);
        i32.set(wordBase + 3, 0);
        i32.set(wordBase + 4, 0);
        i32.set(wordBase + 5, imageId);
        i32.set(wordBase + 6, 0);
    }

    //TODO: other transforms
    public void emitSetTransform(
            float m00, float m01, float m02,
            float m10, float m11, float m12) {
        int idx = ensureSlot();
        int baseByte = cmdBaseByte(idx);
        int wordBase = cmdWordBase(baseByte);

        // operation
        setOperation(baseByte, TSurfaceCommand.Operation.SET_TRANSFORM);

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

    public void emitNoOp() {
        int idx = ensureSlot();
        int baseByte = cmdBaseByte(idx);
        int wordBase = cmdWordBase(baseByte);

        setOperation(baseByte, TSurfaceCommand.Operation.NO_OP);
        i32.set(wordBase + 1, 0);
        i32.set(wordBase + 2, 0);
        i32.set(wordBase + 3, 0);
        i32.set(wordBase + 4, 0);

        i32.set(wordBase + 5, 0);
        i32.set(wordBase + 6, 0);
    }

    private void setOperation(int byteIndex, TSurfaceCommand.Operation op) {
        u8.set(byteIndex, (short) op.ordinal());
    }

    public void emitDrawSurface(WasmSurface surface, int imgX, int imgY) {
        emitBlitImage(surface.getId(), imgX, imgY);
    }
}
