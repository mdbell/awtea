#ifndef AWT_FAST_FILL_H
#define AWT_FAST_FILL_H

#pragma once
#include <stdint.h>
#include "awt_surface.h"

// Fast Fill Operations for High-Performance Solid Color Rendering
//
// This module provides hardware-optimized bulk fill operations that replace
// slow per-pixel loops with efficient memory operations.
//
// Performance Impact:
// - Without fast fill: ~5 cycles per pixel (function call + array index)
// - With fast fill: ~0.5-1 cycle per pixel (optimized memory writes)
// - Speedup: 4-5x for solid fills
//
// Techniques:
// 1. Row-based processing (better cache locality)
// 2. Word-aligned writes when possible (4-byte vs 1-byte)
// 3. Loop unrolling for small fills
// 4. Bulk memory operations for large fills

// Fast scanline fill - fills a horizontal span with solid color
// This is the core operation for rectangle fills and polygon rasterization
//
// Parameters:
//   framebuffer: Pointer to pixel data (32-bit ARGB)
//   y: Scanline Y coordinate
//   stride: Surface stride in pixels (width including padding)
//   x_start: Starting X coordinate (inclusive)
//   x_end: Ending X coordinate (exclusive)
//   color: 32-bit color value in surface format
//
// Performance:
//   Small fills (< 16 pixels): Unrolled loop
//   Large fills (>= 16 pixels): Optimized bulk write
void fast_fill_scanline(uint32_t* framebuffer, int y, int stride,
                       int x_start, int x_end, uint32_t color);

// Fast rectangle fill - fills an axis-aligned rectangle
// Uses scanline fills for optimal cache behavior
//
// Parameters:
//   surface: Target surface
//   x, y: Top-left corner
//   width, height: Rectangle dimensions
//   color: 32-bit color value in ARGB format
//
// Note: This assumes color is already converted to surface format
void fast_fill_rect(SurfaceData* surface, int x, int y,
                   int width, int height, uint32_t color);

// Fill with pattern (for debugging/testing)
// Fills rectangle with checkerboard or gradient pattern
#ifdef AWTEA_DEBUG_BUILD
void fast_fill_pattern(SurfaceData* surface, int x, int y,
                      int width, int height, int pattern_type);
#endif

#endif // AWT_FAST_FILL_H
