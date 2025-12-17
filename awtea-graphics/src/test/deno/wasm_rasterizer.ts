/**
 * WASM Rasterizer Test Harness
 *
 * This module provides a TypeScript interface for testing the AWT rasterizer
 * WASM module in isolation, without the full awtea stack (Java GUI, TSurface, etc).
 *
 * It includes utilities for:
 * - Loading the WASM module
 * - Allocating and managing surfaces
 * - Creating and sending drawing commands
 * - Inspecting pixel buffers
 * - Error handling and validation
 */

// Import auto-generated enums
// Note: Edit schemas/*.yaml files to modify these enums
import { PixelFormat } from "./generated/pixel-format.ts";
import { SurfaceOperation } from "./generated/surface-operation.ts";
import { CompositeMode } from "./generated/composite-mode.ts";

// Color slot constants
export const COLOR_FG = 0;
export const COLOR_BG = 1;

// Command flags
export const CMD_FLAG_EXTENDED = 0x01; // Bit 0: Extended command set (future use)

// Re-export for convenience
export { CompositeMode, PixelFormat, SurfaceOperation };

/**
 * ByteWriter for writing variable-length commands
 *
 * Commands are written in the format:
 * [opcode: uint8][flags: uint8][length: uint16][data: length*4 bytes]
 *
 * The length field is in words (4-byte units) and does NOT include the 4-byte header.
 */
class ByteWriter {
  private view: DataView;
  private basePtr: number;
  private maxBytes: number;
  private position: number;
  private commandStartPos: number;
  private inCommand: boolean;

  constructor(memory: WebAssembly.Memory, basePtr: number, maxWords: number) {
    this.view = new DataView(memory.buffer);
    this.basePtr = basePtr;
    this.maxBytes = maxWords * 4;
    this.position = basePtr;
    this.commandStartPos = -1;
    this.inCommand = false;
  }

  beginCommand(opcode: number, flags: number = 0): void {
    // Auto-finish previous command if needed
    if (this.inCommand) {
      this.finishCommand();
    }

    // Ensure space for header (4 bytes)
    if (this.position + 4 > this.basePtr + this.maxBytes) {
      throw new Error("Buffer overflow: cannot write command header");
    }

    this.commandStartPos = this.position;

    // Write header with placeholder length
    this.view.setUint8(this.position, opcode & 0xFF);
    this.view.setUint8(this.position + 1, flags & 0xFF);
    this.view.setUint16(this.position + 2, 0, true); // Placeholder length (little-endian)

    this.position += 4;
    this.inCommand = true;
  }

  finishCommand(): void {
    if (!this.inCommand) {
      throw new Error("No command in progress");
    }

    // Calculate data length in bytes (excluding header)
    const dataBytes = this.position - this.commandStartPos - 4;

    // Length must be word-aligned
    if (dataBytes % 4 !== 0) {
      throw new Error(`Command data not word-aligned: ${dataBytes} bytes`);
    }

    const lengthWords = dataBytes / 4;

    // Back-patch the length field
    this.view.setUint16(this.commandStartPos + 2, lengthWords, true);

    this.inCommand = false;
    this.commandStartPos = -1;
  }

  writeInt32(value: number): void {
    this.ensureInCommand();
    this.ensureSpace(4);
    this.view.setInt32(this.position, value, true);
    this.position += 4;
  }

  writeFloat(value: number): void {
    this.ensureInCommand();
    this.ensureSpace(4);
    this.view.setFloat32(this.position, value, true);
    this.position += 4;
  }

  getBytesUsed(): number {
    return this.position - this.basePtr;
  }

  reset(): void {
    this.position = this.basePtr;
    this.commandStartPos = -1;
    this.inCommand = false;
  }

  private ensureInCommand(): void {
    if (!this.inCommand) {
      throw new Error("No command in progress. Call beginCommand() first.");
    }
  }

  private ensureSpace(bytes: number): void {
    if (this.position + bytes > this.basePtr + this.maxBytes) {
      throw new Error(
        `Buffer overflow: need ${bytes} bytes, ${
          this.basePtr + this.maxBytes - this.position
        } available`,
      );
    }
  }
}

/**
 * Main test harness class
 */
export class WasmRasterizer {
  private wasmInstance: WebAssembly.Instance | null = null;

  /**
   * Load and instantiate the WASM module
   */
  async load(wasmPath: string): Promise<void> {
    const wasmBytes = await Deno.readFile(wasmPath);
    const wasmModule = await WebAssembly.compile(wasmBytes);

    // Provide the logging callback that the WASM module expects
    const imports = {
      env: {
        wasm_log_callback: (
          level: number,
          messagePtr: number,
          messageLen: number,
        ) => {
          // Get the exports to access memory
          if (!this.wasmInstance) return;
          const memory = this.wasmInstance.exports.memory as WebAssembly.Memory;
          if (!memory) return;

          // Read the message from WASM memory
          const messageBytes = new Uint8Array(
            memory.buffer,
            messagePtr,
            messageLen,
          );
          const message = new TextDecoder().decode(messageBytes);

          // Log based on level (0=ERROR, 1=WARN, 2=INFO, 3=DEBUG, 4=TRACE)
          const levelNames = ["ERROR", "WARN", "INFO", "DEBUG", "TRACE"];
          const levelName = levelNames[level] || "UNKNOWN";
          console.log(`[WASM ${levelName}] ${message}`);
        },
        wasm_get_time_ms: (): number => {
          return performance.now();
        },
        wasm_report_memory_usage: (
          allocatedBytes: number,
          peakBytes: number,
        ) => {
          console.log(
            `[WASM MEMORY] allocated=${allocatedBytes} bytes, peak=${peakBytes} bytes`,
          );
        },
        wasm_assertion_failed: (
          exprPtr: number,
          exprLen: number,
          filePtr: number,
          fileLen: number,
          line: number,
        ) => {
          if (!this.wasmInstance) return;
          const memory = this.wasmInstance.exports.memory as WebAssembly.Memory;
          if (!memory) return;

          const exprBytes = new Uint8Array(memory.buffer, exprPtr, exprLen);
          const expr = new TextDecoder().decode(exprBytes);
          const fileBytes = new Uint8Array(memory.buffer, filePtr, fileLen);
          const file = new TextDecoder().decode(fileBytes);

          console.error(`[WASM ASSERTION] ${expr} failed at ${file}:${line}`);
        },
      },
    };

    this.wasmInstance = await WebAssembly.instantiate(wasmModule, imports);

    // Initialize the surface system (sets contexts to free state)
    const wasm = this.getExports();
    wasm.init_surface_system();
  }

  /**
   * Get WASM exports (throws if not loaded)
   */
  private getExports() {
    if (!this.wasmInstance) {
      throw new Error("WASM module not loaded. Call load() first.");
    }
    return this.wasmInstance.exports as any;
  }

  /**
   * Allocate a new surface
   */
  allocateSurface(
    width: number,
    height: number,
    format: PixelFormat = PixelFormat.PIXEL_FORMAT_ARGB,
  ): number {
    const wasm = this.getExports();
    const surfaceId = wasm.find_free_surface();
    if (surfaceId < 0) {
      throw new Error("No free surface slots available");
    }

    const result = wasm.reset_surface(surfaceId, 0, width, height, format);
    if (result !== 0) {
      throw new Error(`Failed to reset surface: error code ${result}`);
    }

    return surfaceId;
  }

  /**
   * Free a surface
   */
  freeSurface(surfaceId: number): void {
    const wasm = this.getExports();
    // Reset with width=0, height=0 to free
    wasm.reset_surface(surfaceId, 0, 0, 0, PixelFormat.PIXEL_FORMAT_ARGB);
  }

  /**
   * Get surface dimensions
   */
  getSurfaceDimensions(
    surfaceId: number,
  ): { width: number; height: number; stride: number } {
    const wasm = this.getExports();
    return {
      width: wasm.get_surface_width(surfaceId),
      height: wasm.get_surface_height(surfaceId),
      stride: wasm.get_surface_stride(surfaceId),
    };
  }

  /**
   * Get surface pixel buffer as Uint32Array
   */
  getSurfacePixels(surfaceId: number): Uint32Array {
    const wasm = this.getExports();
    const ptr = wasm.get_surface_pixels_ptr(surfaceId);
    if (ptr === 0) {
      throw new Error(
        `Invalid surface ID ${surfaceId} or surface not allocated`,
      );
    }

    const { width, height } = this.getSurfaceDimensions(surfaceId);
    const buffer = new Uint32Array(wasm.memory.buffer, ptr, width * height);

    return buffer;
  }

  /**
   * Get a copy of surface pixels (not a view into WASM memory)
   */
  copySurfacePixels(surfaceId: number): Uint32Array {
    const pixels = this.getSurfacePixels(surfaceId);
    return new Uint32Array(pixels);
  }

  /**
   * Write a variable-length command using ByteWriter.
   * This is a low-level helper for writing commands directly.
   *
   * @param contextId The context to write to
   * @param writer Callback that uses ByteWriter to write the command
   * @returns The number of bytes used
   */
  writeVariableLengthCommand(
    contextId: number,
    writer: (w: ByteWriter) => void,
  ): number {
    const wasm = this.getExports();
    const bufferPtr = wasm.get_context_buffer_ptr(contextId);
    const bufferSizeWords = wasm.get_context_buffer_size_words();

    const byteWriter = new ByteWriter(
      wasm.memory as WebAssembly.Memory,
      bufferPtr,
      bufferSizeWords,
    );
    byteWriter.reset();
    writer(byteWriter);

    return byteWriter.getBytesUsed();
  }

  /**
   * Execute commands on a surface (using context).
   * Commands are written using ByteWriter callbacks.
   *
   * @param contextId The context to render to
   * @param writers Array of ByteWriter callbacks
   */
  renderVariableLengthCommands(
    contextId: number,
    writers: Array<(w: ByteWriter) => void>,
  ): void {
    const wasm = this.getExports();
    const bufferPtr = wasm.get_context_buffer_ptr(contextId);
    const bufferSizeWords = wasm.get_context_buffer_size_words();

    const byteWriter = new ByteWriter(
      wasm.memory as WebAssembly.Memory,
      bufferPtr,
      bufferSizeWords,
    );
    byteWriter.reset();

    // Write all commands
    for (const writer of writers) {
      writer(byteWriter);
    }

    // Render with cmdPtr=0 to use context buffer
    const bytesUsed = byteWriter.getBytesUsed();
    const result = wasm.render_awt(contextId, 0, bytesUsed);
    if (result !== 0) {
      throw new Error(`render_awt failed with error code ${result}`);
    }
  }

  /**
   * Legacy method: Create a command buffer (deprecated)
   * @deprecated Use variable-length commands instead
   */
  createCommandBuffer(maxCommands: number): number {
    throw new Error(
      "Legacy command buffers not supported. Use variable-length commands.",
    );
  }

  /**
   * Legacy method: Write a command to the buffer (deprecated)
   * @deprecated Use writeVariableLengthCommand instead
   */
  writeCommand(bufferPtr: number, index: number, cmd: any): void {
    throw new Error(
      "Legacy command writing not supported. Use writeVariableLengthCommand.",
    );
  }

  /**
   * Legacy method: Execute commands on a surface (deprecated)
   * @deprecated Use renderVariableLengthCommands instead
   */
  renderCommands(
    contextId: number,
    bufferPtr: number,
    commandCount: number,
  ): void {
    throw new Error(
      "Legacy renderCommands not supported. Use renderVariableLengthCommands.",
    );
  }

  /**
   * Legacy method: Execute commands using the context's internal command buffer (deprecated)
   * @deprecated Use renderVariableLengthCommands instead
   */
  renderCommandsToContext(contextId: number, commands: any[]): void {
    throw new Error(
      "Legacy renderCommandsToContext not supported. Use renderVariableLengthCommands.",
    );
  }

  /**
   * Create a rendering context for a surface
   */
  createContext(surfaceId: number): number {
    const wasm = this.getExports();
    const contextId = wasm.create_context(surfaceId);
    if (contextId < 0) {
      throw new Error(`Failed to create context for surface ${surfaceId}`);
    }
    return contextId;
  }

  /**
   * Clone a rendering context (creates independent state copy)
   */
  cloneContext(contextId: number): number {
    const wasm = this.getExports();
    const newContextId = wasm.clone_context(contextId);
    if (newContextId < 0) {
      throw new Error(`Failed to clone context ${contextId}`);
    }
    return newContextId;
  }

  /**
   * Destroy a rendering context (decrements surface ref count)
   */
  destroyContext(contextId: number): void {
    const wasm = this.getExports();
    const result = wasm.destroy_context(contextId);
    if (result !== 0) {
      throw new Error(
        `Failed to destroy context ${contextId}: error code ${result}`,
      );
    }
  }

  /**
   * Create a reference to a surface (increments ref count)
   */
  createReference(surfaceId: number): number {
    const wasm = this.getExports();
    const result = wasm.create_reference(surfaceId);
    if (result < 0) {
      throw new Error(`Failed to create reference for surface ${surfaceId}`);
    }
    return result;
  }

  /**
   * Release a reference to a surface (decrements ref count)
   */
  releaseReference(surfaceId: number): void {
    const wasm = this.getExports();
    const result = wasm.release_reference(surfaceId);
    if (result !== 0) {
      throw new Error(`Failed to release reference for surface ${surfaceId}`);
    }
  }

  /**
   * Get the surface ID associated with a context
   */
  getContextSurfaceId(contextId: number): number {
    const wasm = this.getExports();
    const surfaceId = wasm.get_context_surface_id(contextId);
    if (surfaceId < 0) {
      throw new Error(`Invalid context ID ${contextId}`);
    }
    return surfaceId;
  }

  /**
   * Get the maximum number of words (4-byte units) in context's command buffer
   */
  getContextBufferSizeWords(): number {
    const wasm = this.getExports();
    return wasm.get_context_buffer_size_words();
  }

  /**
   * Get the pointer to a context's command buffer
   */
  getContextBufferPtr(contextId: number): number {
    const wasm = this.getExports();
    const ptr = wasm.get_context_buffer_ptr(contextId);
    if (ptr === 0) {
      throw new Error(`Failed to get command buffer for context ${contextId}`);
    }
    return ptr;
  }

  // ========== Helper methods for common commands ==========

  /**
   * Helper: Write a SET_COLOR command
   */
  static writeSetColorCommand(
    w: ByteWriter,
    argb: number,
    which: number = COLOR_FG,
  ): void {
    w.beginCommand(SurfaceOperation.CMD_SET_COLOR);
    w.writeInt32(argb);
    w.writeInt32(which);
    w.finishCommand();
  }

  /**
   * Helper: Write a FILL_RECT command
   */
  static writeFillRectCommand(
    w: ByteWriter,
    x: number,
    y: number,
    width: number,
    height: number,
  ): void {
    w.beginCommand(SurfaceOperation.CMD_FILL_RECT);
    w.writeInt32(x);
    w.writeInt32(y);
    w.writeInt32(width);
    w.writeInt32(height);
    w.finishCommand();
  }

  /**
   * Helper: Write a DRAW_RECT command
   */
  static writeDrawRectCommand(
    w: ByteWriter,
    x: number,
    y: number,
    width: number,
    height: number,
  ): void {
    w.beginCommand(SurfaceOperation.CMD_DRAW_RECT);
    w.writeInt32(x);
    w.writeInt32(y);
    w.writeInt32(width);
    w.writeInt32(height);
    w.finishCommand();
  }

  /**
   * Helper: Write a DRAW_LINE command
   */
  static writeDrawLineCommand(
    w: ByteWriter,
    x1: number,
    y1: number,
    x2: number,
    y2: number,
  ): void {
    w.beginCommand(SurfaceOperation.CMD_DRAW_LINE);
    w.writeInt32(x1);
    w.writeInt32(y1);
    w.writeInt32(x2);
    w.writeInt32(y2);
    w.finishCommand();
  }

  /**
   * Helper: Write a CLEAR_RECT command
   */
  static writeClearRectCommand(
    w: ByteWriter,
    x: number,
    y: number,
    width: number,
    height: number,
  ): void {
    w.beginCommand(SurfaceOperation.CMD_CLEAR_RECT);
    w.writeInt32(x);
    w.writeInt32(y);
    w.writeInt32(width);
    w.writeInt32(height);
    w.finishCommand();
  }

  /**
   * Helper: Write a SET_CLIP_RECT command
   */
  static writeSetClipRectCommand(
    w: ByteWriter,
    x: number,
    y: number,
    width: number,
    height: number,
  ): void {
    w.beginCommand(SurfaceOperation.CMD_SET_CLIP_RECT);
    w.writeInt32(x);
    w.writeInt32(y);
    w.writeInt32(width);
    w.writeInt32(height);
    w.finishCommand();
  }

  /**
   * Helper: Write a BLIT_IMAGE command
   */
  static writeBlitImageCommand(
    w: ByteWriter,
    imageId: number,
    x: number,
    y: number,
  ): void {
    w.beginCommand(SurfaceOperation.CMD_BLIT_IMAGE);
    w.writeInt32(imageId);
    w.writeInt32(x);
    w.writeInt32(y);
    w.finishCommand();
  }

  /**
   * Helper: Write a SET_TRANSFORM command
   * Transform is a 2x3 affine transform matrix:
   *   m00  m01  m02
   *   m10  m11  m12
   */
  static writeSetTransformCommand(
    w: ByteWriter,
    m00: number,
    m01: number,
    m02: number,
    m10: number,
    m11: number,
    m12: number,
  ): void {
    w.beginCommand(SurfaceOperation.CMD_SET_TRANSFORM);
    w.writeFloat(m00);
    w.writeFloat(m01);
    w.writeFloat(m02);
    w.writeFloat(m10);
    w.writeFloat(m11);
    w.writeFloat(m12);
    w.finishCommand();
  }

  /**
   * Helper: Create a SET_COMPOSITE command
   */
  static writeCompositCommand(
    w: ByteWriter,
    mode: CompositeMode,
    alpha: number,
  ): void {
    w.beginCommand(SurfaceOperation.CMD_SET_COMPOSITE);
    w.writeInt32(mode);
    w.writeFloat(alpha);
    w.finishCommand();
  }

  /**
   * Helper: Write a FILL_POLYGON command
   */
  static writeFillPolygonCommand(
    w: ByteWriter,
    xPoints: number[],
    yPoints: number[],
  ): void {
    if (xPoints.length !== yPoints.length) {
      throw new Error("xPoints and yPoints must have the same length");
    }
    const nPoints = xPoints.length;
    
    w.beginCommand(SurfaceOperation.CMD_FILL_POLYGON);
    w.writeInt32(nPoints);
    for (let i = 0; i < nPoints; i++) {
      w.writeInt32(xPoints[i]);
    }
    for (let i = 0; i < nPoints; i++) {
      w.writeInt32(yPoints[i]);
    }
    w.finishCommand();
  }

  /**
   * Helper: Write a FILL_OVAL command
   */
  static writeFillOvalCommand(
    w: ByteWriter,
    x: number,
    y: number,
    width: number,
    height: number,
  ): void {
    w.beginCommand(SurfaceOperation.CMD_FILL_OVAL);
    w.writeInt32(x);
    w.writeInt32(y);
    w.writeInt32(width);
    w.writeInt32(height);
    w.finishCommand();
  }

  /**
   * Helper: Write a FILL_ARC command
   */
  static writeFillArcCommand(
    w: ByteWriter,
    x: number,
    y: number,
    width: number,
    height: number,
    startAngle: number,
    arcAngle: number,
  ): void {
    w.beginCommand(SurfaceOperation.CMD_FILL_ARC);
    w.writeInt32(x);
    w.writeInt32(y);
    w.writeInt32(width);
    w.writeInt32(height);
    w.writeInt32(startAngle);
    w.writeInt32(arcAngle);
    w.finishCommand();
  }

  /**
   * Helper: Write a FILL_ROUND_RECT command
   */
  static writeFillRoundRectCommand(
    w: ByteWriter,
    x: number,
    y: number,
    width: number,
    height: number,
    arcWidth: number,
    arcHeight: number,
  ): void {
    w.beginCommand(SurfaceOperation.CMD_FILL_ROUND_RECT);
    w.writeInt32(x);
    w.writeInt32(y);
    w.writeInt32(width);
    w.writeInt32(height);
    w.writeInt32(arcWidth);
    w.writeInt32(arcHeight);
    w.finishCommand();
  }

  /**
   * Helper: Convert ARGB color components to a single uint32
   */
  static makeARGB(a: number, r: number, g: number, b: number): number {
    return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) |
      (b & 0xFF);
  }

  /**
   * Helper: Extract ARGB components from a uint32 color
   */
  static extractARGB(
    argb: number,
  ): { a: number; r: number; g: number; b: number } {
    return {
      a: (argb >>> 24) & 0xFF,
      r: (argb >>> 16) & 0xFF,
      g: (argb >>> 8) & 0xFF,
      b: argb & 0xFF,
    };
  }

  /**
   * Get stack tracking information
   */
  getStackBufferPtr(): number {
    const wasm = this.getExports();
    return wasm.get_stack_buffer_ptr ? wasm.get_stack_buffer_ptr() : 0;
  }

  getStackDepth(): number {
    const wasm = this.getExports();
    return wasm.get_stack_depth ? wasm.get_stack_depth() : 0;
  }

  getMaxStackDepth(): number {
    const wasm = this.getExports();
    return wasm.get_max_stack_depth ? wasm.get_max_stack_depth() : 0;
  }

  /**
   * Read the current stack trace from WASM memory
   */
  readStackTrace(): string {
    try {
      const stackPtr = this.getStackBufferPtr();
      const depth = this.getStackDepth();
      const maxDepth = this.getMaxStackDepth();

      if (stackPtr === 0 || depth === 0) {
        return "";
      }

      const wasm = this.getExports();
      const memory = wasm.memory as WebAssembly.Memory;

      let result = `Call stack (depth=${depth}):\n`;

      // Each frame is 32 bytes: 4-byte function name ptr + 4-byte line number + 
      // 8-byte timestamp + 4-byte context ptr + 4-byte error_code +
      // 4-byte surface_id + 4-byte context_id + 2-byte operation_type +
      // 2-byte command_index + 2-byte ref_count + 2-byte flags
      for (let i = 0; i < Math.min(depth, maxDepth); i++) {
        const frameOffset = stackPtr + (i * 32);

        const view = new DataView(memory.buffer);
        const funcNamePtr = view.getUint32(frameOffset, true);
        const lineNumber = view.getInt32(frameOffset + 4, true);
        const timestamp = view.getFloat64(frameOffset + 8, true);
        const contextPtr = view.getUint32(frameOffset + 16, true);
        const errorCode = view.getInt32(frameOffset + 20, true);
        const surfaceId = view.getInt32(frameOffset + 24, true);
        const contextId = view.getInt32(frameOffset + 28, true);
        const operationType = view.getUint16(frameOffset + 32, true);
        const commandIndex = view.getUint16(frameOffset + 34, true);
        const refCount = view.getUint16(frameOffset + 36, true);

        // Read null-terminated function name
        const functionName = this.readNullTerminatedString(funcNamePtr);

        // Format the frame output
        result += `  #${i}: ${functionName} (line ${lineNumber}) [${timestamp.toFixed(3)}ms]`;

        // Add error code if present
        if (errorCode !== 0) {
          result += ` ERR=${errorCode}`;
        }

        // Add surface/context IDs if valid
        if (surfaceId >= 0) {
          result += ` surf=${surfaceId}`;
        }
        if (contextId >= 0) {
          result += ` ctx=${contextId}`;
        }

        // Add operation type if present
        if (operationType !== 0) {
          result += ` op=${operationType}`;
        }

        // Add command index if present
        if (commandIndex !== 0) {
          result += ` cmd=${commandIndex}`;
        }

        // Add reference count if present
        if (refCount !== 0) {
          result += ` refs=${refCount}`;
        }

        // Add context if available
        if (contextPtr !== 0) {
          const context = this.readNullTerminatedString(contextPtr);
          if (context && context !== "<unknown>") {
            result += ` - ${context}`;
          }
        }

        result += "\n";
      }

      return result;
    } catch (e) {
      return `Error reading stack trace: ${e}`;
    }
  }

  /**
   * Read a null-terminated string from WASM memory
   */
  private readNullTerminatedString(ptr: number): string {
    if (ptr === 0) return "<unknown>";

    try {
      const wasm = this.getExports();
      const memory = wasm.memory as WebAssembly.Memory;
      const buffer = new Uint8Array(memory.buffer, ptr, 256);

      let len = 0;
      while (len < 256 && buffer[len] !== 0) {
        len++;
      }

      return new TextDecoder().decode(buffer.slice(0, len));
    } catch (e) {
      return "<error>";
    }
  }
}
