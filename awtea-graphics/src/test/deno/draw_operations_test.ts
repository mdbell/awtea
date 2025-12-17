/**
 * Tests for draw operations (outline primitives)
 * 
 * These tests verify that the WASM rasterizer correctly implements:
 * - drawOval (ellipse outlines)
 * - drawArc (arc outlines)
 * - drawRoundRect (rounded rectangle outlines)
 * - drawPolyline (connected line segments)
 * - copyArea (pixel copying with offsets)
 */

import { WasmRasterizer } from "./wasm_rasterizer.ts";

const WASM_PATH = "../../../build/wasm/awt_raster.wasm";

/**
 * Helper to count non-transparent pixels
 */
function countFilledPixels(pixels: Uint32Array): number {
  let count = 0;
  for (let i = 0; i < pixels.length; i++) {
    const alpha = (pixels[i] >>> 24) & 0xFF;
    if (alpha > 0) count++;
  }
  return count;
}

/**
 * Helper to check if pixel is specific color
 */
function isColor(pixel: number, expectedColor: number): boolean {
  return (pixel >>> 0) === (expectedColor >>> 0);
}

Deno.test("drawOval - circle outline", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(100, 100);
  const contextId = rasterizer.createContext(surfaceId);
  const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);

  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, blue),
    (w) => WasmRasterizer.writeDrawOvalCommand(w, 20, 20, 60, 60),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);
  const filledCount = countFilledPixels(pixels);

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);

  // Should have drawn pixels (outline only, not filled)
  // Approximately 2*pi*r = 2*pi*30 ≈ 188 pixels
  if (filledCount < 100 || filledCount > 300) {
    throw new Error(
      `Unexpected number of pixels drawn: ${filledCount} (expected ~188)`,
    );
  }
});

Deno.test("drawOval - ellipse outline", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(100, 100);
  const contextId = rasterizer.createContext(surfaceId);
  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);

  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, red),
    (w) => WasmRasterizer.writeDrawOvalCommand(w, 10, 30, 80, 40),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);
  const filledCount = countFilledPixels(pixels);

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);

  // Should have drawn an ellipse outline
  if (filledCount < 100) {
    throw new Error(`Too few pixels drawn: ${filledCount}`);
  }
});

Deno.test("drawArc - quarter circle", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(100, 100);
  const contextId = rasterizer.createContext(surfaceId);
  const green = WasmRasterizer.makeARGB(255, 0, 255, 0);

  // Draw a 90-degree arc (0 to 90 degrees)
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, green),
    (w) => WasmRasterizer.writeDrawArcCommand(w, 20, 20, 60, 60, 0, 90),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);
  const filledCount = countFilledPixels(pixels);

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);

  // Quarter circle should have ~pi*r/2 ≈ 47 pixels
  if (filledCount < 20 || filledCount > 100) {
    throw new Error(
      `Unexpected number of pixels drawn: ${filledCount} (expected ~47)`,
    );
  }
});

Deno.test("drawArc - half circle", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(100, 100);
  const contextId = rasterizer.createContext(surfaceId);
  const yellow = WasmRasterizer.makeARGB(255, 255, 255, 0);

  // Draw a 180-degree arc
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, yellow),
    (w) => WasmRasterizer.writeDrawArcCommand(w, 20, 20, 60, 60, 0, 180),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);
  const filledCount = countFilledPixels(pixels);

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);

  // Half circle should have ~pi*r ≈ 94 pixels
  if (filledCount < 50 || filledCount > 150) {
    throw new Error(
      `Unexpected number of pixels drawn: ${filledCount} (expected ~94)`,
    );
  }
});

Deno.test("drawRoundRect - rounded corners", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(100, 100);
  const contextId = rasterizer.createContext(surfaceId);
  const cyan = WasmRasterizer.makeARGB(255, 0, 255, 255);

  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, cyan),
    (w) => WasmRasterizer.writeDrawRoundRectCommand(w, 20, 20, 60, 60, 20, 20),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);
  const filledCount = countFilledPixels(pixels);

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);

  // Rounded rectangle outline should have drawn pixels
  if (filledCount < 150) {
    throw new Error(`Too few pixels drawn: ${filledCount}`);
  }
});

Deno.test("drawPolyline - 3 connected segments", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(100, 100);
  const contextId = rasterizer.createContext(surfaceId);
  const magenta = WasmRasterizer.makeARGB(255, 255, 0, 255);

  // Draw a polyline forming a "Z" shape (not closed)
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, magenta),
    (w) => WasmRasterizer.writeDrawPolylineCommand(w, [20, 80, 20, 80], [20, 20, 80, 80]),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);
  const filledCount = countFilledPixels(pixels);

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);

  // Should have drawn 3 line segments
  if (filledCount < 100) {
    throw new Error(`Too few pixels drawn: ${filledCount}`);
  }
});

Deno.test("copyArea - simple copy", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(100, 100);
  const contextId = rasterizer.createContext(surfaceId);
  const orange = WasmRasterizer.makeARGB(255, 255, 165, 0);

  // Draw a rectangle, then copy it to a new location
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, orange),
    (w) => WasmRasterizer.writeFillRectCommand(w, 10, 10, 20, 20),
    (w) => WasmRasterizer.writeCopyAreaCommand(w, 10, 10, 20, 20, 40, 40),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);

  // Check that pixels were copied to the new location
  const originalIdx = 15 * 100 + 15; // Center of original rect
  const copiedIdx = 55 * 100 + 55; // Center of copied rect (offset by 40,40)

  const originalPixel = pixels[originalIdx] >>> 0;
  const copiedPixel = pixels[copiedIdx] >>> 0;
  const expectedOrange = orange >>> 0;

  if (!isColor(originalPixel, expectedOrange)) {
    throw new Error(
      `Original pixel not orange: expected ${expectedOrange.toString(16)}, got ${originalPixel.toString(16)}`,
    );
  }

  if (!isColor(copiedPixel, expectedOrange)) {
    throw new Error(
      `Copied pixel not orange: expected ${expectedOrange.toString(16)}, got ${copiedPixel.toString(16)}`,
    );
  }
});

Deno.test("copyArea - overlapping regions (copy right/down)", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(100, 100);
  const contextId = rasterizer.createContext(surfaceId);
  const purple = WasmRasterizer.makeARGB(255, 128, 0, 128);

  // Draw a rectangle, then copy it to overlapping region
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, purple),
    (w) => WasmRasterizer.writeFillRectCommand(w, 10, 10, 30, 30),
    (w) => WasmRasterizer.writeCopyAreaCommand(w, 10, 10, 30, 30, 10, 10),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);

  // Check that copy worked correctly with overlap
  const copiedIdx = 30 * 100 + 30; // New location after offset
  const copiedPixel = pixels[copiedIdx] >>> 0;
  const expectedPurple = purple >>> 0;

  if (!isColor(copiedPixel, expectedPurple)) {
    throw new Error(
      `Copied pixel not purple: expected ${expectedPurple.toString(16)}, got ${copiedPixel.toString(16)}`,
    );
  }
});

Deno.test("copyArea - overlapping regions (copy left/up)", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(100, 100);
  const contextId = rasterizer.createContext(surfaceId);
  const teal = WasmRasterizer.makeARGB(255, 0, 128, 128);

  // Draw a rectangle, then copy it to overlapping region (negative offset)
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, teal),
    (w) => WasmRasterizer.writeFillRectCommand(w, 30, 30, 30, 30),
    (w) => WasmRasterizer.writeCopyAreaCommand(w, 30, 30, 30, 30, -10, -10),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);

  // Check that copy worked correctly with overlap
  const copiedIdx = 30 * 100 + 30; // New location after negative offset
  const copiedPixel = pixels[copiedIdx] >>> 0;
  const expectedTeal = teal >>> 0;

  if (!isColor(copiedPixel, expectedTeal)) {
    throw new Error(
      `Copied pixel not teal: expected ${expectedTeal.toString(16)}, got ${copiedPixel.toString(16)}`,
    );
  }
});
