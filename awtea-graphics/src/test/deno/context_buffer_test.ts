/**
 * Test for context-owned command buffers
 * 
 * Verifies that:
 * - Each context has its own fixed-size command buffer
 * - Commands can be written to the context buffer
 * - Rendering works with context buffer (cmdPtr=0)
 * - Buffer size is queryable
 */

import { assertEquals, assertNotEquals } from "https://deno.land/std@0.224.0/assert/mod.ts";
import { WasmRasterizer, PixelFormat } from "./wasm_rasterizer.ts";

const WASM_PATH = "../../../build/wasm/awt_raster.wasm";

// Helper to compare Uint32 values (JavaScript bitwise operations are signed)
function assertPixelEquals(actual: number, expected: number, message: string) {
  const actualU32 = (actual >>> 0);
  const expectedU32 = (expected >>> 0);
  assertEquals(actualU32, expectedU32, message);
}

Deno.test("Context buffer - basic allocation and usage", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(10, 10);
  const contextId = rasterizer.createContext(surfaceId);

  // Verify we can get the max commands
  const maxCommands = rasterizer.getMaxContextCommands();
  assertNotEquals(maxCommands, 0, "Max commands should be non-zero");
  assertEquals(maxCommands, 512, "Max commands should be 512 (as defined in C)");

  // Verify we can get the buffer pointer
  const bufferPtr = rasterizer.getContextCommandBufferPtr(contextId);
  assertNotEquals(bufferPtr, 0, "Buffer pointer should be non-zero");

  // Clean up
  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Context buffer - render with context buffer (cmdPtr=0)", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(10, 10);
  const contextId = rasterizer.createContext(surfaceId);

  // Get the context's command buffer
  const bufferPtr = rasterizer.getContextCommandBufferPtr(contextId);
  
  // Write commands to the context buffer
  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
  rasterizer.writeCommand(bufferPtr, 0, WasmRasterizer.setColorCommand(red));
  rasterizer.writeCommand(bufferPtr, 1, WasmRasterizer.fillRectCommand(0, 0, 10, 10));

  // Render with cmdPtr=0 to use the context buffer
  const wasm = (rasterizer as any).getExports();
  const result = wasm.render_awt(contextId, 0, 2);
  assertEquals(result, 0, "render_awt should succeed");

  // Verify the surface is red
  const pixels = rasterizer.copySurfacePixels(surfaceId);
  assertPixelEquals(pixels[0], red, "Surface should be red");

  // Clean up
  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Context buffer - use helper method renderCommandsToContext", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(10, 10);
  const contextId = rasterizer.createContext(surfaceId);

  // Use the helper method to render commands
  const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);
  const commands = [
    WasmRasterizer.setColorCommand(blue),
    WasmRasterizer.fillRectCommand(0, 0, 10, 10),
  ];
  
  rasterizer.renderCommandsToContext(contextId, commands);

  // Verify the surface is blue
  const pixels = rasterizer.copySurfacePixels(surfaceId);
  assertPixelEquals(pixels[0], blue, "Surface should be blue");

  // Clean up
  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Context buffer - multiple contexts have independent buffers", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(10, 10);
  const context1 = rasterizer.createContext(surfaceId);
  const context2 = rasterizer.createContext(surfaceId);

  // Get buffer pointers for both contexts
  const buffer1 = rasterizer.getContextCommandBufferPtr(context1);
  const buffer2 = rasterizer.getContextCommandBufferPtr(context2);

  // Verify they are different buffers
  assertNotEquals(buffer1, buffer2, "Each context should have its own buffer");

  // Write red to context1's buffer
  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
  rasterizer.writeCommand(buffer1, 0, WasmRasterizer.setColorCommand(red));
  rasterizer.writeCommand(buffer1, 1, WasmRasterizer.fillRectCommand(0, 0, 10, 10));

  // Write green to context2's buffer
  const green = WasmRasterizer.makeARGB(255, 0, 255, 0);
  rasterizer.writeCommand(buffer2, 0, WasmRasterizer.setColorCommand(green));
  rasterizer.writeCommand(buffer2, 1, WasmRasterizer.fillRectCommand(0, 0, 10, 10));

  // Render with context1 - should be red
  const wasm = (rasterizer as any).getExports();
  wasm.render_awt(context1, 0, 2);
  const pixels1 = rasterizer.copySurfacePixels(surfaceId);
  assertPixelEquals(pixels1[0], red, "Context1 render should produce red");

  // Render with context2 - should be green
  wasm.render_awt(context2, 0, 2);
  const pixels2 = rasterizer.copySurfacePixels(surfaceId);
  assertPixelEquals(pixels2[0], green, "Context2 render should produce green");

  // Clean up
  rasterizer.destroyContext(context1);
  rasterizer.destroyContext(context2);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Context buffer - cloned context gets its own buffer", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(10, 10);
  const context1 = rasterizer.createContext(surfaceId);
  const context2 = rasterizer.cloneContext(context1);

  // Get buffer pointers
  const buffer1 = rasterizer.getContextCommandBufferPtr(context1);
  const buffer2 = rasterizer.getContextCommandBufferPtr(context2);

  // Verify they are different buffers
  assertNotEquals(buffer1, buffer2, "Cloned context should have its own buffer");

  // Clean up
  rasterizer.destroyContext(context1);
  rasterizer.destroyContext(context2);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Context buffer - backward compatibility with explicit buffer", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(10, 10);
  const contextId = rasterizer.createContext(surfaceId);

  // Create a temporary buffer (old way)
  const tempBuffer = rasterizer.createCommandBuffer(10);
  
  // Write commands to the temporary buffer
  const yellow = WasmRasterizer.makeARGB(255, 255, 255, 0);
  rasterizer.writeCommand(tempBuffer, 0, WasmRasterizer.setColorCommand(yellow));
  rasterizer.writeCommand(tempBuffer, 1, WasmRasterizer.fillRectCommand(0, 0, 10, 10));

  // Render with explicit buffer pointer (old way)
  rasterizer.renderCommands(contextId, tempBuffer, 2);

  // Verify the surface is yellow
  const pixels = rasterizer.copySurfacePixels(surfaceId);
  assertPixelEquals(pixels[0], yellow, "Surface should be yellow");

  // Clean up temporary buffer (would need to be freed manually in old way)
  // In the new implementation, only context buffers are automatically freed

  // Clean up
  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});
