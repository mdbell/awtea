/**
 * Advanced tests for WASM rasterizer
 *
 * This file demonstrates more complex scenarios:
 * - Image registration and blitting
 * - Transforms
 * - Complex drawing patterns
 */

import { assertEquals, assertThrows } from "https://deno.land/std@0.224.0/assert/mod.ts";
import { PixelFormat, WasmRasterizer } from "./wasm_rasterizer.ts";
import { assertPixelEquals } from "./test_helpers.ts";

const WASM_PATH = "../../../build/wasm/awt_raster.wasm";

Deno.test("Identity transform", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(20, 20);

  const contextId = rasterizer.createContext(surfaceId);

  // Create command with identity transform
  const cyan = WasmRasterizer.makeARGB(255, 0, 255, 255);
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetTransformCommand(w, 1, 0, 0, 0, 1, 0),
    (w) => WasmRasterizer.writeSetColorCommand(w, cyan),
    (w) => WasmRasterizer.writeFillRectCommand(w, 5, 5, 10, 10),
  ]);

  // With identity transform, rect should be at (5, 5)
  const pixels = rasterizer.copySurfacePixels(surfaceId);
  const idx = 5 * 20 + 5;
  assertPixelEquals(
    pixels[idx],
    cyan,
    "Pixel (5,5) should be cyan with identity transform",
  );

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Translation transform", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(30, 30);
  const contextId = rasterizer.createContext(surfaceId);

  // Create command with translation transform
  const orange = WasmRasterizer.makeARGB(255, 255, 165, 0);
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetTransformCommand(w, 1, 0, 5, 0, 1, 5),
    (w) => WasmRasterizer.writeSetColorCommand(w, orange),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 5, 5),
  ]);

  // With translation, rect at (0,0) should appear at (5, 5)
  const pixels = rasterizer.copySurfacePixels(surfaceId);

  // Original position should be black
  assertPixelEquals(pixels[0], 0, "Pixel (0,0) should be 0 (not drawn there)");

  // Translated position should have color
  const translatedIdx = 5 * 30 + 5;
  assertPixelEquals(
    pixels[translatedIdx],
    orange,
    "Pixel (5,5) should be orange (translated)",
  );

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Complex drawing pattern - checkerboard", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const width = 20;
  const height = 20;
  const cellSize = 5;
  const surfaceId = rasterizer.allocateSurface(width, height);
  const contextId = rasterizer.createContext(surfaceId);

  const black = WasmRasterizer.makeARGB(255, 0, 0, 0);
  const white = WasmRasterizer.makeARGB(255, 255, 255, 255);

  // Create checkerboard pattern (4x4 cells)
  const commands = [];
  for (let row = 0; row < height / cellSize; row++) {
    for (let col = 0; col < width / cellSize; col++) {
      const color = ((row + col) % 2 === 0) ? black : white;
      const x = col * cellSize;
      const y = row * cellSize;
      commands.push((w: any) => WasmRasterizer.writeSetColorCommand(w, color));
      commands.push((w: any) => WasmRasterizer.writeFillRectCommand(w, x, y, cellSize, cellSize));
    }
  }

  rasterizer.renderVariableLengthCommands(contextId, commands);

  // Verify checkerboard pattern
  const pixels = rasterizer.copySurfacePixels(surfaceId);

  // Top-left cell (0,0) should be black
  assertPixelEquals(pixels[0], black, "Pixel (0,0) should be black");

  // Next cell to the right (5,0) should be white
  const idx1 = 5;
  assertPixelEquals(pixels[idx1], white, "Pixel (5,0) should be white");

  // Cell below top-left (0,5) should be white
  const idx2 = 5 * width;
  assertPixelEquals(pixels[idx2], white, "Pixel (0,5) should be white");

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Drawing with background color", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(10, 10);
  const contextId = rasterizer.createContext(surfaceId);

  // Set background color to gray
  const gray = WasmRasterizer.makeARGB(255, 128, 128, 128);
  // Set foreground color to red
  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);

  rasterizer.renderVariableLengthCommands(contextId, [
    (w: any) => WasmRasterizer.writeSetColorCommand(w, gray, 1), // which=1 for BG
    (w: any) => WasmRasterizer.writeSetColorCommand(w, red, 0), // which=0 for FG
    (w: any) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 10, 10),
    (w: any) => WasmRasterizer.writeClearRectCommand(w, 3, 3, 4, 4),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);

  // Filled area should be red
  assertPixelEquals(pixels[0], red, "Pixel (0,0) should be red");

  // Cleared area should be background color (gray as set above)
  const clearedIdx = 3 * 10 + 3;
  assertPixelEquals(
    pixels[clearedIdx],
    gray,
    "Pixel (3,3) should be gray (background color)",
  );

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Large surface allocation", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  // Allocate a larger surface (256x256)
  const surfaceId = rasterizer.allocateSurface(256, 256);
  const contextId = rasterizer.createContext(surfaceId);

  const dims = rasterizer.getSurfaceDimensions(surfaceId);
  assertEquals(dims.width, 256);
  assertEquals(dims.height, 256);

  // Just fill the whole surface with mid-gray to test large allocation works
  const midGray = WasmRasterizer.makeARGB(255, 128, 128, 128);
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, midGray),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 256, 256),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);
  assertEquals(pixels.length, 256 * 256, "Should have 65536 pixels");
  assertPixelEquals(pixels[0], midGray, "First pixel should be mid-gray");
  assertPixelEquals(
    pixels[pixels.length - 1],
    midGray,
    "Last pixel should be mid-gray",
  );

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Multiple command batches", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(30, 30);
  const contextId = rasterizer.createContext(surfaceId);

  // First batch: fill with red
  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
  rasterizer.renderVariableLengthCommands(contextId, [
    (w: any) => WasmRasterizer.writeSetColorCommand(w, red),
    (w: any) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 30, 30),
  ]);

  // Second batch: draw blue square on top
  const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);
  rasterizer.renderVariableLengthCommands(contextId, [
    (w: any) => WasmRasterizer.writeSetColorCommand(w, blue),
    (w: any) => WasmRasterizer.writeFillRectCommand(w, 10, 10, 10, 10),
  ]);

  // Third batch: draw green line
  const green = WasmRasterizer.makeARGB(255, 0, 255, 0);
  rasterizer.renderVariableLengthCommands(contextId, [
    (w: any) => WasmRasterizer.writeSetColorCommand(w, green),
    (w: any) => WasmRasterizer.writeDrawLineCommand(w, 0, 15, 29, 15),
  ]);

  // Verify results
  const pixels = rasterizer.copySurfacePixels(surfaceId);

  // Background should be red
  assertPixelEquals(pixels[0], red, "Pixel (0,0) should be red");

  // Blue square area (not on the line)
  const blueIdx = 12 * 30 + 15; // (15, 12) - in blue square, not on line
  assertPixelEquals(pixels[blueIdx], blue, "Pixel (15,12) should be blue");

  // Green line pixel at y=15 (should overwrite other colors or blend)
  const greenIdx = 15 * 30 + 5; // (5, 15) - on the line, outside blue square
  assertPixelEquals(
    pixels[greenIdx],
    green,
    "Pixel (5,15) should be green from line",
  );

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Error handling - invalid surface ID", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  assertThrows(
    () => {
      // Try to access an invalid surface ID
      rasterizer.getSurfacePixels(99999);
    },
    Error,
    "Invalid surface ID",
  );
});

Deno.test("Pixel format ARGB consistency", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  // Create surface with explicit ARGB format
  const surfaceId = rasterizer.allocateSurface(
    10,
    10,
    PixelFormat.PIXEL_FORMAT_ARGB,
  );
  const contextId = rasterizer.createContext(surfaceId);

  // Fill with a specific opaque color (alpha=255 to avoid blending)
  const testColor = WasmRasterizer.makeARGB(255, 100, 150, 75);
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, testColor),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 10, 10),
  ]);

  // Read back and verify
  const pixels = rasterizer.copySurfacePixels(surfaceId);
  const extracted = WasmRasterizer.extractARGB(pixels[0]);

  assertEquals(extracted.a, 255, "Alpha should be 255");
  assertEquals(extracted.r, 100, "Red should be 100");
  assertEquals(extracted.g, 150, "Green should be 150");
  assertEquals(extracted.b, 75, "Blue should be 75");

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});
