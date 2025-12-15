/**
 * Demo/Example: Using the WASM Rasterizer
 * 
 * This file demonstrates practical usage of the WASM rasterizer
 * for various drawing scenarios. Run with: deno run --allow-read demo.ts
 */

import { WasmRasterizer, PixelFormat } from "./wasm_rasterizer.ts";

const WASM_PATH = "../../../build/wasm/awt_raster.wasm";

/**
 * Convert RGB to closest ANSI 256-color code
 */
function rgbToAnsi256(r: number, g: number, b: number): number {
  // For grayscale colors
  if (r === g && g === b) {
    if (r < 8) return 16;
    if (r > 248) return 231;
    return Math.round(((r - 8) / 247) * 24) + 232;
  }
  
  // For color values, map to 6x6x6 color cube (colors 16-231)
  const rIndex = Math.round(r / 255 * 5);
  const gIndex = Math.round(g / 255 * 5);
  const bIndex = Math.round(b / 255 * 5);
  
  return 16 + (36 * rIndex) + (6 * gIndex) + bIndex;
}

/**
 * Helper to print a small surface as colored ASCII art using ANSI escape sequences
 */
function printSurface(rasterizer: WasmRasterizer, surfaceId: number, threshold = 0x80000000) {
  const dims = rasterizer.getSurfaceDimensions(surfaceId);
  const pixels = rasterizer.copySurfacePixels(surfaceId);

  console.log(`\nSurface ${surfaceId} (${dims.width}x${dims.height}):`);
  for (let y = 0; y < dims.height; y++) {
    let row = "";
    for (let x = 0; x < dims.width; x++) {
      const pixel = pixels[y * dims.width + x];
      
      if (pixel > threshold) {
        // Extract color components
        const { r, g, b } = WasmRasterizer.extractARGB(pixel);
        
        // Use ANSI 256-color background
        const ansiColor = rgbToAnsi256(r, g, b);
        row += `\x1b[48;5;${ansiColor}m  \x1b[0m`;
      } else {
        // Empty/transparent pixel - use dots
        row += "··";
      }
    }
    console.log(row);
  }
}

/**
 * Helper to save surface as a simple PPM image file
 */
async function saveSurfaceAsPPM(rasterizer: WasmRasterizer, surfaceId: number, filename: string) {
  const dims = rasterizer.getSurfaceDimensions(surfaceId);
  const pixels = rasterizer.copySurfacePixels(surfaceId);

  let ppm = `P3\n${dims.width} ${dims.height}\n255\n`;
  
  for (let i = 0; i < pixels.length; i++) {
    const color = WasmRasterizer.extractARGB(pixels[i]);
    ppm += `${color.r} ${color.g} ${color.b} `;
    if ((i + 1) % dims.width === 0) ppm += "\n";
  }

  await Deno.writeTextFile(filename, ppm);
  console.log(`Saved surface to ${filename}`);
}

/**
 * Demo 1: Simple filled rectangles
 */
async function demo1_FilledRectangles() {
  console.log("\n=== Demo 1: Filled Rectangles ===");
  
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(20, 20);
  const contextId = rasterizer.createContext(surfaceId);

  // Draw three overlapping colored rectangles using the new context buffer API
  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
  const green = WasmRasterizer.makeARGB(255, 0, 255, 0);
  const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);

  rasterizer.renderCommandsToContext(contextId, [
    WasmRasterizer.setColorCommand(red),
    WasmRasterizer.fillRectCommand(2, 2, 8, 8),
    WasmRasterizer.setColorCommand(green),
    WasmRasterizer.fillRectCommand(6, 6, 8, 8),
    WasmRasterizer.setColorCommand(blue),
    WasmRasterizer.fillRectCommand(10, 10, 8, 8),
  ]);

  printSurface(rasterizer, surfaceId);

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

  rasterizer.renderCommandsToContext(contextId, [
    WasmRasterizer.setClipRectCommand(5, 5, 10, 10),
    WasmRasterizer.setColorCommand(yellow),
    // Try to fill entire surface (should be clipped to center)
    WasmRasterizer.fillRectCommand(0, 0, 20, 20),
    // Draw a line that extends beyond clip (should be clipped)
    WasmRasterizer.drawLineCommand(0, 10, 19, 10),
  ]);

  printSurface(rasterizer, surfaceId);

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
      commands.push(WasmRasterizer.setColorCommand(color));
      commands.push(WasmRasterizer.fillRectCommand(col * cellSize, row * cellSize, cellSize, cellSize));
    }
  }

  rasterizer.renderCommandsToContext(contextId, commands);
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
  rasterizer.renderCommandsToContext(stampContextId, [
    // Draw magenta cross
    WasmRasterizer.setColorCommand(magenta),
    WasmRasterizer.fillRectCommand(1, 0, 2, 1), // top
    WasmRasterizer.fillRectCommand(0, 1, 4, 2), // middle
    WasmRasterizer.fillRectCommand(1, 3, 2, 1), // bottom
  ]);

  // Create destination surface
  const destSurfaceId = rasterizer.allocateSurface(16, 16);
  const destContextId = rasterizer.createContext(destSurfaceId);

  // Use CMD_BLIT_IMAGE to blit the stamp surface to multiple positions
  // This is the proper way to copy one surface onto another
  rasterizer.renderCommandsToContext(destContextId, [
    WasmRasterizer.blitImageCommand(stampSurfaceId, 0, 0),
    WasmRasterizer.blitImageCommand(stampSurfaceId, 6, 0),
    WasmRasterizer.blitImageCommand(stampSurfaceId, 0, 6),
    WasmRasterizer.blitImageCommand(stampSurfaceId, 6, 6),
  ]);

  printSurface(rasterizer, destSurfaceId);

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

  rasterizer.renderCommandsToContext(contextId, [
    // Set a translation transform (shift by 5, 5)
    WasmRasterizer.setTransformCommand(1, 0, 5, 0, 1, 5),
    WasmRasterizer.setColorCommand(cyan),
    // Draw at (0, 0) but should appear at (5, 5) due to transform
    WasmRasterizer.fillRectCommand(0, 0, 6, 6),
    // Reset transform to identity
    WasmRasterizer.setTransformCommand(1, 0, 0, 0, 1, 0),
  ]);

  printSurface(rasterizer, surfaceId);

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

  const commands = [
    // Background fill (light gray)
    WasmRasterizer.setColorCommand(lightGray),
    WasmRasterizer.fillRectCommand(0, 0, width, height),
    // Draw a border (dark gray)
    WasmRasterizer.setColorCommand(darkGray),
    WasmRasterizer.drawRectCommand(0, 0, width, height),
    // Draw colored squares
    WasmRasterizer.setColorCommand(red),
    WasmRasterizer.fillRectCommand(5, 5, 6, 6),
    WasmRasterizer.setColorCommand(green),
    WasmRasterizer.fillRectCommand(13, 5, 6, 6),
    WasmRasterizer.setColorCommand(blue),
    WasmRasterizer.fillRectCommand(21, 5, 6, 6),
    // Draw diagonal lines
    WasmRasterizer.setColorCommand(yellow),
    WasmRasterizer.drawLineCommand(0, 0, width - 1, height - 1),
    WasmRasterizer.drawLineCommand(width - 1, 0, 0, height - 1),
    // Clear a small area in the center
    WasmRasterizer.clearRectCommand(12, 12, 6, 6),
  ];

  rasterizer.renderCommandsToContext(contextId, commands);
  printSurface(rasterizer, surfaceId, 0x20);

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

    console.log("\n✅ All demos completed successfully!");
    console.log("\nNote: This demo shows ASCII visualization of rendered surfaces.");
    console.log("For actual image output, implement PNG encoding or use the PPM helper.");
  } catch (error) {
    console.error("\n❌ Demo failed:", error);
    Deno.exit(1);
  }
}

// Run if this is the main module
if (import.meta.main) {
  await main();
}
