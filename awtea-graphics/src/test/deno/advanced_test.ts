/**
 * Advanced tests for WASM rasterizer
 * 
 * This file demonstrates more complex scenarios:
 * - Image registration and blitting
 * - Transforms
 * - Complex drawing patterns
 */

import { assertEquals, assertThrows } from "https://deno.land/std@0.224.0/assert/mod.ts";
import { WasmRasterizer, PixelFormat } from "./wasm_rasterizer.ts";

const WASM_PATH = "../../../build/wasm/awt_raster.wasm";

// Helper to compare Uint32 values (JavaScript bitwise operations are signed)
function assertPixelEquals(actual: number, expected: number, message: string) {
  const actualU32 = (actual >>> 0);
  const expectedU32 = (expected >>> 0);
  assertEquals(actualU32, expectedU32, message);
}

Deno.test("Image registration and blitting", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  // Register a small 5x5 image
  const imageId = rasterizer.registerImage(5, 5);
  
  // Fill the image with a pattern
  const imagePixels = rasterizer.getImagePixels(imageId, 5, 5);
  const magenta = WasmRasterizer.makeARGB(255, 255, 0, 255);
  for (let i = 0; i < imagePixels.length; i++) {
    imagePixels[i] = magenta;
  }

  // Create a surface
  const surfaceId = rasterizer.allocateSurface(20, 20);

  // Blit the image onto the surface at position (5, 5)
  const cmdBuffer = rasterizer.createCommandBuffer(1);
  rasterizer.writeCommand(cmdBuffer, 0, WasmRasterizer.blitImageCommand(imageId, 5, 5));
  rasterizer.renderCommands(surfaceId, cmdBuffer, 1);

  // Check that pixels at (5, 5) area are magenta
  const surfacePixels = rasterizer.copySurfacePixels(surfaceId);
  const blitIdx = 5 * 20 + 5;
  assertPixelEquals(surfacePixels[blitIdx], magenta, "Pixel (5,5) should be magenta from blit");

  // Check that pixels at (9, 9) (last pixel of blit) are also magenta
  const blitEndIdx = 9 * 20 + 9;
  assertPixelEquals(surfacePixels[blitEndIdx], magenta, "Pixel (9,9) should be magenta from blit");

  // Check that pixels outside blit area are still 0
  assertPixelEquals(surfacePixels[0], 0, "Pixel (0,0) should be 0 (outside blit)");

  rasterizer.freeSurface(surfaceId);
});

Deno.test("Identity transform", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(20, 20);

  // Create command buffer with identity transform
  const cmdBuffer = rasterizer.createCommandBuffer(3);

  // Set identity transform (m00=1, m11=1, rest=0)
  rasterizer.writeCommand(cmdBuffer, 0, WasmRasterizer.setTransformCommand(1, 0, 0, 0, 1, 0));

  // Set color and fill rect
  const cyan = WasmRasterizer.makeARGB(255, 0, 255, 255);
  rasterizer.writeCommand(cmdBuffer, 1, WasmRasterizer.setColorCommand(cyan));
  rasterizer.writeCommand(cmdBuffer, 2, WasmRasterizer.fillRectCommand(5, 5, 10, 10));

  // Execute
  rasterizer.renderCommands(surfaceId, cmdBuffer, 3);

  // With identity transform, rect should be at (5, 5)
  const pixels = rasterizer.copySurfacePixels(surfaceId);
  const idx = 5 * 20 + 5;
  assertPixelEquals(pixels[idx], cyan, "Pixel (5,5) should be cyan with identity transform");

  rasterizer.freeSurface(surfaceId);
});

Deno.test("Translation transform", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(30, 30);

  // Create command buffer with translation
  const cmdBuffer = rasterizer.createCommandBuffer(3);

  // Set translation transform (move by +5, +5)
  // m00=1, m01=0, m02=5, m10=0, m11=1, m12=5
  rasterizer.writeCommand(cmdBuffer, 0, WasmRasterizer.setTransformCommand(1, 0, 5, 0, 1, 5));

  // Set color and fill rect at (0, 0)
  const orange = WasmRasterizer.makeARGB(255, 255, 165, 0);
  rasterizer.writeCommand(cmdBuffer, 1, WasmRasterizer.setColorCommand(orange));
  rasterizer.writeCommand(cmdBuffer, 2, WasmRasterizer.fillRectCommand(0, 0, 5, 5));

  // Execute
  rasterizer.renderCommands(surfaceId, cmdBuffer, 3);

  // With translation, rect at (0,0) should appear at (5, 5)
  const pixels = rasterizer.copySurfacePixels(surfaceId);
  
  // Original position should be black
  assertPixelEquals(pixels[0], 0, "Pixel (0,0) should be 0 (not drawn there)");
  
  // Translated position should have color
  const translatedIdx = 5 * 30 + 5;
  assertPixelEquals(pixels[translatedIdx], orange, "Pixel (5,5) should be orange (translated)");

  rasterizer.freeSurface(surfaceId);
});

Deno.test("Complex drawing pattern - checkerboard", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const width = 20;
  const height = 20;
  const cellSize = 5;
  const surfaceId = rasterizer.allocateSurface(width, height);

  const black = WasmRasterizer.makeARGB(255, 0, 0, 0);
  const white = WasmRasterizer.makeARGB(255, 255, 255, 255);

  // Create checkerboard pattern (4x4 cells)
  const numCells = (width / cellSize) * (height / cellSize);
  const cmdBuffer = rasterizer.createCommandBuffer(numCells * 2); // 2 commands per cell (set color + fill)

  let cmdIdx = 0;
  for (let row = 0; row < height / cellSize; row++) {
    for (let col = 0; col < width / cellSize; col++) {
      const color = ((row + col) % 2 === 0) ? black : white;
      rasterizer.writeCommand(cmdBuffer, cmdIdx++, WasmRasterizer.setColorCommand(color));
      rasterizer.writeCommand(cmdBuffer, cmdIdx++, 
        WasmRasterizer.fillRectCommand(col * cellSize, row * cellSize, cellSize, cellSize));
    }
  }

  rasterizer.renderCommands(surfaceId, cmdBuffer, cmdIdx);

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

  rasterizer.freeSurface(surfaceId);
});

Deno.test("Drawing with background color", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(10, 10);

  // Create command buffer
  const cmdBuffer = rasterizer.createCommandBuffer(4);

  // Set background color to gray
  const gray = WasmRasterizer.makeARGB(255, 128, 128, 128);
  rasterizer.writeCommand(cmdBuffer, 0, WasmRasterizer.setColorCommand(gray, 1)); // which=1 for BG

  // Set foreground color to red
  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
  rasterizer.writeCommand(cmdBuffer, 1, WasmRasterizer.setColorCommand(red, 0)); // which=0 for FG

  // Fill with foreground
  rasterizer.writeCommand(cmdBuffer, 2, WasmRasterizer.fillRectCommand(0, 0, 10, 10));

  // Clear a small area (which should use background color or transparency)
  rasterizer.writeCommand(cmdBuffer, 3, WasmRasterizer.clearRectCommand(3, 3, 4, 4));

  rasterizer.renderCommands(surfaceId, cmdBuffer, 4);

  const pixels = rasterizer.copySurfacePixels(surfaceId);

  // Filled area should be red
  assertPixelEquals(pixels[0], red, "Pixel (0,0) should be red");

  // Cleared area should be background color (gray as set above)
  const clearedIdx = 3 * 10 + 3;
  assertPixelEquals(pixels[clearedIdx], gray, "Pixel (3,3) should be gray (background color)");

  rasterizer.freeSurface(surfaceId);
});

Deno.test("Large surface allocation", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  // Allocate a larger surface (256x256)
  const surfaceId = rasterizer.allocateSurface(256, 256);

  const dims = rasterizer.getSurfaceDimensions(surfaceId);
  assertEquals(dims.width, 256);
  assertEquals(dims.height, 256);

  // Just fill the whole surface with mid-gray to test large allocation works
  const midGray = WasmRasterizer.makeARGB(255, 128, 128, 128);
  const testBuffer = rasterizer.createCommandBuffer(2);
  rasterizer.writeCommand(testBuffer, 0, WasmRasterizer.setColorCommand(midGray));
  rasterizer.writeCommand(testBuffer, 1, WasmRasterizer.fillRectCommand(0, 0, 256, 256));
  rasterizer.renderCommands(surfaceId, testBuffer, 2);

  const pixels = rasterizer.copySurfacePixels(surfaceId);
  assertEquals(pixels.length, 256 * 256, "Should have 65536 pixels");
  assertPixelEquals(pixels[0], midGray, "First pixel should be mid-gray");
  assertPixelEquals(pixels[pixels.length - 1], midGray, "Last pixel should be mid-gray");

  rasterizer.freeSurface(surfaceId);
});

Deno.test("Multiple command batches", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(30, 30);

  // First batch: fill with red
  const cmdBuffer1 = rasterizer.createCommandBuffer(2);
  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
  rasterizer.writeCommand(cmdBuffer1, 0, WasmRasterizer.setColorCommand(red));
  rasterizer.writeCommand(cmdBuffer1, 1, WasmRasterizer.fillRectCommand(0, 0, 30, 30));
  rasterizer.renderCommands(surfaceId, cmdBuffer1, 2);

  // Second batch: draw blue square on top
  const cmdBuffer2 = rasterizer.createCommandBuffer(2);
  const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);
  rasterizer.writeCommand(cmdBuffer2, 0, WasmRasterizer.setColorCommand(blue));
  rasterizer.writeCommand(cmdBuffer2, 1, WasmRasterizer.fillRectCommand(10, 10, 10, 10));
  rasterizer.renderCommands(surfaceId, cmdBuffer2, 2);

  // Third batch: draw green line
  const cmdBuffer3 = rasterizer.createCommandBuffer(2);
  const green = WasmRasterizer.makeARGB(255, 0, 255, 0);
  rasterizer.writeCommand(cmdBuffer3, 0, WasmRasterizer.setColorCommand(green));
  rasterizer.writeCommand(cmdBuffer3, 1, WasmRasterizer.drawLineCommand(0, 15, 29, 15));
  rasterizer.renderCommands(surfaceId, cmdBuffer3, 2);

  // Verify results
  const pixels = rasterizer.copySurfacePixels(surfaceId);

  // Background should be red
  assertPixelEquals(pixels[0], red, "Pixel (0,0) should be red");

  // Blue square area (not on the line)
  const blueIdx = 12 * 30 + 15; // (15, 12) - in blue square, not on line
  assertPixelEquals(pixels[blueIdx], blue, "Pixel (15,12) should be blue");

  // Green line pixel at y=15 (should overwrite other colors or blend)
  const greenIdx = 15 * 30 + 5; // (5, 15) - on the line, outside blue square
  assertPixelEquals(pixels[greenIdx], green, "Pixel (5,15) should be green from line");

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
    "Invalid surface ID"
  );
});

Deno.test("Pixel format ARGB consistency", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  // Create surface with explicit ARGB format
  const surfaceId = rasterizer.allocateSurface(10, 10, PixelFormat.ARGB);

  // Fill with a specific opaque color (alpha=255 to avoid blending)
  const testColor = WasmRasterizer.makeARGB(255, 100, 150, 75);
  const cmdBuffer = rasterizer.createCommandBuffer(2);
  rasterizer.writeCommand(cmdBuffer, 0, WasmRasterizer.setColorCommand(testColor));
  rasterizer.writeCommand(cmdBuffer, 1, WasmRasterizer.fillRectCommand(0, 0, 10, 10));
  rasterizer.renderCommands(surfaceId, cmdBuffer, 2);

  // Read back and verify
  const pixels = rasterizer.copySurfacePixels(surfaceId);
  const extracted = WasmRasterizer.extractARGB(pixels[0]);

  assertEquals(extracted.a, 255, "Alpha should be 255");
  assertEquals(extracted.r, 100, "Red should be 100");
  assertEquals(extracted.g, 150, "Green should be 150");
  assertEquals(extracted.b, 75, "Blue should be 75");

  rasterizer.freeSurface(surfaceId);
});
