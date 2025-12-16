package me.mdbell.awtea.util;

import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.DataView;
import org.teavm.jso.typedarrays.Uint8Array;

/**
 * Helper for writing variable-length commands to a byte buffer.
 * 
 * <p>Commands are written in the format:
 * <pre>
 * [opcode: uint8][flags: uint8][length: uint16][data: length*4 bytes]
 * </pre>
 * 
 * <p>The length field is in words (4-byte units) and does NOT include the 4-byte header.
 * All multi-byte values are written in little-endian byte order.
 * 
 * <p>Usage:
 * <pre>
 * ByteWriter writer = new ByteWriter(memoryBuffer, bufferPtr, bufferSizeWords);
 * writer.beginCommand(opcode, flags);
 * writer.writeInt32(x);
 * writer.writeInt32(y);
 * writer.finishCommand();
 * </pre>
 */
public class ByteWriter {
    
    private final DataView view;
    private final int basePtr;
    private final int maxBytes;
    
    private int position;           // Current byte position (absolute)
    private int commandStartPos;    // Start of current command (absolute)
    private boolean inCommand;      // Whether we're currently writing a command
    
    /**
     * Create a ByteWriter for a WASM memory buffer.
     * 
     * @param memoryBuffer The WASM memory buffer
     * @param basePtr The byte offset into the buffer where commands start
     * @param maxWords Maximum size of the buffer in words (4-byte units)
     */
    public ByteWriter(ArrayBuffer memoryBuffer, int basePtr, int maxWords) {
        this.view = new DataView(memoryBuffer);
        this.basePtr = basePtr;
        this.maxBytes = maxWords * 4;
        this.position = basePtr;
        this.commandStartPos = -1;
        this.inCommand = false;
    }
    
    /**
     * Begin writing a new command.
     * 
     * <p>If a previous command was started but not finished, it will be automatically finished.
     * 
     * @param opcode The command opcode (0-255)
     * @param flags Command flags (0-255). Bit 0 reserved for CMD_FLAG_EXTENDED.
     */
    public void beginCommand(int opcode, int flags) {
        // Auto-finish previous command if needed
        if (inCommand) {
            finishCommand();
        }
        
        // Ensure we have space for header (4 bytes)
        if (position + 4 > basePtr + maxBytes) {
            throw new IllegalStateException("Buffer overflow: cannot write command header");
        }
        
        commandStartPos = position;
        
        // Write header with placeholder length
        view.setUint8(position, (short) (opcode & 0xFF));
        view.setUint8(position + 1, (short) (flags & 0xFF));
        view.setUint16(position + 2, (short) 0, true); // Placeholder length (little-endian)
        
        position += 4;
        inCommand = true;
    }
    
    /**
     * Finish the current command by back-patching the length field.
     * 
     * <p>The length is calculated as the number of words (4-byte units) written
     * since the command header, excluding the header itself.
     */
    public void finishCommand() {
        if (!inCommand) {
            throw new IllegalStateException("No command in progress");
        }
        
        // Calculate data length in bytes (excluding header)
        int dataBytes = position - commandStartPos - 4;
        
        // Length must be a multiple of 4 (word-aligned)
        if (dataBytes % 4 != 0) {
            throw new IllegalStateException("Command data not word-aligned: " + dataBytes + " bytes");
        }
        
        int lengthWords = dataBytes / 4;
        
        // Back-patch the length field
        view.setUint16(commandStartPos + 2, (short) lengthWords, true);
        
        inCommand = false;
        commandStartPos = -1;
    }
    
    /**
     * Write a signed 8-bit integer.
     */
    public void writeInt8(int value) {
        ensureInCommand();
        ensureSpace(1);
        view.setInt8(position, (byte) value);
        position += 1;
    }
    
    /**
     * Write an unsigned 8-bit integer.
     */
    public void writeUInt8(int value) {
        ensureInCommand();
        ensureSpace(1);
        view.setUint8(position, (short) (value & 0xFF));
        position += 1;
    }
    
    /**
     * Write a signed 16-bit integer (little-endian).
     */
    public void writeInt16(int value) {
        ensureInCommand();
        ensureSpace(2);
        view.setInt16(position, (short) value, true);
        position += 2;
    }
    
    /**
     * Write an unsigned 16-bit integer (little-endian).
     */
    public void writeUInt16(int value) {
        ensureInCommand();
        ensureSpace(2);
        view.setUint16(position, (short) (value & 0xFFFF), true);
        position += 2;
    }
    
    /**
     * Write a signed 32-bit integer (little-endian).
     */
    public void writeInt32(int value) {
        ensureInCommand();
        ensureSpace(4);
        view.setInt32(position, value, true);
        position += 4;
    }
    
    /**
     * Write a 32-bit floating point value (little-endian).
     */
    public void writeFloat(float value) {
        ensureInCommand();
        ensureSpace(4);
        view.setFloat32(position, value, true);
        position += 4;
    }
    
    /**
     * Write a signed 64-bit integer (little-endian).
     * Note: JavaScript doesn't have true 64-bit integers, so this uses two 32-bit writes.
     */
    public void writeInt64(long value) {
        ensureInCommand();
        ensureSpace(8);
        // Split into low and high 32 bits
        int low = (int) (value & 0xFFFFFFFFL);
        int high = (int) (value >>> 32);
        view.setInt32(position, low, true);
        view.setInt32(position + 4, high, true);
        position += 8;
    }
    
    /**
     * Get the current position (byte offset from start of buffer).
     */
    public int getPosition() {
        return position - basePtr;
    }
    
    /**
     * Get the number of bytes written.
     */
    public int getBytesUsed() {
        return position - basePtr;
    }
    
    /**
     * Reset the writer position to the start of the buffer.
     * Any in-progress command is discarded.
     */
    public void reset() {
        position = basePtr;
        commandStartPos = -1;
        inCommand = false;
    }
    
    private void ensureInCommand() {
        if (!inCommand) {
            throw new IllegalStateException("No command in progress. Call beginCommand() first.");
        }
    }
    
    private void ensureSpace(int bytes) {
        if (position + bytes > basePtr + maxBytes) {
            throw new IllegalStateException("Buffer overflow: need " + bytes + " bytes, " +
                    (basePtr + maxBytes - position) + " available");
        }
    }
}
