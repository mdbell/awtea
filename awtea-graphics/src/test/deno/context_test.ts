/**
 * Multi-context tests for WASM rasterizer
 *
 * This file tests the context management functionality:
 * - Context creation and destruction
 * - Context cloning (independent state)
 * - Reference counting
 * - Multiple contexts rendering to the same surface
 */

import { assertEquals, assertNotEquals } from "@std/assert";
import { PixelFormat, WasmRasterizer } from "./wasm_rasterizer.ts";

const WASM_PATH = "../../../build/wasm/awt_raster.wasm";

// Helper to compare Uint32 values (JavaScript bitwise operations are signed)
function assertPixelEquals(actual: number, expected: number, message: string) {
  const actualU32 = actual >>> 0;
  const expectedU32 = expected >>> 0;
  assertEquals(actualU32, expectedU32, message);
}

Deno.test("Context creation and destruction", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(10, 10);

  // Create a context
  const contextId = rasterizer.createContext(surfaceId);
  assertNotEquals(contextId, -1, "Context creation should succeed");

  // Verify context is associated with the surface
  const associatedSurfaceId = rasterizer.getContextSurfaceId(contextId);
  assertEquals(
    associatedSurfaceId,
    surfaceId,
    "Context should be associated with the surface",
  );

  // Destroy context
  rasterizer.destroyContext(contextId);

  // Clean up
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Context cloning creates independent state", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(10, 10);
  const context1 = rasterizer.createContext(surfaceId);

  // Set context1 to render in red
  const cmdBuffer1 = rasterizer.createCommandBuffer(2);
  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
  rasterizer.writeCommand(cmdBuffer1, 0, WasmRasterizer.setColorCommand(red));
  rasterizer.writeCommand(
    cmdBuffer1,
    1,
    WasmRasterizer.fillRectCommand(0, 0, 10, 10),
  );
  rasterizer.renderCommands(context1, cmdBuffer1, 2);

  // Clone the context
  const context2 = rasterizer.cloneContext(context1);
  assertNotEquals(
    context2,
    context1,
    "Cloned context should have different ID",
  );

  // Verify context2 is associated with the same surface
  assertEquals(rasterizer.getContextSurfaceId(context2), surfaceId);

  // Change context2 state to render in blue
  const cmdBuffer2 = rasterizer.createCommandBuffer(2);
  const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);
  rasterizer.writeCommand(cmdBuffer2, 0, WasmRasterizer.setColorCommand(blue));
  rasterizer.writeCommand(
    cmdBuffer2,
    1,
    WasmRasterizer.fillRectCommand(0, 0, 10, 10),
  );
  rasterizer.renderCommands(context2, cmdBuffer2, 2);

  // Surface should be blue (last render wins)
  const pixels = rasterizer.copySurfacePixels(surfaceId);
  assertPixelEquals(
    pixels[0],
    blue,
    "Surface should be blue after context2 render",
  );

  // Render with context1 again - should still use red (independent state)
  const cmdBuffer3 = rasterizer.createCommandBuffer(1);
  rasterizer.writeCommand(
    cmdBuffer3,
    0,
    WasmRasterizer.fillRectCommand(0, 0, 10, 10),
  );
  rasterizer.renderCommands(context1, cmdBuffer3, 1);

  // Surface should now be red (context1's color state is still red)
  const pixelsAfter = rasterizer.copySurfacePixels(surfaceId);
  assertPixelEquals(
    pixelsAfter[0],
    red,
    "Context1 should still have red color state",
  );

  // Clean up
  rasterizer.destroyContext(context1);
  rasterizer.destroyContext(context2);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Independent clip rectangles per context", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(20, 20);
  const context1 = rasterizer.createContext(surfaceId);
  const context2 = rasterizer.cloneContext(context1);

  // Set context1 clip to left half (0, 0, 10, 20)
  const cmdBuffer1 = rasterizer.createCommandBuffer(3);
  rasterizer.writeCommand(
    cmdBuffer1,
    0,
    WasmRasterizer.setClipRectCommand(0, 0, 10, 20),
  );
  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
  rasterizer.writeCommand(cmdBuffer1, 1, WasmRasterizer.setColorCommand(red));
  rasterizer.writeCommand(
    cmdBuffer1,
    2,
    WasmRasterizer.fillRectCommand(0, 0, 20, 20),
  );
  rasterizer.renderCommands(context1, cmdBuffer1, 3);

  // Verify only left half is red
  const pixelsAfterCtx1 = rasterizer.copySurfacePixels(surfaceId);
  assertPixelEquals(pixelsAfterCtx1[5], red, "Left side should be red");
  assertEquals(pixelsAfterCtx1[15], 0, "Right side should still be black");

  // Set context2 clip to right half (10, 0, 10, 20)
  const cmdBuffer2 = rasterizer.createCommandBuffer(3);
  rasterizer.writeCommand(
    cmdBuffer2,
    0,
    WasmRasterizer.setClipRectCommand(10, 0, 10, 20),
  );
  const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);
  rasterizer.writeCommand(cmdBuffer2, 1, WasmRasterizer.setColorCommand(blue));
  rasterizer.writeCommand(
    cmdBuffer2,
    2,
    WasmRasterizer.fillRectCommand(0, 0, 20, 20),
  );
  rasterizer.renderCommands(context2, cmdBuffer2, 3);

  // Verify left is still red, right is now blue
  const pixelsFinal = rasterizer.copySurfacePixels(surfaceId);
  assertPixelEquals(pixelsFinal[5], red, "Left side should still be red");
  assertPixelEquals(pixelsFinal[15], blue, "Right side should now be blue");

  // Clean up
  rasterizer.destroyContext(context1);
  rasterizer.destroyContext(context2);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Reference counting: surface survives until all contexts destroyed", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(10, 10);

  // Create multiple contexts
  const context1 = rasterizer.createContext(surfaceId);
  const context2 = rasterizer.createContext(surfaceId);
  const context3 = rasterizer.createContext(surfaceId);

  // Destroy contexts one by one
  rasterizer.destroyContext(context1);
  rasterizer.destroyContext(context2);

  // Surface should still be accessible through context3
  const cmdBuffer = rasterizer.createCommandBuffer(2);
  const green = WasmRasterizer.makeARGB(255, 0, 255, 0);
  rasterizer.writeCommand(cmdBuffer, 0, WasmRasterizer.setColorCommand(green));
  rasterizer.writeCommand(
    cmdBuffer,
    1,
    WasmRasterizer.fillRectCommand(0, 0, 10, 10),
  );
  rasterizer.renderCommands(context3, cmdBuffer, 2);

  const pixels = rasterizer.copySurfacePixels(surfaceId);
  assertPixelEquals(
    pixels[0],
    green,
    "Surface should be accessible and render green",
  );

  // Clean up last context
  rasterizer.destroyContext(context3);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Multiple contexts with independent transforms", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(20, 20);
  const context1 = rasterizer.createContext(surfaceId);
  const context2 = rasterizer.cloneContext(context1);

  // Context1: Identity transform, render at (0, 0)
  const cmdBuffer1 = rasterizer.createCommandBuffer(2);
  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
  rasterizer.writeCommand(cmdBuffer1, 0, WasmRasterizer.setColorCommand(red));
  rasterizer.writeCommand(
    cmdBuffer1,
    1,
    WasmRasterizer.fillRectCommand(0, 0, 5, 5),
  );
  rasterizer.renderCommands(context1, cmdBuffer1, 2);

  // Context2: Translate by (10, 10), render at (0, 0) which becomes (10, 10)
  const cmdBuffer2 = rasterizer.createCommandBuffer(3);
  rasterizer.writeCommand(
    cmdBuffer2,
    0,
    WasmRasterizer.setTransformCommand(1, 0, 10, 0, 1, 10),
  );
  const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);
  rasterizer.writeCommand(cmdBuffer2, 1, WasmRasterizer.setColorCommand(blue));
  rasterizer.writeCommand(
    cmdBuffer2,
    2,
    WasmRasterizer.fillRectCommand(0, 0, 5, 5),
  );
  rasterizer.renderCommands(context2, cmdBuffer2, 3);

  // Verify rendering positions
  const pixels = rasterizer.copySurfacePixels(surfaceId);
  assertPixelEquals(pixels[0], red, "Top-left should be red (context1)");
  assertPixelEquals(
    pixels[10 * 20 + 10],
    blue,
    "Position (10,10) should be blue (context2 with transform)",
  );

  // Clean up
  rasterizer.destroyContext(context1);
  rasterizer.destroyContext(context2);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("createReference and releaseReference", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(10, 10);

  // Create references explicitly
  rasterizer.createReference(surfaceId);
  rasterizer.createReference(surfaceId);

  // Release references
  rasterizer.releaseReference(surfaceId);
  rasterizer.releaseReference(surfaceId);

  // Surface should still be valid after ref count operations
  const dims = rasterizer.getSurfaceDimensions(surfaceId);
  assertEquals(dims.width, 10);
  assertEquals(dims.height, 10);

  // Clean up
  rasterizer.freeSurface(surfaceId);
});
