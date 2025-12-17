/**
 * Basic tests for WASM rasterizer
 *
 * This file demonstrates core functionality of the rasterizer:
 * - Surface allocation and deallocation
 * - Pixel buffer access
 * - Basic drawing commands
 */

import { assertEquals, assertNotEquals } from "@std/assert";
import {
  PixelFormat,
  SurfaceOperation,
  WasmRasterizer,
} from "./wasm_rasterizer.ts";
import { assertPixelEquals } from "./test_helpers.ts";

const WASM_PATH = "../../../build/wasm/awt_raster.wasm";

Deno.test("WASM module loads successfully", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);
  // If we got here without throwing, the module loaded successfully
});

Deno.test("Surface allocation and deallocation", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  // Allocate a surface
  const surfaceId = rasterizer.allocateSurface(100, 100);
  assertNotEquals(surfaceId, -1, "Surface allocation should succeed");

  // Check dimensions
  const dims = rasterizer.getSurfaceDimensions(surfaceId);
  assertEquals(dims.width, 100);
  assertEquals(dims.height, 100);
  assertEquals(dims.stride, 400); // 100 * 4 bytes

  // Free the surface
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Pixel buffer access", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(10, 10);
  const pixels = rasterizer.getSurfacePixels(surfaceId);

  assertEquals(pixels.length, 100, "Pixel buffer should have 100 pixels");

  // All pixels should be initialized to 0
  for (let i = 0; i < pixels.length; i++) {
    assertEquals(pixels[i], 0, `Pixel ${i} should be initialized to 0`);
  }

  rasterizer.freeSurface(surfaceId);
});

Deno.test("Fill rect with red", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const width = 10;
  const height = 10;
  const surfaceId = rasterizer.allocateSurface(width, height);
  const contextId = rasterizer.createContext(surfaceId);

  // Set color to opaque red (0xFFFF0000)
  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);

  // Write commands using variable-length format
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, red),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, width, height),
  ]);

  // Check that all pixels are red
  const pixels = rasterizer.copySurfacePixels(surfaceId);
  for (let i = 0; i < pixels.length; i++) {
    assertPixelEquals(pixels[i], red, `Pixel ${i} should be red`);
  }

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Fill rect with partial area", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const width = 10;
  const height = 10;
  const surfaceId = rasterizer.allocateSurface(width, height);
  const contextId = rasterizer.createContext(surfaceId);

  // Set color to opaque blue (0xFF0000FF)
  const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);

  // Write commands using variable-length format
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, blue),
    (w) => WasmRasterizer.writeFillRectCommand(w, 2, 2, 5, 5),
  ]);

  // Check pixels
  const pixels = rasterizer.copySurfacePixels(surfaceId);

  // Pixels outside the rect should be 0
  assertEquals(pixels[0], 0, "Pixel (0,0) should be 0");
  assertEquals(pixels[1], 0, "Pixel (1,0) should be 0");

  // Pixels inside the rect should be blue
  const insideIdx = 2 * width + 2; // (2, 2)
  assertPixelEquals(pixels[insideIdx], blue, "Pixel (2,2) should be blue");

  const insideIdx2 = 6 * width + 6; // (6, 6)
  assertPixelEquals(pixels[insideIdx2], blue, "Pixel (6,6) should be blue");

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Clear rect", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const width = 10;
  const height = 10;
  const surfaceId = rasterizer.allocateSurface(width, height);
  const contextId = rasterizer.createContext(surfaceId);

  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, red),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, width, height),
    (w) => WasmRasterizer.writeClearRectCommand(w, 3, 3, 3, 3),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);
  assertPixelEquals(pixels[0], red, "Pixel (0,0) should be red");
  const clearedIdx = 3 * width + 3;
  const defaultBgColor = WasmRasterizer.makeARGB(255, 255, 255, 255);
  assertPixelEquals(pixels[clearedIdx], defaultBgColor, "Pixel (3,3) should be cleared");

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Draw line", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const width = 20;
  const height = 20;
  const surfaceId = rasterizer.allocateSurface(width, height);
  const contextId = rasterizer.createContext(surfaceId);

  const white = WasmRasterizer.makeARGB(255, 255, 255, 255);
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, white),
    (w) => WasmRasterizer.writeDrawLineCommand(w, 5, 10, 15, 10),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);
  const beforeIdx = 10 * width + 4;
  assertEquals(pixels[beforeIdx], 0, "Pixel (4,10) should be 0");
  for (let x = 5; x <= 15; x++) {
    const idx = 10 * width + x;
    assertPixelEquals(pixels[idx], white, `Pixel (,10) should be white`);
  }
  const afterIdx = 10 * width + 16;
  assertEquals(pixels[afterIdx], 0, "Pixel (16,10) should be 0");

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Draw rect outline", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const width = 20;
  const height = 20;
  const surfaceId = rasterizer.allocateSurface(width, height);
  const contextId = rasterizer.createContext(surfaceId);

  const green = WasmRasterizer.makeARGB(255, 0, 255, 0);
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, green),
    (w) => WasmRasterizer.writeDrawRectCommand(w, 5, 5, 10, 10),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);
  const topLeftIdx = 5 * width + 5;
  assertPixelEquals(pixels[topLeftIdx], green, "Pixel (5,5) should be green");
  const insideIdx = 10 * width + 10;
  assertEquals(pixels[insideIdx], 0, "Pixel (10,10) should be 0 (not filled)");

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Clipping rect", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const width = 20;
  const height = 20;
  const surfaceId = rasterizer.allocateSurface(width, height);
  const context = rasterizer.createContext(surfaceId);

  const yellow = WasmRasterizer.makeARGB(255, 255, 255, 0);
  rasterizer.renderVariableLengthCommands(context, [
    (w) => WasmRasterizer.writeSetClipRectCommand(w, 5, 5, 10, 10),
    (w) => WasmRasterizer.writeSetColorCommand(w, yellow),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, width, height),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);
  assertEquals(pixels[0], 0, "Pixel (0,0) should be 0 (outside clip)");
  const insideIdx = 10 * width + 10;
  assertPixelEquals(pixels[insideIdx], yellow, "Pixel (10,10) should be yellow");

  rasterizer.destroyContext(context);
  rasterizer.freeSurface(surfaceId);
});

Deno.test("Multiple surfaces", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  // Allocate two surfaces
  const surface1 = rasterizer.allocateSurface(10, 10);
  const surface2 = rasterizer.allocateSurface(20, 20);

  const contextId1 = rasterizer.createContext(surface1);
  const contextId2 = rasterizer.createContext(surface2);

  // Check dimensions
  const dims1 = rasterizer.getSurfaceDimensions(surface1);
  assertEquals(dims1.width, 10);
  assertEquals(dims1.height, 10);

  const dims2 = rasterizer.getSurfaceDimensions(surface2);
  assertEquals(dims2.width, 20);
  assertEquals(dims2.height, 20);

  // Fill surface1 with red
  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
  rasterizer.renderVariableLengthCommands(contextId1, [
    (w) => WasmRasterizer.writeSetColorCommand(w, red),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 10, 10),
  ]);

  // Fill surface2 with blue
  const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);
  rasterizer.renderVariableLengthCommands(contextId2, [
    (w) => WasmRasterizer.writeSetColorCommand(w, blue),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 20, 20),
  ]);

  // Verify each surface has its own color
  const pixels1 = rasterizer.copySurfacePixels(surface1);
  assertPixelEquals(pixels1[0], red, "Surface 1 should be red");

  const pixels2 = rasterizer.copySurfacePixels(surface2);
  assertPixelEquals(pixels2[0], blue, "Surface 2 should be blue");

  rasterizer.destroyContext(contextId1);
  rasterizer.destroyContext(contextId2);
  rasterizer.freeSurface(surface1);
  rasterizer.freeSurface(surface2);
});

Deno.test("Color extraction and creation", () => {
  const r = 128, g = 64, b = 32, a = 255;
  const color = WasmRasterizer.makeARGB(a, r, g, b);

  const extracted = WasmRasterizer.extractARGB(color);
  assertEquals(extracted.a, a);
  assertEquals(extracted.r, r);
  assertEquals(extracted.g, g);
  assertEquals(extracted.b, b);
});
