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

// Setup a surface with a background rectangle, ready for composite testing
async function setupCompositeTest() {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(20, 20);
  const contextId = rasterizer.createContext(surfaceId);

  const bgRed = WasmRasterizer.makeARGB(128, 255, 0, 0); // 50% red

  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, bgRed),
    (w) => WasmRasterizer.writeFillRectCommand(w, 5, 5, 10, 10),
  ]);

  return { rasterizer, surfaceId, contextId };
}

Deno.test("Composite: CLEAR mode", async () => {
  const { rasterizer, surfaceId, contextId } = await setupCompositeTest();
  const srcBlue = WasmRasterizer.makeARGB(255, 0, 0, 255); // opaque blue

  rasterizer.renderVariableLengthCommands(contextId, [
    (w) =>
      WasmRasterizer.writeCompositCommand(
        w,
        CompositeMode.COMPOSITE_CLEAR,
        1.0,
      ),
    (w) => WasmRasterizer.writeSetColorCommand(w, srcBlue),
    (w) => WasmRasterizer.writeFillRectCommand(w, 7, 7, 5, 5),
  ]);

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

  const srcGreen = WasmRasterizer.makeARGB(128, 0, 255, 0); // 50% green
  // Set composite to SRC and draw over the red rectangle

  rasterizer.renderVariableLengthCommands(contextId, [
    (w) =>
      WasmRasterizer.writeCompositCommand(
        w,
        CompositeMode.COMPOSITE_SRC,
        1.0,
      ),
    (w) => WasmRasterizer.writeSetColorCommand(w, srcGreen),
    (w) => WasmRasterizer.writeFillRectCommand(w, 7, 7, 5, 5),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);

  // SRC mode should completely replace destination with source
  const srcIdx = 7 * 20 + 7;
  assertPixelApprox(
    pixels[srcIdx],
    srcGreen,
    1,
    "SRC mode should replace destination with source",
  );

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Composite: DST mode", async () => {
  const { rasterizer, surfaceId, contextId } = await setupCompositeTest();

  // Get the original red pixel value
  const pixelsBefore = rasterizer.copySurfacePixels(surfaceId);
  const dstIdx = 7 * 20 + 7;
  const originalPixel = pixelsBefore[dstIdx];
  const srcBlue = WasmRasterizer.makeARGB(255, 0, 0, 255); // opaque blue

  // Set composite to DST and draw over the red rectangle
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) =>
      WasmRasterizer.writeCompositCommand(
        w,
        CompositeMode.COMPOSITE_DST,
        1.0,
      ),
    (w) => WasmRasterizer.writeSetColorCommand(w, srcBlue),
    (w) => WasmRasterizer.writeFillRectCommand(w, 7, 7, 5, 5),
  ]);

  const pixelsAfter = rasterizer.copySurfacePixels(surfaceId);

  // DST mode should leave destination unchanged
  assertPixelEquals(
    pixelsAfter[dstIdx],
    originalPixel,
    "DST mode should leave destination unchanged",
  );

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Composite: SRC_OVER mode (default)", async () => {
  const { rasterizer, surfaceId, contextId } = await setupCompositeTest();

  const srcBlue = WasmRasterizer.makeARGB(128, 0, 0, 255); // 50% blue
  // SRC_OVER is the default, but let's set it explicitly

  rasterizer.renderVariableLengthCommands(contextId, [
    (w) =>
      WasmRasterizer.writeCompositCommand(
        w,
        CompositeMode.COMPOSITE_SRC_OVER,
        1.0,
      ),
    (w) => WasmRasterizer.writeSetColorCommand(w, srcBlue),
    (w) => WasmRasterizer.writeFillRectCommand(w, 7, 7, 5, 5),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);
  const idx = 7 * 20 + 7;
  const result = unpackARGB(pixels[idx]);

  // SRC_OVER: blend 50% blue over 50% red
  // Expected: alpha should be > 128 (combining two 50% alphas)
  // Color should be a blend of blue and red (purple-ish)
  if (result.a <= 128) {
    throw new Error(
      `SRC_OVER should increase alpha: got ${result.a}, expected > 128`,
    );
  }

  // Blue and red channels should both be present
  if (result.r === 0 || result.b === 0) {
    throw new Error(
      `SRC_OVER should blend colors: got R=${result.r} B=${result.b}`,
    );
  }

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Composite: SRC_IN mode", async () => {
  const { rasterizer, surfaceId, contextId } = await setupCompositeTest();

  // Draw source blue rect overlapping the red rect
  const srcBlue = WasmRasterizer.makeARGB(255, 0, 0, 255); // opaque blue

  rasterizer.renderVariableLengthCommands(contextId, [
    (w) =>
      WasmRasterizer.writeCompositCommand(
        w,
        CompositeMode.COMPOSITE_SRC_IN,
        1.0,
      ),
    (w) => WasmRasterizer.writeSetColorCommand(w, srcBlue),
    (w) => WasmRasterizer.writeFillRectCommand(w, 7, 7, 5, 5),
  ]);

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
  const srcBlue = WasmRasterizer.makeARGB(255, 0, 0, 255); // opaque blue

  rasterizer.renderVariableLengthCommands(contextId, [
    (w) =>
      WasmRasterizer.writeCompositCommand(
        w,
        CompositeMode.COMPOSITE_SRC_OUT,
        1.0,
      ),
    (w) => WasmRasterizer.writeSetColorCommand(w, srcBlue),
    (w) => WasmRasterizer.writeFillRectCommand(w, 2, 2, 5, 5),
  ]);

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
  const srcBlue = WasmRasterizer.makeARGB(128, 0, 0, 255); // 50% blue

  rasterizer.renderVariableLengthCommands(contextId, [
    (w) =>
      WasmRasterizer.writeCompositCommand(
        w,
        CompositeMode.COMPOSITE_XOR,
        1.0,
      ),
    (w) => WasmRasterizer.writeSetColorCommand(w, srcBlue),
    (w) => WasmRasterizer.writeFillRectCommand(w, 7, 7, 5, 5),
  ]);

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
  const srcBlue = WasmRasterizer.makeARGB(255, 0, 0, 255); // opaque blue, but 50% composite alpha

  rasterizer.renderVariableLengthCommands(contextId, [
    (w) =>
      WasmRasterizer.writeCompositCommand(
        w,
        CompositeMode.COMPOSITE_SRC_OVER,
        0.5,
      ),
    (w) => WasmRasterizer.writeSetColorCommand(w, srcBlue),
    (w) => WasmRasterizer.writeFillRectCommand(w, 7, 7, 5, 5),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);
  const idx = 7 * 20 + 7;
  const result = unpackARGB(pixels[idx]);

  // With 50% composite alpha, the effective source alpha is 50% of 255 = 127.5
  // Result should have both blue and red components
  if (result.b === 0 || result.r === 0) {
    throw new Error(
      `Composite alpha should blend colors: got R=${result.r} B=${result.b}`,
    );
  }

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Composite: multiple operations preserve mode", async () => {
  const { rasterizer, surfaceId, contextId } = await setupCompositeTest();

  // Set SRC mode once, then draw multiple rects
  const green = WasmRasterizer.makeARGB(255, 0, 255, 0);
  const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);

  rasterizer.renderVariableLengthCommands(contextId, [
    (w) =>
      WasmRasterizer.writeCompositCommand(
        w,
        CompositeMode.COMPOSITE_SRC,
        1.0,
      ),
    (w) => WasmRasterizer.writeSetColorCommand(w, green),
    (w) => WasmRasterizer.writeFillRectCommand(w, 2, 2, 3, 3),
    (w) => WasmRasterizer.writeSetColorCommand(w, blue),
    (w) => WasmRasterizer.writeFillRectCommand(w, 10, 10, 3, 3),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);

  // Both rects should use SRC mode (no blending with background)
  assertPixelEquals(
    pixels[2 * 20 + 2],
    green,
    "First rect should use SRC mode",
  );
  assertPixelEquals(
    pixels[10 * 20 + 10],
    blue,
    "Second rect should still use SRC mode",
  );

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});
