/**
 * Visual demo for new draw operations
 * 
 * This demo showcases the newly implemented draw primitives:
 * - drawOval
 * - drawArc
 * - drawRoundRect
 * - drawPolyline
 * - copyArea
 * 
 * Run with: deno run --allow-read src/test/deno/draw_operations_demo.ts
 */

import { WasmRasterizer } from "./wasm_rasterizer.ts";
import { printSurface } from "./test_helpers.ts";

const WASM_PATH = "../../../build/wasm/awt_raster.wasm";
async function main() {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  console.log("\n=== Draw Operations Demo ===\n");

  // Demo 1: Draw Ovals (circles and ellipses)
  {
    const surfaceId = rasterizer.allocateSurface(30, 20);
    const contextId = rasterizer.createContext(surfaceId);

    const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
    const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);

    rasterizer.renderVariableLengthCommands(contextId, [
      // Draw circle
      (w) => WasmRasterizer.writeSetColorCommand(w, red),
      (w) => WasmRasterizer.writeDrawOvalCommand(w, 2, 2, 12, 12),
      // Draw ellipse
      (w) => WasmRasterizer.writeSetColorCommand(w, blue),
      (w) => WasmRasterizer.writeDrawOvalCommand(w, 16, 5, 12, 8),
    ]);

    printSurface(rasterizer, surfaceId, "Demo 1: Draw Ovals");

    rasterizer.destroyContext(contextId);
    rasterizer.freeSurface(surfaceId);
  }

  // Demo 2: Draw Arcs
  {
    const surfaceId = rasterizer.allocateSurface(40, 15);
    const contextId = rasterizer.createContext(surfaceId);

    const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
    const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);
    const green = WasmRasterizer.makeARGB(255, 0, 255, 0);

    rasterizer.renderVariableLengthCommands(contextId, [
      // Quarter circle (0-90 degrees)
      (w) => WasmRasterizer.writeSetColorCommand(w, red),
      (w) => WasmRasterizer.writeDrawArcCommand(w, 2, 2, 10, 10, 0, 90),
      // Half circle (0-180 degrees)
      (w) => WasmRasterizer.writeSetColorCommand(w, blue),
      (w) => WasmRasterizer.writeDrawArcCommand(w, 14, 2, 12, 10, 0, 180),
      // Three-quarter circle
      (w) => WasmRasterizer.writeSetColorCommand(w, green),
      (w) => WasmRasterizer.writeDrawArcCommand(w, 28, 2, 10, 10, 0, 270),
    ]);

    printSurface(rasterizer, surfaceId, "Demo 2: Draw Arcs");

    rasterizer.destroyContext(contextId);
    rasterizer.freeSurface(surfaceId);
  }

  // Demo 3: Draw Rounded Rectangles
  {
    const surfaceId = rasterizer.allocateSurface(35, 20);
    const contextId = rasterizer.createContext(surfaceId);

    const cyan = WasmRasterizer.makeARGB(255, 0, 255, 255);
    const magenta = WasmRasterizer.makeARGB(255, 255, 0, 255);

    rasterizer.renderVariableLengthCommands(contextId, [
      // Small corner radius
      (w) => WasmRasterizer.writeSetColorCommand(w, cyan),
      (w) => WasmRasterizer.writeDrawRoundRectCommand(w, 2, 2, 14, 10, 4, 4),
      // Large corner radius
      (w) => WasmRasterizer.writeSetColorCommand(w, magenta),
      (w) => WasmRasterizer.writeDrawRoundRectCommand(w, 18, 5, 14, 12, 6, 6),
    ]);

    printSurface(rasterizer, surfaceId, "Demo 3: Draw Rounded Rectangles");

    rasterizer.destroyContext(contextId);
    rasterizer.freeSurface(surfaceId);
  }

  // Demo 4: Draw Polylines
  {
    const surfaceId = rasterizer.allocateSurface(30, 15);
    const contextId = rasterizer.createContext(surfaceId);

    const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
    const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);

    rasterizer.renderVariableLengthCommands(contextId, [
      // Zigzag pattern
      (w) => WasmRasterizer.writeSetColorCommand(w, red),
      (w) =>
        WasmRasterizer.writeDrawPolylineCommand(
          w,
          [2, 8, 2, 8, 2],
          [2, 4, 6, 8, 10],
        ),
      // Star pattern (not closed)
      (w) => WasmRasterizer.writeSetColorCommand(w, blue),
      (w) =>
        WasmRasterizer.writeDrawPolylineCommand(
          w,
          [20, 15, 25, 13, 22],
          [3, 8, 8, 12, 6],
        ),
    ]);

    printSurface(rasterizer, surfaceId, "Demo 4: Draw Polylines");

    rasterizer.destroyContext(contextId);
    rasterizer.freeSurface(surfaceId);
  }

  // Demo 5: Copy Area
  {
    const surfaceId = rasterizer.allocateSurface(30, 20);
    const contextId = rasterizer.createContext(surfaceId);

    const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
    const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);
    const green = WasmRasterizer.makeARGB(255, 0, 255, 0);

    rasterizer.renderVariableLengthCommands(contextId, [
      // Draw original pattern
      (w) => WasmRasterizer.writeSetColorCommand(w, red),
      (w) => WasmRasterizer.writeFillRectCommand(w, 2, 2, 6, 6),
      (w) => WasmRasterizer.writeSetColorCommand(w, blue),
      (w) => WasmRasterizer.writeFillRectCommand(w, 4, 4, 3, 3),
      // Copy to new location
      (w) => WasmRasterizer.writeCopyAreaCommand(w, 2, 2, 8, 8, 12, 0),
      // Draw border around copy
      (w) => WasmRasterizer.writeSetColorCommand(w, green),
      (w) => WasmRasterizer.writeDrawRectCommand(w, 14, 2, 8, 8),
    ]);

    printSurface(rasterizer, surfaceId, "Demo 5: Copy Area");

    rasterizer.destroyContext(contextId);
    rasterizer.freeSurface(surfaceId);
  }

  // Demo 6: Combined showcase
  {
    const surfaceId = rasterizer.allocateSurface(40, 25);
    const contextId = rasterizer.createContext(surfaceId);

    const colors = [
      WasmRasterizer.makeARGB(255, 255, 0, 0), // red
      WasmRasterizer.makeARGB(255, 0, 255, 0), // green
      WasmRasterizer.makeARGB(255, 0, 0, 255), // blue
      WasmRasterizer.makeARGB(255, 255, 255, 0), // yellow
      WasmRasterizer.makeARGB(255, 255, 0, 255), // magenta
      WasmRasterizer.makeARGB(255, 0, 255, 255), // cyan
    ];

    rasterizer.renderVariableLengthCommands(contextId, [
      // Row 1: Draw operations
      (w) => WasmRasterizer.writeSetColorCommand(w, colors[0]),
      (w) => WasmRasterizer.writeDrawOvalCommand(w, 2, 2, 10, 8),
      (w) => WasmRasterizer.writeSetColorCommand(w, colors[1]),
      (w) => WasmRasterizer.writeDrawArcCommand(w, 14, 2, 10, 8, 0, 180),
      (w) => WasmRasterizer.writeSetColorCommand(w, colors[2]),
      (w) => WasmRasterizer.writeDrawRoundRectCommand(w, 26, 2, 12, 8, 3, 3),
      // Row 2: Fill operations
      (w) => WasmRasterizer.writeSetColorCommand(w, colors[3]),
      (w) => WasmRasterizer.writeFillOvalCommand(w, 2, 14, 10, 8),
      (w) => WasmRasterizer.writeSetColorCommand(w, colors[4]),
      (w) => WasmRasterizer.writeFillArcCommand(w, 14, 14, 10, 8, 45, 90),
      (w) => WasmRasterizer.writeSetColorCommand(w, colors[5]),
      (w) => WasmRasterizer.writeFillRoundRectCommand(w, 26, 14, 12, 8, 4, 4),
    ]);

    printSurface(rasterizer, surfaceId, "Demo 6: Combined Showcase");

    rasterizer.destroyContext(contextId);
    rasterizer.freeSurface(surfaceId);
  }

  console.log("\n=== All demos complete! ===\n");
}

if (import.meta.main) {
  main();
}
