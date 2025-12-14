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

  // Draw three overlapping colored rectangles
  const cmdBuffer = rasterizer.createCommandBuffer(6);

  // Red rectangle
  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
  rasterizer.writeCommand(cmdBuffer, 0, WasmRasterizer.setColorCommand(red));
  rasterizer.writeCommand(cmdBuffer, 1, WasmRasterizer.fillRectCommand(2, 2, 8, 8));

  // Green rectangle (overlapping)
  const green = WasmRasterizer.makeARGB(255, 0, 255, 0);
  rasterizer.writeCommand(cmdBuffer, 2, WasmRasterizer.setColorCommand(green));
  rasterizer.writeCommand(cmdBuffer, 3, WasmRasterizer.fillRectCommand(6, 6, 8, 8));

  // Blue rectangle
  const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);
  rasterizer.writeCommand(cmdBuffer, 4, WasmRasterizer.setColorCommand(blue));
  rasterizer.writeCommand(cmdBuffer, 5, WasmRasterizer.fillRectCommand(10, 10, 8, 8));

  rasterizer.renderCommands(surfaceId, cmdBuffer, 6);
  printSurface(rasterizer, surfaceId);

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

  const cmdBuffer = rasterizer.createCommandBuffer(4);

  // Set clip rect to center 10x10 area
  rasterizer.writeCommand(cmdBuffer, 0, WasmRasterizer.setClipRectCommand(5, 5, 10, 10));

  // Set color to yellow
  const yellow = WasmRasterizer.makeARGB(255, 255, 255, 0);
  rasterizer.writeCommand(cmdBuffer, 1, WasmRasterizer.setColorCommand(yellow));

  // Try to fill entire surface (should be clipped to center)
  rasterizer.writeCommand(cmdBuffer, 2, WasmRasterizer.fillRectCommand(0, 0, 20, 20));

  // Draw a line that extends beyond clip (should be clipped)
  rasterizer.writeCommand(cmdBuffer, 3, WasmRasterizer.drawLineCommand(0, 10, 19, 10));

  rasterizer.renderCommands(surfaceId, cmdBuffer, 4);
  printSurface(rasterizer, surfaceId);

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

  const black = WasmRasterizer.makeARGB(255, 0, 0, 0);
  const white = WasmRasterizer.makeARGB(255, 255, 255, 255);

  const numCellsX = width / cellSize;
  const numCellsY = height / cellSize;
  const totalCells = numCellsX * numCellsY;
  const cmdBuffer = rasterizer.createCommandBuffer(totalCells * 2);

  let cmdIdx = 0;
  for (let row = 0; row < numCellsY; row++) {
    for (let col = 0; col < numCellsX; col++) {
      const color = ((row + col) % 2 === 0) ? black : white;
      rasterizer.writeCommand(cmdBuffer, cmdIdx++, WasmRasterizer.setColorCommand(color));
      rasterizer.writeCommand(cmdBuffer, cmdIdx++, 
        WasmRasterizer.fillRectCommand(col * cellSize, row * cellSize, cellSize, cellSize));
    }
  }

  rasterizer.renderCommands(surfaceId, cmdBuffer, cmdIdx);
  printSurface(rasterizer, surfaceId, 0x80);

  rasterizer.freeSurface(surfaceId);
}

/**
 * Demo 4: Image blitting
 */
async function demo4_ImageBlitting() {
  console.log("\n=== Demo 4: Image Blitting ===");
  
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  // Create a small stamp image (4x4 cross pattern)
  const imageId = rasterizer.registerImage(4, 4);
  const imagePixels = rasterizer.getImagePixels(imageId, 4, 4);
  
  const magenta = WasmRasterizer.makeARGB(255, 255, 0, 255);
  const crossPattern = [
    0, 1, 1, 0,
    1, 1, 1, 1,
    1, 1, 1, 1,
    0, 1, 1, 0,
  ];
  
  for (let i = 0; i < crossPattern.length; i++) {
    imagePixels[i] = crossPattern[i] ? magenta : 0;
  }

  // Create surface and blit the image multiple times
  const surfaceId = rasterizer.allocateSurface(16, 16);

  const cmdBuffer = rasterizer.createCommandBuffer(4);
  rasterizer.writeCommand(cmdBuffer, 0, WasmRasterizer.blitImageCommand(imageId, 0, 0));
  rasterizer.writeCommand(cmdBuffer, 1, WasmRasterizer.blitImageCommand(imageId, 6, 0));
  rasterizer.writeCommand(cmdBuffer, 2, WasmRasterizer.blitImageCommand(imageId, 0, 6));
  rasterizer.writeCommand(cmdBuffer, 3, WasmRasterizer.blitImageCommand(imageId, 6, 6));

  rasterizer.renderCommands(surfaceId, cmdBuffer, 4);
  printSurface(rasterizer, surfaceId);

  rasterizer.freeSurface(surfaceId);
}

/**
 * Demo 5: Drawing with transforms (translation)
 */
async function demo5_Transforms() {
  console.log("\n=== Demo 5: Transforms (Translation) ===");
  
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(20, 20);

  const cmdBuffer = rasterizer.createCommandBuffer(4);

  // Set a translation transform (shift by 5, 5)
  rasterizer.writeCommand(cmdBuffer, 0, WasmRasterizer.setTransformCommand(1, 0, 5, 0, 1, 5));

  // Set color
  const cyan = WasmRasterizer.makeARGB(255, 0, 255, 255);
  rasterizer.writeCommand(cmdBuffer, 1, WasmRasterizer.setColorCommand(cyan));

  // Draw at (0, 0) but should appear at (5, 5) due to transform
  rasterizer.writeCommand(cmdBuffer, 2, WasmRasterizer.fillRectCommand(0, 0, 6, 6));

  // Reset transform to identity
  rasterizer.writeCommand(cmdBuffer, 3, WasmRasterizer.setTransformCommand(1, 0, 0, 0, 1, 0));

  rasterizer.renderCommands(surfaceId, cmdBuffer, 4);
  printSurface(rasterizer, surfaceId);

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

  const cmdBuffer = rasterizer.createCommandBuffer(20);

  let cmdIdx = 0;

  // Background fill (light gray)
  const lightGray = WasmRasterizer.makeARGB(255, 200, 200, 200);
  rasterizer.writeCommand(cmdBuffer, cmdIdx++, WasmRasterizer.setColorCommand(lightGray));
  rasterizer.writeCommand(cmdBuffer, cmdIdx++, WasmRasterizer.fillRectCommand(0, 0, width, height));

  // Draw a border (dark gray)
  const darkGray = WasmRasterizer.makeARGB(255, 50, 50, 50);
  rasterizer.writeCommand(cmdBuffer, cmdIdx++, WasmRasterizer.setColorCommand(darkGray));
  rasterizer.writeCommand(cmdBuffer, cmdIdx++, WasmRasterizer.drawRectCommand(0, 0, width, height));

  // Draw some colored squares
  const colors = [
    WasmRasterizer.makeARGB(255, 255, 0, 0),   // red
    WasmRasterizer.makeARGB(255, 0, 255, 0),   // green
    WasmRasterizer.makeARGB(255, 0, 0, 255),   // blue
  ];

  for (let i = 0; i < colors.length; i++) {
    rasterizer.writeCommand(cmdBuffer, cmdIdx++, WasmRasterizer.setColorCommand(colors[i]));
    rasterizer.writeCommand(cmdBuffer, cmdIdx++, 
      WasmRasterizer.fillRectCommand(5 + i * 8, 5, 6, 6));
  }

  // Draw diagonal lines
  const yellow = WasmRasterizer.makeARGB(255, 255, 255, 0);
  rasterizer.writeCommand(cmdBuffer, cmdIdx++, WasmRasterizer.setColorCommand(yellow));
  rasterizer.writeCommand(cmdBuffer, cmdIdx++, WasmRasterizer.drawLineCommand(0, 0, width - 1, height - 1));
  rasterizer.writeCommand(cmdBuffer, cmdIdx++, WasmRasterizer.drawLineCommand(width - 1, 0, 0, height - 1));

  // Clear a small area in the center
  rasterizer.writeCommand(cmdBuffer, cmdIdx++, WasmRasterizer.clearRectCommand(12, 12, 6, 6));

  rasterizer.renderCommands(surfaceId, cmdBuffer, cmdIdx);
  printSurface(rasterizer, surfaceId, 0x20);

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
    await demo4_ImageBlitting();
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
