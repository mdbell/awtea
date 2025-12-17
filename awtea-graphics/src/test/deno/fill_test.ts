/**
 * Tests for fill operations using edge table infrastructure
 *
 * This file tests the new fill operations:
 * - fillPolygon
 * - fillOval
 * - fillArc
 * - fillRoundRect
 * - fillRect (with transforms)
 */

import { assertEquals, assertNotEquals } from "@std/assert";
import {
  PixelFormat,
  SurfaceOperation,
  WasmRasterizer,
} from "./wasm_rasterizer.ts";

const WASM_PATH = "../../../build/wasm/awt_raster.wasm";

// Helper to compare Uint32 values (JavaScript bitwise operations are signed)
function assertPixelEquals(actual: number, expected: number, message: string) {
  const actualU32 = actual >>> 0;
  const expectedU32 = expected >>> 0;
  assertEquals(actualU32, expectedU32, message);
}

// Helper to count non-black pixels
function countNonBlackPixels(pixels: Uint32Array): number {
  let count = 0;
  for (let i = 0; i < pixels.length; i++) {
    const alpha = (pixels[i] >>> 24) & 0xFF;
    if (alpha > 0) {
      count++;
    }
  }
  return count;
}

Deno.test("fillPolygon - simple triangle", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(100, 100);
  const contextId = rasterizer.createContext(surfaceId);

  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);

  // Triangle vertices
  const xPoints = [25, 75, 50];
  const yPoints = [25, 25, 75];

  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, red),
    (w) => WasmRasterizer.writeFillPolygonCommand(w, xPoints, yPoints),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);

  // Check that center of triangle is filled (centroid is around y=42)
  const centerIdx = 42 * 100 + 50;
  assertPixelEquals(pixels[centerIdx], red, "Center of triangle should be red");

  // Check that outside is not filled
  const outsideIdx = 10 * 100 + 10;
  assertPixelEquals(
    pixels[outsideIdx],
    0,
    "Pixel outside triangle should be transparent",
  );
  
  // Verify some pixels were filled
  const filledCount = countNonBlackPixels(pixels);
  assertNotEquals(filledCount, 0, "Triangle should have filled pixels");

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("fillPolygon - rectangle (4 points)", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(100, 100);
  const contextId = rasterizer.createContext(surfaceId);

  const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);

  // Rectangle vertices
  const xPoints = [20, 80, 80, 20];
  const yPoints = [20, 20, 80, 80];

  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, blue),
    (w) => WasmRasterizer.writeFillPolygonCommand(w, xPoints, yPoints),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);

  // Check multiple points inside
  const idx1 = 30 * 100 + 30;
  const idx2 = 50 * 100 + 50;
  const idx3 = 70 * 100 + 70;

  assertPixelEquals(pixels[idx1], blue, "Point (30,30) should be blue");
  assertPixelEquals(pixels[idx2], blue, "Point (50,50) should be blue");
  assertPixelEquals(pixels[idx3], blue, "Point (70,70) should be blue");

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("fillOval - circle", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(100, 100);
  const contextId = rasterizer.createContext(surfaceId);

  const green = WasmRasterizer.makeARGB(255, 0, 255, 0);

  // Circle at center with radius 30
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, green),
    (w) => WasmRasterizer.writeFillOvalCommand(w, 20, 20, 60, 60),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);

  // Check center is filled
  const centerIdx = 50 * 100 + 50;
  assertPixelEquals(pixels[centerIdx], green, "Center of circle should be green");

  // Check that some pixels are filled
  const filledCount = countNonBlackPixels(pixels);
  assertNotEquals(filledCount, 0, "Circle should have filled pixels");

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("fillArc - pie slice", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(100, 100);
  const contextId = rasterizer.createContext(surfaceId);

  const yellow = WasmRasterizer.makeARGB(255, 255, 255, 0);

  // Arc from 0 to 90 degrees (upper right quadrant)
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, yellow),
    (w) => WasmRasterizer.writeFillArcCommand(w, 20, 20, 60, 60, 0, 90),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);

  // Check that some pixels are filled
  const filledCount = countNonBlackPixels(pixels);
  assertNotEquals(filledCount, 0, "Arc should have filled pixels");

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("fillRoundRect - rounded corners", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(100, 100);
  const contextId = rasterizer.createContext(surfaceId);

  const cyan = WasmRasterizer.makeARGB(255, 0, 255, 255);

  // Rounded rectangle with corner radius 10
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, cyan),
    (w) => WasmRasterizer.writeFillRoundRectCommand(w, 20, 20, 60, 60, 20, 20),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);

  // Check center is filled
  const centerIdx = 50 * 100 + 50;
  assertPixelEquals(
    pixels[centerIdx],
    cyan,
    "Center of rounded rect should be cyan",
  );

  // Check that some pixels are filled
  const filledCount = countNonBlackPixels(pixels);
  assertNotEquals(filledCount, 0, "Rounded rect should have filled pixels");

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("fillRect with transform - rotation", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(100, 100);
  const contextId = rasterizer.createContext(surfaceId);

  const magenta = WasmRasterizer.makeARGB(255, 255, 0, 255);

  // 45-degree rotation around center
  const angle = Math.PI / 4; // 45 degrees
  const cos = Math.cos(angle);
  const sin = Math.sin(angle);

  // Translate to center, rotate, translate back
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetTransformCommand(w, cos, sin, 0, -sin, cos, 0),
    (w) => WasmRasterizer.writeSetColorCommand(w, magenta),
    (w) => WasmRasterizer.writeFillRectCommand(w, 35, 35, 30, 30),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);

  // Check that some pixels are filled (rotated rect)
  const filledCount = countNonBlackPixels(pixels);
  assertNotEquals(
    filledCount,
    0,
    "Rotated rectangle should have filled pixels",
  );

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("fillPolygon with transform - translation", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(100, 100);
  const contextId = rasterizer.createContext(surfaceId);

  const orange = WasmRasterizer.makeARGB(255, 255, 165, 0);

  // Triangle at origin, translate by (30, 30)
  const xPoints = [0, 20, 10];
  const yPoints = [0, 0, 20];

  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetTransformCommand(w, 1, 0, 30, 0, 1, 30),
    (w) => WasmRasterizer.writeSetColorCommand(w, orange),
    (w) => WasmRasterizer.writeFillPolygonCommand(w, xPoints, yPoints),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);

  // Check that some pixel in the translated triangle is filled
  // Triangle centroid would be around (36, 36) after translation
  const centerIdx = 36 * 100 + 36;
  assertPixelEquals(
    pixels[centerIdx],
    orange,
    "Center of translated triangle should be orange",
  );
  
  // Verify some pixels were filled
  const filledCount = countNonBlackPixels(pixels);
  assertNotEquals(filledCount, 0, "Translated triangle should have filled pixels");

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("fillPolygon - complex concave shape", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(100, 100);
  const contextId = rasterizer.createContext(surfaceId);

  const purple = WasmRasterizer.makeARGB(255, 128, 0, 128);

  // Star shape (concave polygon)
  const xPoints = [50, 60, 80, 65, 70, 50, 30, 35, 20, 40];
  const yPoints = [10, 35, 35, 50, 75, 60, 75, 50, 35, 35];

  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, purple),
    (w) => WasmRasterizer.writeFillPolygonCommand(w, xPoints, yPoints),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);

  // Check that some pixels are filled
  const filledCount = countNonBlackPixels(pixels);
  assertNotEquals(filledCount, 0, "Star shape should have filled pixels");

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});
