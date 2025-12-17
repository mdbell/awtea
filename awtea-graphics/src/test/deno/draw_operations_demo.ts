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
 * Run with: deno run --allow-read --allow-write src/test/deno/draw_operations_demo.ts
 */

import { WasmRasterizer } from "./wasm_rasterizer.ts";

const WASM_PATH = "../../../build/wasm/awt_raster.wasm";
const OUTPUT_DIR = "/tmp/awtea-draw-demos/";

/**
 * Save surface pixels as PPM image
 */
function saveToPPM(
  pixels: Uint32Array,
  width: number,
  height: number,
  filename: string,
): void {
  const lines: string[] = [];
  lines.push("P3");
  lines.push(`${width} ${height}`);
  lines.push("255");

  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      const pixel = pixels[y * width + x];
      const r = (pixel >> 16) & 0xFF;
      const g = (pixel >> 8) & 0xFF;
      const b = pixel & 0xFF;
      lines.push(`${r} ${g} ${b}`);
    }
  }

  try {
    Deno.mkdirSync(OUTPUT_DIR, { recursive: true });
  } catch {
    // Directory might already exist
  }

  Deno.writeTextFileSync(OUTPUT_DIR + filename, lines.join("\n"));
  console.log(`Saved: ${OUTPUT_DIR}${filename}`);
}

async function main() {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  console.log("\n=== Draw Operations Demo ===\n");

  // Demo 1: Draw Ovals (circles and ellipses)
  {
    const surfaceId = rasterizer.allocateSurface(200, 200);
    const contextId = rasterizer.createContext(surfaceId);

    const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
    const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);
    const green = WasmRasterizer.makeARGB(255, 0, 255, 0);

    rasterizer.renderVariableLengthCommands(contextId, [
      // Draw circle
      (w) => WasmRasterizer.writeSetColorCommand(w, red),
      (w) => WasmRasterizer.writeDrawOvalCommand(w, 20, 20, 60, 60),
      // Draw wide ellipse
      (w) => WasmRasterizer.writeSetColorCommand(w, blue),
      (w) => WasmRasterizer.writeDrawOvalCommand(w, 100, 50, 80, 40),
      // Draw tall ellipse
      (w) => WasmRasterizer.writeSetColorCommand(w, green),
      (w) => WasmRasterizer.writeDrawOvalCommand(w, 60, 110, 40, 70),
    ]);

    const pixels = rasterizer.copySurfacePixels(surfaceId);
    saveToPPM(pixels, 200, 200, "draw_ovals.ppm");

    rasterizer.destroyContext(contextId);
    rasterizer.freeSurface(surfaceId);
  }

  // Demo 2: Draw Arcs
  {
    const surfaceId = rasterizer.allocateSurface(200, 200);
    const contextId = rasterizer.createContext(surfaceId);

    const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
    const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);
    const green = WasmRasterizer.makeARGB(255, 0, 255, 0);
    const yellow = WasmRasterizer.makeARGB(255, 255, 255, 0);

    rasterizer.renderVariableLengthCommands(contextId, [
      // Quarter circle (0-90 degrees)
      (w) => WasmRasterizer.writeSetColorCommand(w, red),
      (w) => WasmRasterizer.writeDrawArcCommand(w, 20, 20, 60, 60, 0, 90),
      // Half circle (0-180 degrees)
      (w) => WasmRasterizer.writeSetColorCommand(w, blue),
      (w) => WasmRasterizer.writeDrawArcCommand(w, 100, 20, 80, 60, 0, 180),
      // Three-quarter circle (0-270 degrees)
      (w) => WasmRasterizer.writeSetColorCommand(w, green),
      (w) => WasmRasterizer.writeDrawArcCommand(w, 20, 110, 80, 70, 0, 270),
      // Full circle via arc (0-360 degrees)
      (w) => WasmRasterizer.writeSetColorCommand(w, yellow),
      (w) => WasmRasterizer.writeDrawArcCommand(w, 120, 120, 60, 60, 0, 360),
    ]);

    const pixels = rasterizer.copySurfacePixels(surfaceId);
    saveToPPM(pixels, 200, 200, "draw_arcs.ppm");

    rasterizer.destroyContext(contextId);
    rasterizer.freeSurface(surfaceId);
  }

  // Demo 3: Draw Rounded Rectangles
  {
    const surfaceId = rasterizer.allocateSurface(200, 200);
    const contextId = rasterizer.createContext(surfaceId);

    const cyan = WasmRasterizer.makeARGB(255, 0, 255, 255);
    const magenta = WasmRasterizer.makeARGB(255, 255, 0, 255);
    const orange = WasmRasterizer.makeARGB(255, 255, 165, 0);

    rasterizer.renderVariableLengthCommands(contextId, [
      // Small corner radius
      (w) => WasmRasterizer.writeSetColorCommand(w, cyan),
      (w) => WasmRasterizer.writeDrawRoundRectCommand(w, 20, 20, 60, 60, 10, 10),
      // Medium corner radius
      (w) => WasmRasterizer.writeSetColorCommand(w, magenta),
      (w) => WasmRasterizer.writeDrawRoundRectCommand(w, 100, 20, 80, 60, 20, 20),
      // Large corner radius (nearly circular)
      (w) => WasmRasterizer.writeSetColorCommand(w, orange),
      (w) => WasmRasterizer.writeDrawRoundRectCommand(w, 50, 110, 100, 70, 40, 35),
    ]);

    const pixels = rasterizer.copySurfacePixels(surfaceId);
    saveToPPM(pixels, 200, 200, "draw_round_rects.ppm");

    rasterizer.destroyContext(contextId);
    rasterizer.freeSurface(surfaceId);
  }

  // Demo 4: Draw Polylines
  {
    const surfaceId = rasterizer.allocateSurface(200, 200);
    const contextId = rasterizer.createContext(surfaceId);

    const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
    const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);

    rasterizer.renderVariableLengthCommands(contextId, [
      // Zigzag pattern
      (w) => WasmRasterizer.writeSetColorCommand(w, red),
      (w) =>
        WasmRasterizer.writeDrawPolylineCommand(
          w,
          [20, 50, 20, 50, 20],
          [20, 40, 60, 80, 100],
        ),
      // Star pattern (not closed)
      (w) => WasmRasterizer.writeSetColorCommand(w, blue),
      (w) =>
        WasmRasterizer.writeDrawPolylineCommand(
          w,
          [150, 120, 180, 100, 170],
          [50, 80, 80, 110, 60],
        ),
    ]);

    const pixels = rasterizer.copySurfacePixels(surfaceId);
    saveToPPM(pixels, 200, 200, "draw_polylines.ppm");

    rasterizer.destroyContext(contextId);
    rasterizer.freeSurface(surfaceId);
  }

  // Demo 5: Copy Area
  {
    const surfaceId = rasterizer.allocateSurface(200, 200);
    const contextId = rasterizer.createContext(surfaceId);

    const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
    const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);
    const green = WasmRasterizer.makeARGB(255, 0, 255, 0);

    rasterizer.renderVariableLengthCommands(contextId, [
      // Draw original rectangles
      (w) => WasmRasterizer.writeSetColorCommand(w, red),
      (w) => WasmRasterizer.writeFillRectCommand(w, 20, 20, 30, 30),
      (w) => WasmRasterizer.writeSetColorCommand(w, blue),
      (w) => WasmRasterizer.writeFillRectCommand(w, 30, 30, 20, 20),
      // Copy to new locations
      (w) => WasmRasterizer.writeCopyAreaCommand(w, 20, 20, 40, 40, 80, 0),
      (w) => WasmRasterizer.writeCopyAreaCommand(w, 20, 20, 40, 40, 0, 80),
      (w) => WasmRasterizer.writeCopyAreaCommand(w, 20, 20, 40, 40, 80, 80),
      // Draw a green border around one copy to show it's a copy
      (w) => WasmRasterizer.writeSetColorCommand(w, green),
      (w) => WasmRasterizer.writeDrawRectCommand(w, 100, 100, 40, 40),
    ]);

    const pixels = rasterizer.copySurfacePixels(surfaceId);
    saveToPPM(pixels, 200, 200, "copy_area.ppm");

    rasterizer.destroyContext(contextId);
    rasterizer.freeSurface(surfaceId);
  }

  // Demo 6: Combined showcase
  {
    const surfaceId = rasterizer.allocateSurface(300, 300);
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
      // Draw various shapes in a grid
      (w) => WasmRasterizer.writeSetColorCommand(w, colors[0]),
      (w) => WasmRasterizer.writeDrawOvalCommand(w, 20, 20, 60, 60),
      (w) => WasmRasterizer.writeSetColorCommand(w, colors[1]),
      (w) => WasmRasterizer.writeDrawArcCommand(w, 120, 20, 60, 60, 0, 180),
      (w) => WasmRasterizer.writeSetColorCommand(w, colors[2]),
      (w) => WasmRasterizer.writeDrawRoundRectCommand(w, 220, 20, 60, 60, 15, 15),
      (w) => WasmRasterizer.writeSetColorCommand(w, colors[3]),
      (w) => WasmRasterizer.writeFillOvalCommand(w, 20, 120, 60, 60),
      (w) => WasmRasterizer.writeSetColorCommand(w, colors[4]),
      (w) => WasmRasterizer.writeFillArcCommand(w, 120, 120, 60, 60, 45, 90),
      (w) => WasmRasterizer.writeSetColorCommand(w, colors[5]),
      (w) => WasmRasterizer.writeFillRoundRectCommand(w, 220, 120, 60, 60, 20, 20),
    ]);

    const pixels = rasterizer.copySurfacePixels(surfaceId);
    saveToPPM(pixels, 300, 300, "showcase.ppm");

    rasterizer.destroyContext(contextId);
    rasterizer.freeSurface(surfaceId);
  }

  console.log("\n=== All demos complete! ===");
  console.log(`Output saved to: ${OUTPUT_DIR}`);
  console.log("\nTo view images, convert PPM to PNG with ImageMagick:");
  console.log(`  cd ${OUTPUT_DIR} && for f in *.ppm; do convert "$f" "\${f%.ppm}.png"; done\n`);
}

if (import.meta.main) {
  main();
}
