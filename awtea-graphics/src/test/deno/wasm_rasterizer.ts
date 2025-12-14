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

// Color slot constants
export const COLOR_FG = 0;
export const COLOR_BG = 1;

// Re-export for convenience
export { PixelFormat, SurfaceOperation };

/**
 * Surface command structure (must match SurfaceCommand in awt_raster_internal.h)
 * Total size: 28 bytes
 */
export interface SurfaceCommand {
  operation: number;      // uint8_t (1 byte)
  reserved: [number, number, number]; // uint8_t[3] (3 bytes)
  x: number;              // uint32_t (4 bytes)
  y: number;              // uint32_t (4 bytes)
  width: number;          // uint32_t (4 bytes)
  height: number;         // uint32_t (4 bytes)
  arg1: number;           // uint32_t (4 bytes)
  arg2: number;           // uint32_t (4 bytes)
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
        wasm_log_callback: (level: number, messagePtr: number, messageLen: number) => {
          // Get the exports to access memory
          if (!this.wasmInstance) return;
          const memory = (this.wasmInstance.exports.memory as WebAssembly.Memory);
          if (!memory) return;
          
          // Read the message from WASM memory
          const messageBytes = new Uint8Array(memory.buffer, messagePtr, messageLen);
          const message = new TextDecoder().decode(messageBytes);
          
          // Log based on level (0=ERROR, 1=WARN, 2=INFO, 3=DEBUG, 4=TRACE)
          const levelNames = ['ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE'];
          const levelName = levelNames[level] || 'UNKNOWN';
          console.log(`[WASM ${levelName}] ${message}`);
        }
      }
    };
    
    this.wasmInstance = await WebAssembly.instantiate(wasmModule, imports);
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
  allocateSurface(width: number, height: number, format: PixelFormat = PixelFormat.ARGB): number {
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
    wasm.reset_surface(surfaceId, 0, 0, 0, PixelFormat.ARGB);
  }

  /**
   * Get surface dimensions
   */
  getSurfaceDimensions(surfaceId: number): { width: number; height: number; stride: number } {
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
      throw new Error(`Invalid surface ID ${surfaceId} or surface not allocated`);
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
   * Create a command buffer
   */
  createCommandBuffer(maxCommands: number): number {
    const wasm = this.getExports();
    const ptr = wasm.request_command_buffer(maxCommands);
    if (ptr === 0) {
      throw new Error("Failed to allocate command buffer");
    }
    return ptr;
  }

  /**
   * Get command size in bytes
   */
  getCommandSize(): number {
    const wasm = this.getExports();
    return wasm.get_command_size();
  }

  /**
   * Write a command to the buffer
   */
  writeCommand(bufferPtr: number, index: number, cmd: SurfaceCommand): void {
    const wasm = this.getExports();
    const cmdSize = wasm.get_command_size();
    const offset = bufferPtr + (index * cmdSize);
    
    const view = new DataView(wasm.memory.buffer);
    view.setUint8(offset + 0, cmd.operation);
    view.setUint8(offset + 1, cmd.reserved[0]);
    view.setUint8(offset + 2, cmd.reserved[1]);
    view.setUint8(offset + 3, cmd.reserved[2]);
    view.setUint32(offset + 4, cmd.x, true);
    view.setUint32(offset + 8, cmd.y, true);
    view.setUint32(offset + 12, cmd.width, true);
    view.setUint32(offset + 16, cmd.height, true);
    view.setUint32(offset + 20, cmd.arg1, true);
    view.setUint32(offset + 24, cmd.arg2, true);
  }

  /**
   * Execute commands on a surface
   */
  renderCommands(surfaceId: number, bufferPtr: number, commandCount: number): void {
    const wasm = this.getExports();
    const result = wasm.render_awt(surfaceId, bufferPtr, commandCount);
    if (result !== 0) {
      throw new Error(`render_awt failed with error code ${result}`);
    }
  }

  /**
   * Register an image buffer
   */
  registerImage(width: number, height: number, format: PixelFormat = PixelFormat.ARGB): number {
    const wasm = this.getExports();
    const stride = width * 4; // 4 bytes per pixel
    const imageId = wasm.register_image(format, width, height, stride);
    if (imageId < 0) {
      throw new Error(`Failed to register image: error code ${imageId}`);
    }
    return imageId;
  }

  /**
   * Get image pixel buffer
   */
  getImagePixels(imageId: number, width: number, height: number): Uint32Array {
    const wasm = this.getExports();
    const ptr = wasm.get_image_pixels_ptr(imageId);
    if (ptr === 0) {
      throw new Error(`Invalid image ID ${imageId} or image not allocated`);
    }
    
    const buffer = new Uint32Array(wasm.memory.buffer, ptr, width * height);
    return buffer;
  }

  /**
   * Free image pixels
   */
  freeImage(imageId: number): void {
    const bufferPtr = this.createCommandBuffer(1);
    this.writeCommand(bufferPtr, 0, {
      operation: SurfaceOperation.EXT_FREE_IMAGE,
      reserved: [0, 0, 0],
      x: imageId,
      y: 0,
      width: 0,
      height: 0,
      arg1: 0,
      arg2: 0,
    });
    // Note: We need to execute this through a surface, using surface 0 as dummy
    // In practice, EXT_FREE_IMAGE doesn't need a surface context
  }

  /**
   * Helper: Create a SET_COLOR command
   */
  static setColorCommand(argb: number, which: number = COLOR_FG): SurfaceCommand {
    return {
      operation: SurfaceOperation.CMD_SET_COLOR,
      reserved: [0, 0, 0],
      x: 0,
      y: 0,
      width: 0,
      height: 0,
      arg1: argb,
      arg2: which,
    };
  }

  /**
   * Helper: Create a FILL_RECT command
   */
  static fillRectCommand(x: number, y: number, width: number, height: number): SurfaceCommand {
    return {
      operation: SurfaceOperation.CMD_FILL_RECT,
      reserved: [0, 0, 0],
      x,
      y,
      width,
      height,
      arg1: 0,
      arg2: 0,
    };
  }

  /**
   * Helper: Create a DRAW_RECT command
   */
  static drawRectCommand(x: number, y: number, width: number, height: number): SurfaceCommand {
    return {
      operation: SurfaceOperation.CMD_DRAW_RECT,
      reserved: [0, 0, 0],
      x,
      y,
      width,
      height,
      arg1: 0,
      arg2: 0,
    };
  }

  /**
   * Helper: Create a DRAW_LINE command
   */
  static drawLineCommand(x1: number, y1: number, x2: number, y2: number): SurfaceCommand {
    return {
      operation: SurfaceOperation.CMD_DRAW_LINE,
      reserved: [0, 0, 0],
      x: x1,
      y: y1,
      width: x2,
      height: y2,
      arg1: 0,
      arg2: 0,
    };
  }

  /**
   * Helper: Create a CLEAR_RECT command
   */
  static clearRectCommand(x: number, y: number, width: number, height: number): SurfaceCommand {
    return {
      operation: SurfaceOperation.CMD_CLEAR_RECT,
      reserved: [0, 0, 0],
      x,
      y,
      width,
      height,
      arg1: 0,
      arg2: 0,
    };
  }

  /**
   * Helper: Create a SET_CLIP_RECT command
   */
  static setClipRectCommand(x: number, y: number, width: number, height: number): SurfaceCommand {
    return {
      operation: SurfaceOperation.CMD_SET_CLIP_RECT,
      reserved: [0, 0, 0],
      x,
      y,
      width,
      height,
      arg1: 0,
      arg2: 0,
    };
  }

  /**
   * Helper: Create a BLIT_IMAGE command
   */
  static blitImageCommand(imageId: number, x: number, y: number): SurfaceCommand {
    return {
      operation: SurfaceOperation.CMD_BLIT_IMAGE,
      reserved: [0, 0, 0],
      x,
      y,
      width: 0,
      height: 0,
      arg1: imageId,
      arg2: 0,
    };
  }

  /**
   * Helper: Create a SET_TRANSFORM command
   * Transform is a 2x3 affine transform matrix:
   *   m00  m01  m02
   *   m10  m11  m12
   */
  static setTransformCommand(m00: number, m01: number, m02: number, m10: number, m11: number, m12: number): SurfaceCommand {
    // Convert floats to uint32 representation
    const floatToU32 = (f: number): number => {
      const buf = new ArrayBuffer(4);
      new Float32Array(buf)[0] = f;
      return new Uint32Array(buf)[0];
    };

    return {
      operation: SurfaceOperation.CMD_SET_TRANSFORM,
      reserved: [0, 0, 0],
      x: floatToU32(m00),
      y: floatToU32(m01),
      width: floatToU32(m02),
      height: floatToU32(m10),
      arg1: floatToU32(m11),
      arg2: floatToU32(m12),
    };
  }

  /**
   * Helper: Convert ARGB color components to a single uint32
   */
  static makeARGB(a: number, r: number, g: number, b: number): number {
    return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
  }

  /**
   * Helper: Extract ARGB components from a uint32 color
   */
  static extractARGB(argb: number): { a: number; r: number; g: number; b: number } {
    return {
      a: (argb >>> 24) & 0xFF,
      r: (argb >>> 16) & 0xFF,
      g: (argb >>> 8) & 0xFF,
      b: argb & 0xFF,
    };
  }
}
