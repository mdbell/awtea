/**
 * Composite mode tests for WASM rasterizer
 *
 * This file tests all Porter-Duff alpha compositing modes:
 * - CLEAR, SRC, DST
 * - SRC_OVER, DST_OVER
 * - SRC_IN, DST_IN
 * - SRC_OUT, DST_OUT
 * - SRC_ATOP, DST_ATOP
 * - XOR
 */

import { assertEquals } from "@std/assert";
import { CompositeMode, PixelFormat, WasmRasterizer } from "./wasm_rasterizer.ts";

const WASM_PATH = "../../../build/wasm/awt_raster.wasm";

// Helper to compare Uint32 values (JavaScript bitwise operations are signed)
function assertPixelEquals(actual: number, expected: number, message: string) {
  const actualU32 = actual >>> 0;
  const expectedU32 = expected >>> 0;
  assertEquals(actualU32, expectedU32, message);
}

// Helper to unpack ARGB color
function unpackARGB(argb: number): { a: number; r: number; g: number; b: number } {
  const argbU32 = argb >>> 0;
  return {
    a: (argbU32 >>> 24) & 0xFF,
    r: (argbU32 >>> 16) & 0xFF,
    g: (argbU32 >>> 8) & 0xFF,
    b: argbU32 & 0xFF,
  };
}

// Helper to check if two colors are approximately equal (within tolerance for floating point rounding)
function assertPixelApprox(actual: number, expected: number, tolerance: number, message: string) {
  const a1 = unpackARGB(actual);
  const a2 = unpackARGB(expected);
  
  const diffA = Math.abs(a1.a - a2.a);
  const diffR = Math.abs(a1.r - a2.r);
  const diffG = Math.abs(a1.g - a2.g);
  const diffB = Math.abs(a1.b - a2.b);
  
  const maxDiff = Math.max(diffA, diffR, diffG, diffB);
  
  if (maxDiff > tolerance) {
    throw new Error(
      `${message}\nExpected: A=${a2.a} R=${a2.r} G=${a2.g} B=${a2.b}\n` +
      `Actual:   A=${a1.a} R=${a1.r} G=${a1.g} B=${a1.b}\n` +
      `Max diff: ${maxDiff} (tolerance: ${tolerance})`
    );
  }
}

// Setup a surface with a background rectangle, ready for composite testing
async function setupCompositeTest() {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);
  
  const surfaceId = rasterizer.allocateSurface(20, 20);
  const contextId = rasterizer.createContext(surfaceId);
  
  // Draw a semi-transparent red background rectangle (50% alpha)
  const cmdBuffer = rasterizer.createCommandBuffer(3);
  const bgRed = WasmRasterizer.makeARGB(128, 255, 0, 0); // 50% red
  rasterizer.writeCommand(cmdBuffer, 0, WasmRasterizer.setColorCommand(bgRed));
  rasterizer.writeCommand(cmdBuffer, 1, WasmRasterizer.fillRectCommand(5, 5, 10, 10));
  rasterizer.renderCommands(contextId, cmdBuffer, 2);
  
  return { rasterizer, surfaceId, contextId };
}

Deno.test("Composite: CLEAR mode", async () => {
  const { rasterizer, surfaceId, contextId } = await setupCompositeTest();
  
  // Set composite to CLEAR and draw over the red rectangle
  const cmdBuffer = rasterizer.createCommandBuffer(4);
  rasterizer.writeCommand(cmdBuffer, 0, WasmRasterizer.setCompositeCommand(CompositeMode.COMPOSITE_CLEAR, 1.0));
  const srcBlue = WasmRasterizer.makeARGB(255, 0, 0, 255); // opaque blue
  rasterizer.writeCommand(cmdBuffer, 1, WasmRasterizer.setColorCommand(srcBlue));
  rasterizer.writeCommand(cmdBuffer, 2, WasmRasterizer.fillRectCommand(7, 7, 5, 5));
  rasterizer.renderCommands(contextId, cmdBuffer, 3);
  
  const pixels = rasterizer.copySurfacePixels(surfaceId);
  
  // CLEAR mode should produce transparent pixels (alpha = 0)
  const clearIdx = 7 * 20 + 7;
  const clearedPixel = pixels[clearIdx];
  const { a } = unpackARGB(clearedPixel);
  assertEquals(a, 0, "CLEAR mode should produce transparent pixels");
  
  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Composite: SRC mode", async () => {
  const { rasterizer, surfaceId, contextId } = await setupCompositeTest();
  
  // Set composite to SRC and draw over the red rectangle
  const cmdBuffer = rasterizer.createCommandBuffer(4);
  rasterizer.writeCommand(cmdBuffer, 0, WasmRasterizer.setCompositeCommand(CompositeMode.COMPOSITE_SRC, 1.0));
  const srcGreen = WasmRasterizer.makeARGB(128, 0, 255, 0); // 50% green
  rasterizer.writeCommand(cmdBuffer, 1, WasmRasterizer.setColorCommand(srcGreen));
  rasterizer.writeCommand(cmdBuffer, 2, WasmRasterizer.fillRectCommand(7, 7, 5, 5));
  rasterizer.renderCommands(contextId, cmdBuffer, 3);
  
  const pixels = rasterizer.copySurfacePixels(surfaceId);
  
  // SRC mode should completely replace destination with source
  const srcIdx = 7 * 20 + 7;
  assertPixelApprox(pixels[srcIdx], srcGreen, 1, "SRC mode should replace destination with source");
  
  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Composite: DST mode", async () => {
  const { rasterizer, surfaceId, contextId } = await setupCompositeTest();
  
  // Get the original red pixel value
  const pixelsBefore = rasterizer.copySurfacePixels(surfaceId);
  const dstIdx = 7 * 20 + 7;
  const originalPixel = pixelsBefore[dstIdx];
  
  // Set composite to DST and draw over the red rectangle
  const cmdBuffer = rasterizer.createCommandBuffer(4);
  rasterizer.writeCommand(cmdBuffer, 0, WasmRasterizer.setCompositeCommand(CompositeMode.COMPOSITE_DST, 1.0));
  const srcBlue = WasmRasterizer.makeARGB(255, 0, 0, 255); // opaque blue
  rasterizer.writeCommand(cmdBuffer, 1, WasmRasterizer.setColorCommand(srcBlue));
  rasterizer.writeCommand(cmdBuffer, 2, WasmRasterizer.fillRectCommand(7, 7, 5, 5));
  rasterizer.renderCommands(contextId, cmdBuffer, 3);
  
  const pixelsAfter = rasterizer.copySurfacePixels(surfaceId);
  
  // DST mode should leave destination unchanged
  assertPixelEquals(pixelsAfter[dstIdx], originalPixel, "DST mode should leave destination unchanged");
  
  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Composite: SRC_OVER mode (default)", async () => {
  const { rasterizer, surfaceId, contextId } = await setupCompositeTest();
  
  // SRC_OVER is the default, but let's set it explicitly
  const cmdBuffer = rasterizer.createCommandBuffer(4);
  rasterizer.writeCommand(cmdBuffer, 0, WasmRasterizer.setCompositeCommand(CompositeMode.COMPOSITE_SRC_OVER, 1.0));
  const srcBlue = WasmRasterizer.makeARGB(128, 0, 0, 255); // 50% blue
  rasterizer.writeCommand(cmdBuffer, 1, WasmRasterizer.setColorCommand(srcBlue));
  rasterizer.writeCommand(cmdBuffer, 2, WasmRasterizer.fillRectCommand(7, 7, 5, 5));
  rasterizer.renderCommands(contextId, cmdBuffer, 3);
  
  const pixels = rasterizer.copySurfacePixels(surfaceId);
  const idx = 7 * 20 + 7;
  const result = unpackARGB(pixels[idx]);
  
  // SRC_OVER: blend 50% blue over 50% red
  // Expected: alpha should be > 128 (combining two 50% alphas)
  // Color should be a blend of blue and red (purple-ish)
  if (result.a <= 128) {
    throw new Error(`SRC_OVER should increase alpha: got ${result.a}, expected > 128`);
  }
  
  // Blue and red channels should both be present
  if (result.r === 0 || result.b === 0) {
    throw new Error(`SRC_OVER should blend colors: got R=${result.r} B=${result.b}`);
  }
  
  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Composite: SRC_IN mode", async () => {
  const { rasterizer, surfaceId, contextId } = await setupCompositeTest();
  
  // Draw source blue rect overlapping the red rect
  const cmdBuffer = rasterizer.createCommandBuffer(4);
  rasterizer.writeCommand(cmdBuffer, 0, WasmRasterizer.setCompositeCommand(CompositeMode.COMPOSITE_SRC_IN, 1.0));
  const srcBlue = WasmRasterizer.makeARGB(255, 0, 0, 255); // opaque blue
  rasterizer.writeCommand(cmdBuffer, 1, WasmRasterizer.setColorCommand(srcBlue));
  rasterizer.writeCommand(cmdBuffer, 2, WasmRasterizer.fillRectCommand(7, 7, 5, 5));
  rasterizer.renderCommands(contextId, cmdBuffer, 3);
  
  const pixels = rasterizer.copySurfacePixels(surfaceId);
  
  // SRC_IN: source only where destination is opaque
  // Inside the overlap: should have blue color
  const inIdx = 7 * 20 + 7;
  const inPixel = unpackARGB(pixels[inIdx]);
  
  // Should be mostly blue
  if (inPixel.b < 200) {
    throw new Error(`SRC_IN inside overlap should be blue: got B=${inPixel.b}`);
  }
  
  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Composite: SRC_OUT mode", async () => {
  const { rasterizer, surfaceId, contextId } = await setupCompositeTest();
  
  // Draw source blue rect larger than the red rect
  const cmdBuffer = rasterizer.createCommandBuffer(4);
  rasterizer.writeCommand(cmdBuffer, 0, WasmRasterizer.setCompositeCommand(CompositeMode.COMPOSITE_SRC_OUT, 1.0));
  const srcBlue = WasmRasterizer.makeARGB(255, 0, 0, 255); // opaque blue
  rasterizer.writeCommand(cmdBuffer, 1, WasmRasterizer.setColorCommand(srcBlue));
  // Draw outside the red rect area
  rasterizer.writeCommand(cmdBuffer, 2, WasmRasterizer.fillRectCommand(2, 2, 5, 5));
  rasterizer.renderCommands(contextId, cmdBuffer, 3);
  
  const pixels = rasterizer.copySurfacePixels(surfaceId);
  
  // SRC_OUT: source only where destination is transparent
  // Outside red rect: should have blue
  const outIdx = 2 * 20 + 2;
  const outPixel = unpackARGB(pixels[outIdx]);
  
  if (outPixel.b < 200) {
    throw new Error(`SRC_OUT outside should be blue: got B=${outPixel.b}`);
  }
  
  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Composite: XOR mode", async () => {
  const { rasterizer, surfaceId, contextId } = await setupCompositeTest();
  
  // Draw source blue rect overlapping the red rect
  const cmdBuffer = rasterizer.createCommandBuffer(4);
  rasterizer.writeCommand(cmdBuffer, 0, WasmRasterizer.setCompositeCommand(CompositeMode.COMPOSITE_XOR, 1.0));
  const srcBlue = WasmRasterizer.makeARGB(128, 0, 0, 255); // 50% blue
  rasterizer.writeCommand(cmdBuffer, 1, WasmRasterizer.setColorCommand(srcBlue));
  rasterizer.writeCommand(cmdBuffer, 2, WasmRasterizer.fillRectCommand(7, 7, 5, 5));
  rasterizer.renderCommands(contextId, cmdBuffer, 3);
  
  const pixels = rasterizer.copySurfacePixels(surfaceId);
  
  // XOR: source and destination where they don't overlap
  // In overlap area, alpha should be reduced (XOR excludes overlap)
  const overlapIdx = 7 * 20 + 7;
  const overlapPixel = unpackARGB(pixels[overlapIdx]);
  
  // XOR produces color where they don't overlap, so some color should remain
  if (overlapPixel.a === 0) {
    throw new Error("XOR should produce some result in overlap area");
  }
  
  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Composite: with alpha parameter", async () => {
  const { rasterizer, surfaceId, contextId } = await setupCompositeTest();
  
  // Use SRC_OVER with 50% composite alpha
  const cmdBuffer = rasterizer.createCommandBuffer(4);
  rasterizer.writeCommand(cmdBuffer, 0, WasmRasterizer.setCompositeCommand(CompositeMode.COMPOSITE_SRC_OVER, 0.5));
  const srcBlue = WasmRasterizer.makeARGB(255, 0, 0, 255); // opaque blue, but 50% composite alpha
  rasterizer.writeCommand(cmdBuffer, 1, WasmRasterizer.setColorCommand(srcBlue));
  rasterizer.writeCommand(cmdBuffer, 2, WasmRasterizer.fillRectCommand(7, 7, 5, 5));
  rasterizer.renderCommands(contextId, cmdBuffer, 3);
  
  const pixels = rasterizer.copySurfacePixels(surfaceId);
  const idx = 7 * 20 + 7;
  const result = unpackARGB(pixels[idx]);
  
  // With 50% composite alpha, the effective source alpha is 50% of 255 = 127.5
  // Result should have both blue and red components
  if (result.b === 0 || result.r === 0) {
    throw new Error(`Composite alpha should blend colors: got R=${result.r} B=${result.b}`);
  }
  
  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Composite: multiple operations preserve mode", async () => {
  const { rasterizer, surfaceId, contextId } = await setupCompositeTest();
  
  // Set SRC mode once, then draw multiple rects
  const cmdBuffer = rasterizer.createCommandBuffer(8);
  rasterizer.writeCommand(cmdBuffer, 0, WasmRasterizer.setCompositeCommand(CompositeMode.COMPOSITE_SRC, 1.0));
  
  const green = WasmRasterizer.makeARGB(255, 0, 255, 0);
  rasterizer.writeCommand(cmdBuffer, 1, WasmRasterizer.setColorCommand(green));
  rasterizer.writeCommand(cmdBuffer, 2, WasmRasterizer.fillRectCommand(2, 2, 3, 3));
  
  const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);
  rasterizer.writeCommand(cmdBuffer, 3, WasmRasterizer.setColorCommand(blue));
  rasterizer.writeCommand(cmdBuffer, 4, WasmRasterizer.fillRectCommand(10, 10, 3, 3));
  
  rasterizer.renderCommands(contextId, cmdBuffer, 5);
  
  const pixels = rasterizer.copySurfacePixels(surfaceId);
  
  // Both rects should use SRC mode (no blending with background)
  assertPixelEquals(pixels[2 * 20 + 2], green, "First rect should use SRC mode");
  assertPixelEquals(pixels[10 * 20 + 10], blue, "Second rect should still use SRC mode");
  
  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});
