package me.mdbell.awtea.gfx.wasm;

import lombok.Getter;
import lombok.Setter;
import me.mdbell.awtea.gfx.generated.Operation;
import me.mdbell.awtea.util.ByteWriter;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.typedarrays.ArrayBuffer;

/**
 * Command buffer for building variable-length rendering commands.
 * 
 * <p>Commands are written in the format:
 * <pre>
 * [opcode: uint8][flags: uint8][length: uint16][data: length*4 bytes]
 * </pre>
 * 
 * <p>This class manages the context's internal command buffer and provides
 * high-level methods for emitting graphics commands.
 */
public final class SurfaceCommandBuffer {

    private static final Logger log = LoggerFactory.getLogger(SurfaceCommandBuffer.class);
    
    // Command flags
    private static final int CMD_FLAG_EXTENDED = 0x01; // Reserved for future use

    private final WasmAwtRasterizerExports exports;
    private final ByteWriter writer;
    @Getter
    private final int basePtr;
    @Getter
    private final int maxBytes;

    @Setter
    private int contextId;

    /**
     * Create a command buffer for a context.
     * 
     * @param contextId The context ID (must be valid)
     * @param exports The WASM exports
     */
    SurfaceCommandBuffer(int contextId, WasmAwtRasterizerExports exports) {
        if (contextId < 0) {
            throw new IllegalArgumentException("Invalid context ID: " + contextId);
        }
        
        this.contextId = contextId;
        this.exports = exports;
        
        // Query buffer size and pointer from WASM
        int bufferSizeWords = exports.getContextBufferSizeWords();
        this.basePtr = exports.getContextBufferPtr(contextId);
        this.maxBytes = bufferSizeWords * 4;
        
        if (basePtr == 0) {
            throw new IllegalStateException("Failed to get context command buffer for context " + contextId);
        }
        
        ArrayBuffer memoryBuffer = exports.getMemory().getBuffer();
        this.writer = new ByteWriter(memoryBuffer, basePtr, bufferSizeWords);
        
        log.trace("SurfaceCommandBuffer created: context={}, basePtr={}, maxBytes={}",
                contextId, basePtr, maxBytes);
    }

    /**
     * Reset the command buffer, discarding any commands.
     */
    public void reset() {
        log.trace("SurfaceCommandBuffer.reset: context={}", contextId);
        writer.reset();
    }

    /**
     * Flush the command buffer by sending all commands to the WASM renderer.
     * After flushing, the buffer is automatically reset.
     */
    public void flush() {
        // Finish any in-progress command
        try {
            writer.finishCommand();
        } catch (IllegalStateException e) {
            // No command in progress, that's fine
        }
        
        int bytesUsed = writer.getBytesUsed();
        log.trace("SurfaceCommandBuffer.flush: context={}, bytesUsed={}", contextId, bytesUsed);
        
        if (bytesUsed == 0) {
            return; // Nothing to flush
        }
        
        // Pass cmdPtr=0 to use context's internal buffer, bytesUsed in bytes
        int rc = exports.renderAwt(contextId, 0, bytesUsed);
        if (rc == 0) {
            reset();
        } else {
            log.error("SurfaceCommandBuffer.flush: renderAwt failed with code {}", rc);
        }
    }

    // ---- Command emission methods ----

    public void emitSetColor(int argb, int which) {
        log.trace("SurfaceCommandBuffer.emitSetColor: argb=0x{}, which={}",
                Integer.toHexString(argb), which);
        
        writer.beginCommand(Operation.SET_COLOR.ordinal(), 0);
        writer.writeInt32(argb);
        writer.writeInt32(which);
        writer.finishCommand();
    }

    public void emitFillRect(int x, int y, int w, int h) {
        log.trace("SurfaceCommandBuffer.emitFillRect: x={}, y={}, w={}, h={}",
                x, y, w, h);
        
        writer.beginCommand(Operation.FILL_RECT.ordinal(), 0);
        writer.writeInt32(x);
        writer.writeInt32(y);
        writer.writeInt32(w);
        writer.writeInt32(h);
        writer.finishCommand();
    }

    public void emitDrawRect(int x, int y, int w, int h) {
        log.trace("SurfaceCommandBuffer.emitDrawRect: x={}, y={}, w={}, h={}",
                x, y, w, h);
        
        writer.beginCommand(Operation.DRAW_RECT.ordinal(), 0);
        writer.writeInt32(x);
        writer.writeInt32(y);
        writer.writeInt32(w);
        writer.writeInt32(h);
        writer.finishCommand();
    }

    public void emitClearRect(int x, int y, int w, int h) {
        log.trace("SurfaceCommandBuffer.emitClearRect: x={}, y={}, w={}, h={}",
                x, y, w, h);
        
        writer.beginCommand(Operation.CLEAR_RECT.ordinal(), 0);
        writer.writeInt32(x);
        writer.writeInt32(y);
        writer.writeInt32(w);
        writer.writeInt32(h);
        writer.finishCommand();
    }

    public void emitSetClipRect(int x, int y, int w, int h) {
        log.trace("SurfaceCommandBuffer.emitSetClipRect: x={}, y={}, w={}, h={}",
                x, y, w, h);
        
        writer.beginCommand(Operation.SET_CLIP_RECT.ordinal(), 0);
        writer.writeInt32(x);
        writer.writeInt32(y);
        writer.writeInt32(w);
        writer.writeInt32(h);
        writer.finishCommand();
    }

    public void emitBlitImage(int surfaceId, int x, int y) {
        log.trace("SurfaceCommandBuffer.emitBlitImage: surfaceId={}, x={}, y={}",
                surfaceId, x, y);
        
        writer.beginCommand(Operation.BLIT_IMAGE.ordinal(), 0);
        writer.writeInt32(surfaceId);
        writer.writeInt32(x);
        writer.writeInt32(y);
        writer.finishCommand();
    }

    public void emitSetTransform(
            float m00, float m10, float m01,
            float m11, float m02, float m12) {
        log.trace("SurfaceCommandBuffer.emitSetTransform: [" +
                        "{}, {}, {}}, " +
                        "{{}, {}, {}}]",
                m00, m01, m02,
                m10, m11, m12);
        
        writer.beginCommand(Operation.SET_TRANSFORM.ordinal(), 0);
        writer.writeFloat(m00);
        writer.writeFloat(m01);
        writer.writeFloat(m02);
        writer.writeFloat(m10);
        writer.writeFloat(m11);
        writer.writeFloat(m12);
        writer.finishCommand();
    }

    public void emitDrawLine(int x0, int y0, int x1, int y1) {
        log.trace("SurfaceCommandBuffer.emitDrawLine: x0={}, y0={}, x1={}, y1={}",
                x0, y0, x1, y1);
        
        writer.beginCommand(Operation.DRAW_LINE.ordinal(), 0);
        writer.writeInt32(x0);
        writer.writeInt32(y0);
        writer.writeInt32(x1);
        writer.writeInt32(y1);
        writer.finishCommand();
    }

    public void emitDrawSurface(WasmSurface surface, int imgX, int imgY) {
        emitBlitImage(surface.getId(), imgX, imgY);
    }
}
