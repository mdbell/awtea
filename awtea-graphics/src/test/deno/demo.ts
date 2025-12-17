/**
 * Demo/Example: Using the WASM Rasterizer
 * 
 * This file demonstrates practical usage of the WASM rasterizer
 * for various drawing scenarios. Run with: deno run --allow-read demo.ts
 */

import { WasmRasterizer, PixelFormat } from "./wasm_rasterizer.ts";
import { printSurface, saveSurfaceAsPPM } from "./test_helpers.ts";

const WASM_PATH = "../../../build/wasm/awt_raster.wasm";

/**
 * Demo 1: Simple filled rectangles
 */
async function demo1_FilledRectangles() {
  console.log("\n=== Demo 1: Filled Rectangles ===");
  
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(20, 20);
  const contextId = rasterizer.createContext(surfaceId);

  // Draw three overlapping colored rectangles
  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
  const green = WasmRasterizer.makeARGB(255, 0, 255, 0);
  const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);

  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, red),
    (w) => WasmRasterizer.writeFillRectCommand(w, 2, 2, 8, 8),
    (w) => WasmRasterizer.writeSetColorCommand(w, green),
    (w) => WasmRasterizer.writeFillRectCommand(w, 6, 6, 8, 8),
    (w) => WasmRasterizer.writeSetColorCommand(w, blue),
    (w) => WasmRasterizer.writeFillRectCommand(w, 10, 10, 8, 8),
  ]);

  printSurface(rasterizer, surfaceId, "Surface");

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
}

/**
 * Demo 2: Drawing with clipping
 */
async function demo2_Clipping() {
  console.log("\n=== Demo 2: Clipping ===");
  
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(20, 20);
  const contextId = rasterizer.createContext(surfaceId);

  // Set clip rect to center 10x10 area
  const yellow = WasmRasterizer.makeARGB(255, 255, 255, 0);

  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetClipRectCommand(w, 5, 5, 10, 10),
    (w) => WasmRasterizer.writeSetColorCommand(w, yellow),
    // Try to fill entire surface (should be clipped to center)
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 20, 20),
    // Draw a line that extends beyond clip (should be clipped)
    (w) => WasmRasterizer.writeDrawLineCommand(w, 0, 10, 19, 10),
  ]);

  printSurface(rasterizer, surfaceId, "Surface");

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
}

/**
 * Demo 3: Checkerboard pattern
 */
async function demo3_Checkerboard() {
  console.log("\n=== Demo 3: Checkerboard Pattern ===");
  
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const width = 16;
  const height = 16;
  const cellSize = 2;
  const surfaceId = rasterizer.allocateSurface(width, height);
  const contextId = rasterizer.createContext(surfaceId);

  const black = WasmRasterizer.makeARGB(255, 0, 0, 0);
  const white = WasmRasterizer.makeARGB(255, 255, 255, 255);

  const numCellsX = width / cellSize;
  const numCellsY = height / cellSize;
  const commands = [];

  for (let row = 0; row < numCellsY; row++) {
    for (let col = 0; col < numCellsX; col++) {
      const color = ((row + col) % 2 === 0) ? black : white;
      commands.push((w: any) => WasmRasterizer.writeSetColorCommand(w, color));
      commands.push((w: any) => WasmRasterizer.writeFillRectCommand(w, col * cellSize, row * cellSize, cellSize, cellSize));
    }
  }

  rasterizer.renderVariableLengthCommands(contextId, commands);
  printSurface(rasterizer, surfaceId, 0x80);

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
}

/**
 * Demo 4: Surface-to-surface blitting
 */
async function demo4_SurfaceBlitting() {
  console.log("\n=== Demo 4: Surface-to-Surface Blitting ===");
  
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  // Create a small stamp surface (4x4 cross pattern)
  const stampSurfaceId = rasterizer.allocateSurface(4, 4);
  const stampContextId = rasterizer.createContext(stampSurfaceId);
  
  const magenta = WasmRasterizer.makeARGB(255, 255, 0, 255);
  
  // Draw a cross pattern on the stamp surface
  rasterizer.renderVariableLengthCommands(stampContextId, [
    // Draw magenta cross
    (w) => WasmRasterizer.writeSetColorCommand(w, magenta),
    (w) => WasmRasterizer.writeFillRectCommand(w, 1, 0, 2, 1), // top
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 1, 4, 2), // middle
    (w) => WasmRasterizer.writeFillRectCommand(w, 1, 3, 2, 1), // bottom
  ]);

  // Create destination surface
  const destSurfaceId = rasterizer.allocateSurface(16, 16);
  const destContextId = rasterizer.createContext(destSurfaceId);

  // Use CMD_BLIT_IMAGE to blit the stamp surface to multiple positions
  // This is the proper way to copy one surface onto another
  rasterizer.renderVariableLengthCommands(destContextId, [
    (w) => WasmRasterizer.writeBlitImageCommand(w, stampSurfaceId, 0, 0),
    (w) => WasmRasterizer.writeBlitImageCommand(w, stampSurfaceId, 6, 0),
    (w) => WasmRasterizer.writeBlitImageCommand(w, stampSurfaceId, 0, 6),
    (w) => WasmRasterizer.writeBlitImageCommand(w, stampSurfaceId, 6, 6),
  ]);

  printSurface(rasterizer, destSurfaceId, "Surface");

  rasterizer.destroyContext(stampContextId);
  rasterizer.destroyContext(destContextId);
  rasterizer.freeSurface(stampSurfaceId);
  rasterizer.freeSurface(destSurfaceId);
}

/**
 * Demo 5: Drawing with transforms (translation)
 */
async function demo5_Transforms() {
  console.log("\n=== Demo 5: Transforms (Translation) ===");
  
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(20, 20);
  const contextId = rasterizer.createContext(surfaceId);

  const cyan = WasmRasterizer.makeARGB(255, 0, 255, 255);

  rasterizer.renderVariableLengthCommands(contextId, [
    // Set a translation transform (shift by 5, 5)
    (w) => WasmRasterizer.writeSetTransformCommand(w, 1, 0, 5, 0, 1, 5),
    (w) => WasmRasterizer.writeSetColorCommand(w, cyan),
    // Draw at (0, 0) but should appear at (5, 5) due to transform
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 6, 6),
    // Reset transform to identity
    (w) => WasmRasterizer.writeSetTransformCommand(w, 1, 0, 0, 0, 1, 0),
  ]);

  printSurface(rasterizer, surfaceId, "Surface");

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
}

/**
 * Demo 6: Complex scene with multiple operations
 */
async function demo6_ComplexScene() {
  console.log("\n=== Demo 6: Complex Scene ===");
  
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const width = 30;
  const height = 30;
  const surfaceId = rasterizer.allocateSurface(width, height);
  const contextId = rasterizer.createContext(surfaceId);

  const lightGray = WasmRasterizer.makeARGB(255, 200, 200, 200);
  const darkGray = WasmRasterizer.makeARGB(255, 50, 50, 50);
  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
  const green = WasmRasterizer.makeARGB(255, 0, 255, 0);
  const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);
  const yellow = WasmRasterizer.makeARGB(255, 255, 255, 0);

  rasterizer.renderVariableLengthCommands(contextId, [
    // Background fill (light gray)
    (w) => WasmRasterizer.writeSetColorCommand(w, lightGray),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, width, height),
    // Draw a border (dark gray)
    (w) => WasmRasterizer.writeSetColorCommand(w, darkGray),
    (w) => WasmRasterizer.writeDrawRectCommand(w, 0, 0, width, height),
    // Draw colored squares
    (w) => WasmRasterizer.writeSetColorCommand(w, red),
    (w) => WasmRasterizer.writeFillRectCommand(w, 5, 5, 6, 6),
    (w) => WasmRasterizer.writeSetColorCommand(w, green),
    (w) => WasmRasterizer.writeFillRectCommand(w, 13, 5, 6, 6),
    (w) => WasmRasterizer.writeSetColorCommand(w, blue),
    (w) => WasmRasterizer.writeFillRectCommand(w, 21, 5, 6, 6),
    // Draw diagonal lines
    (w) => WasmRasterizer.writeSetColorCommand(w, yellow),
    (w) => WasmRasterizer.writeDrawLineCommand(w, 0, 0, width - 1, height - 1),
    (w) => WasmRasterizer.writeDrawLineCommand(w, width - 1, 0, 0, height - 1),
    // Clear a small area in the center
    (w) => WasmRasterizer.writeClearRectCommand(w, 12, 12, 6, 6),
  ]);

  printSurface(rasterizer, surfaceId, "Surface", 0x20);

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
}

/**
 * Demo 7: Fill operations with edge table infrastructure
 */
async function demo7_FillOperations() {
  console.log("\n=== Demo 7: Fill Operations (Edge Table) ===");
  
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const width = 40;
  const height = 30;
  const surfaceId = rasterizer.allocateSurface(width, height);
  const contextId = rasterizer.createContext(surfaceId);

  // Clear background to white
  const white = WasmRasterizer.makeARGB(255, 255, 255, 255);
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, white),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, width, height),
  ]);

  // Draw a filled triangle (polygon)
  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, red),
    (w) => WasmRasterizer.writeFillPolygonCommand(w, [5, 15, 10], [5, 5, 15]),
  ]);

  // Draw a filled oval
  const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, blue),
    (w) => WasmRasterizer.writeFillOvalCommand(w, 18, 5, 12, 12),
  ]);

  // Draw a filled rounded rectangle
  const green = WasmRasterizer.makeARGB(255, 0, 200, 0);
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, green),
    (w) => WasmRasterizer.writeFillRoundRectCommand(w, 5, 19, 15, 8, 4, 4),
  ]);

  // Draw a filled arc (pie slice)
  const yellow = WasmRasterizer.makeARGB(255, 255, 255, 0);
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, yellow),
    (w) => WasmRasterizer.writeFillArcCommand(w, 24, 18, 12, 10, 0, 180),
  ]);

  printSurface(rasterizer, surfaceId, "Surface");

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
}

/**
 * Demo 8: Filled polygons with transforms
 */
async function demo8_TransformedFillOperations() {
  console.log("\n=== Demo 8: Transformed Fill Operations ===");
  
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const width = 30;
  const height = 30;
  const surfaceId = rasterizer.allocateSurface(width, height);
  const contextId = rasterizer.createContext(surfaceId);

  // Clear background to light gray
  const lightGray = WasmRasterizer.makeARGB(255, 220, 220, 220);
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, lightGray),
    (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, width, height),
  ]);

  // Draw same triangle with different translations
  const red = WasmRasterizer.makeARGB(200, 255, 0, 0);
  const green = WasmRasterizer.makeARGB(200, 0, 255, 0);
  const blue = WasmRasterizer.makeARGB(200, 0, 0, 255);

  const triX = [0, 8, 4];
  const triY = [0, 0, 8];

  // Red triangle - translated to (5, 5)
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetTransformCommand(w, 1, 0, 5, 0, 1, 5),
    (w) => WasmRasterizer.writeSetColorCommand(w, red),
    (w) => WasmRasterizer.writeFillPolygonCommand(w, triX, triY),
  ]);

  // Green triangle - translated to (10, 10)
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetTransformCommand(w, 1, 0, 10, 0, 1, 10),
    (w) => WasmRasterizer.writeSetColorCommand(w, green),
    (w) => WasmRasterizer.writeFillPolygonCommand(w, triX, triY),
  ]);

  // Blue triangle - translated to (15, 15)
  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetTransformCommand(w, 1, 0, 15, 0, 1, 15),
    (w) => WasmRasterizer.writeSetColorCommand(w, blue),
    (w) => WasmRasterizer.writeFillPolygonCommand(w, triX, triY),
  ]);

  printSurface(rasterizer, surfaceId, "Surface");

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
}

/**
 * Main demo runner
 */
async function main() {
  console.log("╔════════════════════════════════════════════╗");
  console.log("║  WASM Rasterizer Demo & Example Usage     ║");
  console.log("╚════════════════════════════════════════════╝");

  try {
    await demo1_FilledRectangles();
    await demo2_Clipping();
    await demo3_Checkerboard();
    await demo4_SurfaceBlitting();
    await demo5_Transforms();
    await demo6_ComplexScene();
    await demo7_FillOperations();
    await demo8_TransformedFillOperations();

    console.log("\n✅ All demos completed successfully!");
    console.log("\nNote: This demo shows ASCII visualization of rendered surfaces.");
    console.log("For actual image output, implement PNG encoding or use the PPM helper.");
    console.log("\nTo run automated tests, use: ./gradlew :awtea-graphics:denoTest");
  } catch (error) {
    console.error("\n❌ Demo failed:", error);
    Deno.exit(1);
  }
}

// Run if this is the main module
if (import.meta.main) {
  await main();
}
