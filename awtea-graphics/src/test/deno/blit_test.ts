/**
 * Blit operation tests for WASM rasterizer
 *
 * This file tests image blitting operations:
 * - Basic blitting (same/different formats)
 * - Blend modes during blit (SRC, SRC_OVER)
 * - Opaque vs transparent source images
 * - Transform blitting
 * - Performance scenarios
 */

import { assertEquals } from "https://deno.land/std@0.224.0/assert/mod.ts";
import {
  CompositeMode,
  PixelFormat,
  WasmRasterizer,
} from "./wasm_rasterizer.ts";
import {
  assertPixelApprox,
  assertPixelEquals,
  unpackARGB,
} from "./test_helpers.ts";

const WASM_PATH = "../../../build/wasm/awt_raster.wasm";

Deno.test("Blit: Basic same-format opaque blit", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  // Create source image (10x10) with red pixels
  const srcId = rasterizer.allocateSurface(10, 10, PixelFormat.PIXEL_FORMAT_ARGB);
  const srcCtx = rasterizer.createContext(srcId);
  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
  rasterizer.renderVariableLengthCommands(srcCtx, [
    (w) => WasmRasterizer.writeSetColorCommand(w, red),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 10, 10),
  ]);

  // Create destination surface (30x30) with blue background
  const dstId = rasterizer.allocateSurface(30, 30, PixelFormat.PIXEL_FORMAT_ARGB);
  const dstCtx = rasterizer.createContext(dstId);
  const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);
  rasterizer.renderVariableLengthCommands(dstCtx, [
    (w) => WasmRasterizer.writeSetColorCommand(w, blue),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 30, 30),
  ]);

  // Blit source to destination at (5, 5)
  rasterizer.renderVariableLengthCommands(dstCtx, [
    (w) => WasmRasterizer.writeBlitImageCommand(w, srcId, 5, 5),
  ]);

  const pixels = rasterizer.copySurfacePixels(dstId);

  // Check that area outside blit is still blue
  assertPixelEquals(pixels[0], blue, "Pixel (0,0) should still be blue");
  assertPixelEquals(pixels[4 * 30 + 4], blue, "Pixel (4,4) should still be blue");

  // Check that blitted area is red (10x10 starting at 5,5)
  assertPixelEquals(pixels[5 * 30 + 5], red, "Pixel (5,5) should be red");
  assertPixelEquals(pixels[10 * 30 + 10], red, "Pixel (10,10) should be red");
  assertPixelEquals(pixels[14 * 30 + 14], red, "Pixel (14,14) should be red");

  // Check that area after blit is still blue
  assertPixelEquals(pixels[15 * 30 + 15], blue, "Pixel (15,15) should still be blue");

  rasterizer.destroyContext(srcCtx);
  rasterizer.destroyContext(dstCtx);
  rasterizer.freeSurface(srcId);
  rasterizer.freeSurface(dstId);
});

Deno.test("Blit: SRC mode opaque source (should replace destination)", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  // Create source image (5x5) with opaque green
  const srcId = rasterizer.allocateSurface(5, 5, PixelFormat.PIXEL_FORMAT_ARGB);
  const srcCtx = rasterizer.createContext(srcId);
  const green = WasmRasterizer.makeARGB(255, 0, 255, 0);
  rasterizer.renderVariableLengthCommands(srcCtx, [
    (w) => WasmRasterizer.writeSetColorCommand(w, green),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 5, 5),
  ]);

  // Create destination surface (20x20) with red background
  const dstId = rasterizer.allocateSurface(20, 20, PixelFormat.PIXEL_FORMAT_ARGB);
  const dstCtx = rasterizer.createContext(dstId);
  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
  rasterizer.renderVariableLengthCommands(dstCtx, [
    (w) => WasmRasterizer.writeSetColorCommand(w, red),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 20, 20),
  ]);

  // Blit with SRC mode (should replace, not blend)
  rasterizer.renderVariableLengthCommands(dstCtx, [
    (w) => WasmRasterizer.writeCompositCommand(w, CompositeMode.COMPOSITE_SRC, 1.0),
    (w) => WasmRasterizer.writeBlitImageCommand(w, srcId, 7, 7),
  ]);

  const pixels = rasterizer.copySurfacePixels(dstId);

  // Check that blitted area is exactly green (no blending with red background)
  assertPixelEquals(pixels[7 * 20 + 7], green, "Pixel (7,7) should be green (SRC mode)");
  assertPixelEquals(pixels[10 * 20 + 10], green, "Pixel (10,10) should be green (SRC mode)");
  
  // Outside blit area should still be red
  assertPixelEquals(pixels[0], red, "Pixel (0,0) should still be red");

  rasterizer.destroyContext(srcCtx);
  rasterizer.destroyContext(dstCtx);
  rasterizer.freeSurface(srcId);
  rasterizer.freeSurface(dstId);
});

Deno.test("Blit: SRC_OVER mode with semi-transparent source", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  // Create source image (5x5) with 50% transparent blue
  const srcId = rasterizer.allocateSurface(5, 5, PixelFormat.PIXEL_FORMAT_ARGB);
  const srcCtx = rasterizer.createContext(srcId);
  const semiBlue = WasmRasterizer.makeARGB(128, 0, 0, 255); // 50% alpha
  rasterizer.renderVariableLengthCommands(srcCtx, [
    (w) => WasmRasterizer.writeSetColorCommand(w, semiBlue),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 5, 5),
  ]);

  // Create destination surface (20x20) with opaque red background
  const dstId = rasterizer.allocateSurface(20, 20, PixelFormat.PIXEL_FORMAT_ARGB);
  const dstCtx = rasterizer.createContext(dstId);
  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
  rasterizer.renderVariableLengthCommands(dstCtx, [
    (w) => WasmRasterizer.writeSetColorCommand(w, red),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 20, 20),
  ]);

  // Blit with SRC_OVER mode (default, should blend)
  rasterizer.renderVariableLengthCommands(dstCtx, [
    (w) => WasmRasterizer.writeCompositCommand(w, CompositeMode.COMPOSITE_SRC_OVER, 1.0),
    (w) => WasmRasterizer.writeBlitImageCommand(w, srcId, 5, 5),
  ]);

  const pixels = rasterizer.copySurfacePixels(dstId);

  // Calculate expected blend: 50% blue over 100% red
  // Result = src*srcA + dst*dstA*(1-srcA)
  // For color channels: (0*128 + 255*255*(1-128/255)) / resultAlpha
  // resultAlpha = 128/255 + 255/255*(1-128/255) = 0.5 + 1.0*0.5 = 1.0 (in normalized)
  // So resultAlpha = 255
  // R = (0*0.5 + 255*1.0*0.5) / 1.0 = 127.5 ≈ 128
  // G = (0*0.5 + 0*1.0*0.5) / 1.0 = 0
  // B = (255*0.5 + 0*1.0*0.5) / 1.0 = 127.5 ≈ 128
  const expectedBlended = WasmRasterizer.makeARGB(255, 128, 0, 128);

  const blittedPixel = pixels[7 * 20 + 7];
  assertPixelApprox(
    blittedPixel,
    expectedBlended,
    2, // Tolerance of 2 for rounding differences
    "Pixel (7,7) should be blended purple (50% blue over red)"
  );

  rasterizer.destroyContext(srcCtx);
  rasterizer.destroyContext(dstCtx);
  rasterizer.freeSurface(srcId);
  rasterizer.freeSurface(dstId);
});

Deno.test("Blit: SRC_OVER mode with fully opaque source", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  // Create source image (8x8) with opaque yellow
  const srcId = rasterizer.allocateSurface(8, 8, PixelFormat.PIXEL_FORMAT_ARGB);
  const srcCtx = rasterizer.createContext(srcId);
  const yellow = WasmRasterizer.makeARGB(255, 255, 255, 0);
  rasterizer.renderVariableLengthCommands(srcCtx, [
    (w) => WasmRasterizer.writeSetColorCommand(w, yellow),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 8, 8),
  ]);

  // Create destination surface (25x25) with cyan background
  const dstId = rasterizer.allocateSurface(25, 25, PixelFormat.PIXEL_FORMAT_ARGB);
  const dstCtx = rasterizer.createContext(dstId);
  const cyan = WasmRasterizer.makeARGB(255, 0, 255, 255);
  rasterizer.renderVariableLengthCommands(dstCtx, [
    (w) => WasmRasterizer.writeSetColorCommand(w, cyan),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 25, 25),
  ]);

  // Blit with SRC_OVER mode
  rasterizer.renderVariableLengthCommands(dstCtx, [
    (w) => WasmRasterizer.writeCompositCommand(w, CompositeMode.COMPOSITE_SRC_OVER, 1.0),
    (w) => WasmRasterizer.writeBlitImageCommand(w, srcId, 10, 10),
  ]);

  const pixels = rasterizer.copySurfacePixels(dstId);

  // With fully opaque source in SRC_OVER, result should be exactly the source color
  // (no blending needed since srcAlpha = 1.0)
  assertPixelEquals(
    pixels[12 * 25 + 12],
    yellow,
    "Pixel (12,12) should be yellow (opaque source in SRC_OVER)"
  );

  // Outside blit area should still be cyan
  assertPixelEquals(pixels[5 * 25 + 5], cyan, "Pixel (5,5) should still be cyan");

  rasterizer.destroyContext(srcCtx);
  rasterizer.destroyContext(dstCtx);
  rasterizer.freeSurface(srcId);
  rasterizer.freeSurface(dstId);
});

Deno.test("Blit: Different pixel formats (ARGB to RGB)", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  // Create source image with alpha channel (ARGB)
  const srcId = rasterizer.allocateSurface(6, 6, PixelFormat.PIXEL_FORMAT_ARGB);
  const srcCtx = rasterizer.createContext(srcId);
  const magenta = WasmRasterizer.makeARGB(255, 255, 0, 255);
  rasterizer.renderVariableLengthCommands(srcCtx, [
    (w) => WasmRasterizer.writeSetColorCommand(w, magenta),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 6, 6),
  ]);

  // Create destination without alpha channel (RGB)
  const dstId = rasterizer.allocateSurface(20, 20, PixelFormat.PIXEL_FORMAT_RGB);
  const dstCtx = rasterizer.createContext(dstId);
  const white = WasmRasterizer.makeARGB(255, 255, 255, 255);
  rasterizer.renderVariableLengthCommands(dstCtx, [
    (w) => WasmRasterizer.writeSetColorCommand(w, white),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 20, 20),
  ]);

  // Blit ARGB source to RGB destination
  rasterizer.renderVariableLengthCommands(dstCtx, [
    (w) => WasmRasterizer.writeBlitImageCommand(w, srcId, 7, 7),
  ]);

  const pixels = rasterizer.copySurfacePixels(dstId);

  // RGB format masks out alpha, so we only check RGB components
  const result = pixels[9 * 20 + 9];
  const extracted = unpackARGB(result);
  
  // Magenta RGB components should be preserved (R=255, G=0, B=255)
  assertEquals(extracted.r, 255, "Red component should be 255");
  assertEquals(extracted.g, 0, "Green component should be 0");
  assertEquals(extracted.b, 255, "Blue component should be 255");

  rasterizer.destroyContext(srcCtx);
  rasterizer.destroyContext(dstCtx);
  rasterizer.freeSurface(srcId);
  rasterizer.freeSurface(dstId);
});

Deno.test("Blit: Large image performance test", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  // Create larger source image (100x100)
  const srcId = rasterizer.allocateSurface(100, 100, PixelFormat.PIXEL_FORMAT_ARGB);
  const srcCtx = rasterizer.createContext(srcId);
  const orange = WasmRasterizer.makeARGB(255, 255, 165, 0);
  rasterizer.renderVariableLengthCommands(srcCtx, [
    (w) => WasmRasterizer.writeSetColorCommand(w, orange),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 100, 100),
  ]);

  // Create large destination (256x256)
  const dstId = rasterizer.allocateSurface(256, 256, PixelFormat.PIXEL_FORMAT_ARGB);
  const dstCtx = rasterizer.createContext(dstId);
  const gray = WasmRasterizer.makeARGB(255, 128, 128, 128);
  rasterizer.renderVariableLengthCommands(dstCtx, [
    (w) => WasmRasterizer.writeSetColorCommand(w, gray),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 256, 256),
  ]);

  // Blit large image (should use fast path)
  const startTime = performance.now();
  rasterizer.renderVariableLengthCommands(dstCtx, [
    (w) => WasmRasterizer.writeBlitImageCommand(w, srcId, 50, 50),
  ]);
  const endTime = performance.now();

  const pixels = rasterizer.copySurfacePixels(dstId);

  // Verify correctness
  assertPixelEquals(pixels[75 * 256 + 75], orange, "Pixel (75,75) should be orange");
  assertPixelEquals(pixels[149 * 256 + 149], orange, "Pixel (149,149) should be orange");
  assertPixelEquals(pixels[25 * 256 + 25], gray, "Pixel (25,25) should still be gray");

  // Just log timing (not asserting, as this is informational)
  console.log(`Large blit (100x100) took: ${(endTime - startTime).toFixed(2)}ms`);

  rasterizer.destroyContext(srcCtx);
  rasterizer.destroyContext(dstCtx);
  rasterizer.freeSurface(srcId);
  rasterizer.freeSurface(dstId);
});

Deno.test("Blit: Clipping at edges", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  // Create source image (10x10)
  const srcId = rasterizer.allocateSurface(10, 10, PixelFormat.PIXEL_FORMAT_ARGB);
  const srcCtx = rasterizer.createContext(srcId);
  const purple = WasmRasterizer.makeARGB(255, 128, 0, 128);
  rasterizer.renderVariableLengthCommands(srcCtx, [
    (w) => WasmRasterizer.writeSetColorCommand(w, purple),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 10, 10),
  ]);

  // Create small destination (15x15)
  const dstId = rasterizer.allocateSurface(15, 15, PixelFormat.PIXEL_FORMAT_ARGB);
  const dstCtx = rasterizer.createContext(dstId);
  const black = WasmRasterizer.makeARGB(255, 0, 0, 0);
  rasterizer.renderVariableLengthCommands(dstCtx, [
    (w) => WasmRasterizer.writeSetColorCommand(w, black),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 15, 15),
  ]);

  // Blit source partially off the edge (x=10, y=10)
  // Only top-left 5x5 of source should be visible
  rasterizer.renderVariableLengthCommands(dstCtx, [
    (w) => WasmRasterizer.writeBlitImageCommand(w, srcId, 10, 10),
  ]);

  const pixels = rasterizer.copySurfacePixels(dstId);

  // Check that visible portion is purple
  assertPixelEquals(pixels[10 * 15 + 10], purple, "Pixel (10,10) should be purple");
  assertPixelEquals(pixels[14 * 15 + 14], purple, "Pixel (14,14) should be purple");

  // Check that area not covered by blit is still black
  assertPixelEquals(pixels[5 * 15 + 5], black, "Pixel (5,5) should still be black");
  assertPixelEquals(pixels[9 * 15 + 9], black, "Pixel (9,9) should still be black");

  rasterizer.destroyContext(srcCtx);
  rasterizer.destroyContext(dstCtx);
  rasterizer.freeSurface(srcId);
  rasterizer.freeSurface(dstId);
});

Deno.test("Blit: Source with mixed transparency", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  // Create source image (10x10) with pattern: transparent left, opaque right
  const srcId = rasterizer.allocateSurface(10, 10, PixelFormat.PIXEL_FORMAT_ARGB);
  const srcCtx = rasterizer.createContext(srcId);
  
  const transparent = WasmRasterizer.makeARGB(0, 0, 0, 0); // Fully transparent
  const opaqueGreen = WasmRasterizer.makeARGB(255, 0, 255, 0); // Opaque green

  // Fill left half with transparent, right half with opaque green
  rasterizer.renderVariableLengthCommands(srcCtx, [
    (w) => WasmRasterizer.writeSetColorCommand(w, transparent),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 5, 10),
    (w) => WasmRasterizer.writeSetColorCommand(w, opaqueGreen),
    (w) => WasmRasterizer.writeFillRectCommand(w, 5, 0, 5, 10),
  ]);

  // Create destination with red background
  const dstId = rasterizer.allocateSurface(30, 30, PixelFormat.PIXEL_FORMAT_ARGB);
  const dstCtx = rasterizer.createContext(dstId);
  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
  rasterizer.renderVariableLengthCommands(dstCtx, [
    (w) => WasmRasterizer.writeSetColorCommand(w, red),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 30, 30),
  ]);

  // Blit with SRC_OVER mode
  rasterizer.renderVariableLengthCommands(dstCtx, [
    (w) => WasmRasterizer.writeCompositCommand(w, CompositeMode.COMPOSITE_SRC_OVER, 1.0),
    (w) => WasmRasterizer.writeBlitImageCommand(w, srcId, 10, 10),
  ]);

  const pixels = rasterizer.copySurfacePixels(dstId);

  // Left side of blitted area (transparent source) should show background red
  assertPixelEquals(
    pixels[12 * 30 + 12],
    red,
    "Pixel (12,12) should be red (transparent source shows through)"
  );

  // Right side of blitted area (opaque green) should be green
  assertPixelEquals(
    pixels[12 * 30 + 17],
    opaqueGreen,
    "Pixel (17,12) should be green (opaque source)"
  );

  rasterizer.destroyContext(srcCtx);
  rasterizer.destroyContext(dstCtx);
  rasterizer.freeSurface(srcId);
  rasterizer.freeSurface(dstId);
});
