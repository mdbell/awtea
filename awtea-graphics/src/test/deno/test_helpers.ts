/**
 * Common test helper functions for WASM rasterizer tests
 *
 * This module provides shared utilities for testing pixel operations,
 * color comparisons, and assertions across multiple test files.
 */

import { assertEquals } from "https://deno.land/std@0.224.0/assert/mod.ts";
import { WasmRasterizer } from "./wasm_rasterizer.ts";

/**
 * Maximum length for null-terminated strings read from WASM memory
 * Used as a safety limit to prevent infinite loops
 */
export const MAX_STRING_LENGTH = 1024;

/**
 * Decode a null-terminated string from WASM memory
 * 
 * @param memory - The WASM memory instance
 * @param ptr - Pointer to the start of the string in WASM memory
 * @returns The decoded string
 */
export function decodeNullTerminatedString(
  memory: WebAssembly.Memory,
  ptr: number,
): string {
  const view = new Uint8Array(memory.buffer);
  let len = 0;
  while (view[ptr + len] !== 0 && len < MAX_STRING_LENGTH) {
    len++;
  }
  const bytes = view.slice(ptr, ptr + len);
  return new TextDecoder().decode(bytes);
}

/**
 * Compare two pixel values as unsigned 32-bit integers
 * 
 * JavaScript bitwise operations are signed, so we convert to unsigned
 * before comparison to avoid issues with the sign bit.
 * 
 * @param actual - The actual pixel value
 * @param expected - The expected pixel value
 * @param message - Assertion failure message
 */
export function assertPixelEquals(
  actual: number,
  expected: number,
  message: string,
) {
  const actualU32 = actual >>> 0;
  const expectedU32 = expected >>> 0;
  assertEquals(actualU32, expectedU32, message);
}

/**
 * Unpack an ARGB pixel value into its color components
 * 
 * @param argb - The ARGB pixel value (0xAARRGGBB format)
 * @returns Object with a, r, g, b components (0-255)
 */
export function unpackARGB(
  argb: number,
): { a: number; r: number; g: number; b: number } {
  const argbU32 = argb >>> 0;
  return {
    a: (argbU32 >>> 24) & 0xFF,
    r: (argbU32 >>> 16) & 0xFF,
    g: (argbU32 >>> 8) & 0xFF,
    b: argbU32 & 0xFF,
  };
}

/**
 * Assert that two pixel values are approximately equal within a tolerance
 * 
 * Useful for testing blending operations where floating-point rounding
 * may introduce small differences in the final pixel values.
 * 
 * @param actual - The actual pixel value
 * @param expected - The expected pixel value
 * @param tolerance - Maximum allowed difference per channel (0-255)
 * @param message - Assertion failure message
 */
export function assertPixelApprox(
  actual: number,
  expected: number,
  tolerance: number,
  message: string,
) {
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
        `Max diff: ${maxDiff} (tolerance: ${tolerance})`,
    );
  }
}

/**
 * Convert RGB to closest ANSI 256-color code
 */
export function rgbToAnsi256(r: number, g: number, b: number): number {
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
 * Helper to print a surface as colored ASCII art using ANSI escape sequences
 */
export function printSurface(
  rasterizer: WasmRasterizer,
  surfaceId: number,
  title: string,
  threshold = 0x80000000,
) {
  const dims = rasterizer.getSurfaceDimensions(surfaceId);
  const pixels = rasterizer.copySurfacePixels(surfaceId);

  console.log(`\n${title} (${dims.width}x${dims.height}):`);
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
export async function saveSurfaceAsPPM(
  rasterizer: WasmRasterizer,
  surfaceId: number,
  filename: string,
) {
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
