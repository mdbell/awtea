/**
 * Test for context-owned command buffers
 *
 * Verifies that:
 * - Each context has its own fixed-size command buffer
 * - Commands can be written to the context buffer
 * - Rendering works with context buffer (cmdPtr=0)
 * - Buffer size is queryable
 */

import {
  assertEquals,
  assertNotEquals,
} from "https://deno.land/std@0.224.0/assert/mod.ts";
import { PixelFormat, WasmRasterizer } from "./wasm_rasterizer.ts";
import { assertPixelEquals } from "./test_helpers.ts";

const WASM_PATH = "../../../build/wasm/awt_raster.wasm";

Deno.test("Context buffer - basic allocation and usage", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(10, 10);
  const contextId = rasterizer.createContext(surfaceId);

  // Verify we can get the buffer size in words
  const bufferSizeWords = rasterizer.getContextBufferSizeWords();
  assertNotEquals(bufferSizeWords, 0, "Buffer size should be non-zero");
  assertEquals(
    bufferSizeWords,
    8192,
    "Buffer size should be 8192 words (32KB)",
  );

  // Verify we can get the buffer pointer
  const bufferPtr = rasterizer.getContextBufferPtr(contextId);
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

  // Write commands using variable-length format
  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, red),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 10, 10),
  ]);

  // Verify the surface is red
  const pixels = rasterizer.copySurfacePixels(surfaceId);
  assertPixelEquals(pixels[0], red, "Surface should be red");

  // Clean up
  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Context buffer - use helper method renderVariableLengthCommands", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(10, 10);
  const contextId = rasterizer.createContext(surfaceId);

  // Use the helper method to render commands
  const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, blue),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 10, 10),
  ]);

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
  const buffer1 = rasterizer.getContextBufferPtr(context1);
  const buffer2 = rasterizer.getContextBufferPtr(context2);

  // Verify they are different buffers
  assertNotEquals(buffer1, buffer2, "Each context should have its own buffer");

  // Render with context1 - should be red
  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
  rasterizer.renderVariableLengthCommands(context1, [
    (w) => WasmRasterizer.writeSetColorCommand(w, red),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 10, 10),
  ]);
  const pixels1 = rasterizer.copySurfacePixels(surfaceId);
  assertPixelEquals(pixels1[0], red, "Context1 render should produce red");

  // Render with context2 - should be green
  const green = WasmRasterizer.makeARGB(255, 0, 255, 0);
  rasterizer.renderVariableLengthCommands(context2, [
    (w) => WasmRasterizer.writeSetColorCommand(w, green),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 10, 10),
  ]);
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
  const buffer1 = rasterizer.getContextBufferPtr(context1);
  const buffer2 = rasterizer.getContextBufferPtr(context2);

  // Verify they are different buffers
  assertNotEquals(
    buffer1,
    buffer2,
    "Cloned context should have its own buffer",
  );

  // Clean up
  rasterizer.destroyContext(context1);
  rasterizer.destroyContext(context2);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Context buffer - variable-length command writing", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(10, 10);
  const contextId = rasterizer.createContext(surfaceId);

  // Write commands using variable-length format
  const yellow = WasmRasterizer.makeARGB(255, 255, 255, 0);
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, yellow),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 10, 10),
  ]);

  // Verify the surface is yellow
  const pixels = rasterizer.copySurfacePixels(surfaceId);
  assertPixelEquals(pixels[0], yellow, "Surface should be yellow");

  // Clean up
  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});
