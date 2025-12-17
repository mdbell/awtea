/**
 * Tests for fill operations using edge table infrastructure
 * 
 * These tests verify that the WASM rasterizer correctly implements:
 * - fillPolygon (arbitrary polygons)
 * - fillOval (circles and ellipses)
 * - fillArc (pie slices)
 * - fillRoundRect (rounded rectangles)
 * - Transform support for fill operations
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

Deno.test("fillPolygon - simple triangle", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(100, 100);
  const contextId = rasterizer.createContext(surfaceId);
  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);

  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, red),
    (w) => WasmRasterizer.writeFillPolygonCommand(w, [25, 75, 50], [25, 25, 75]),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);
  const centerIdx = 42 * 100 + 50;
  const centerPixel = pixels[centerIdx] >>> 0;
  const expectedRed = red >>> 0;

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);

  if (centerPixel !== expectedRed) {
    throw new Error(
      `Center pixel mismatch: expected ${expectedRed.toString(16)}, got ${centerPixel.toString(16)}`,
    );
  }
});

Deno.test("fillPolygon - rectangle (4 points)", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(100, 100);
  const contextId = rasterizer.createContext(surfaceId);
  const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);

  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, blue),
    (w) => WasmRasterizer.writeFillPolygonCommand(w, [20, 80, 80, 20], [20, 20, 80, 80]),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);
  const filled = countFilledPixels(pixels);

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);

  // Rectangle should fill approximately 60x60 = 3600 pixels
  if (filled < 3000) {
    throw new Error(`Too few pixels filled: ${filled} (expected ~3600)`);
  }
});

Deno.test("fillOval - circle", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(100, 100);
  const contextId = rasterizer.createContext(surfaceId);
  const green = WasmRasterizer.makeARGB(255, 0, 255, 0);

  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, green),
    (w) => WasmRasterizer.writeFillOvalCommand(w, 20, 20, 60, 60),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);
  const centerIdx = 50 * 100 + 50;
  const centerPixel = pixels[centerIdx] >>> 0;
  const expectedGreen = green >>> 0;

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);

  if (centerPixel !== expectedGreen) {
    throw new Error("Center of circle not filled with correct color");
  }
});

Deno.test("fillArc - pie slice (0-90 degrees)", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(100, 100);
  const contextId = rasterizer.createContext(surfaceId);
  const yellow = WasmRasterizer.makeARGB(255, 255, 255, 0);

  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, yellow),
    (w) => WasmRasterizer.writeFillArcCommand(w, 20, 20, 60, 60, 0, 90),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);
  const filled = countFilledPixels(pixels);

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);

  // Quarter circle should fill approximately 700-800 pixels
  if (filled < 500) {
    throw new Error(`Too few pixels filled: ${filled} (expected ~700)`);
  }
});

Deno.test("fillRoundRect - rounded corners", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(100, 100);
  const contextId = rasterizer.createContext(surfaceId);
  const cyan = WasmRasterizer.makeARGB(255, 0, 255, 255);

  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, cyan),
    (w) => WasmRasterizer.writeFillRoundRectCommand(w, 20, 20, 60, 60, 20, 20),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);
  const filled = countFilledPixels(pixels);

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);

  // Rounded rect should fill most of the area (at least 2900 pixels)
  // Note: Exact count depends on arc tessellation, allow some variance
  if (filled < 2900) {
    throw new Error(`Too few pixels filled: ${filled} (expected >2900)`);
  }
});

Deno.test("fillPolygon with transform - translation", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(100, 100);
  const contextId = rasterizer.createContext(surfaceId);
  const orange = WasmRasterizer.makeARGB(255, 255, 165, 0);

  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetTransformCommand(w, 1, 0, 30, 0, 1, 30),
    (w) => WasmRasterizer.writeSetColorCommand(w, orange),
    (w) => WasmRasterizer.writeFillPolygonCommand(w, [0, 20, 10], [0, 0, 20]),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);
  const centerIdx = 36 * 100 + 36;
  const centerPixel = pixels[centerIdx] >>> 0;
  const expectedOrange = orange >>> 0;

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);

  if (centerPixel !== expectedOrange) {
    throw new Error("Transform not applied correctly to fillPolygon");
  }
});
