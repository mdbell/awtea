/**
 * Common test helper functions for WASM rasterizer tests
 *
 * This module provides shared utilities for testing pixel operations,
 * color comparisons, and assertions across multiple test files.
 */

import { assertEquals } from "@std/assert";

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
