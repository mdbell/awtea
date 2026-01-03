#include "awt_fast_fill.h"
#include "awt_log.h"
#include "awt_util.h"
#include "awt_simd.h"
#include <string.h>

// Compile-time flag to enable SIMD optimizations
// Set to 1 to use SIMD (default if available)
// Set to 0 for scalar-only fallback
#ifndef USE_SIMD_FILLS
#ifdef __wasm_simd128__
#define USE_SIMD_FILLS 1
#else
#define USE_SIMD_FILLS 0
#endif
#endif

// Fast scanline fill using optimized memory operations
void fast_fill_scanline(uint32_t* framebuffer, int y, int stride,
                       int x_start, int x_end, uint32_t color) {
    if (x_end <= x_start) {
        return; // Nothing to fill
    }
    
    int count = x_end - x_start;
    
#if USE_SIMD_FILLS
    // SIMD path: Use vectorized fills for better performance
    simd_fill_scanline(framebuffer, y, stride, x_start, x_end, color);
#else
    // Scalar path: Original implementation
    uint32_t* row = &framebuffer[y * stride];
    
    // Strategy: Different approaches based on fill size
    if (count == 1) {
        // Single pixel: direct write
        row[x_start] = color;
    } else if (count <= 8) {
        // Small fill: unrolled loop (avoids loop overhead)
        uint32_t* p = &row[x_start];
        switch (count) {
            case 8: *p++ = color;  // Fall through
            case 7: *p++ = color;
            case 6: *p++ = color;
            case 5: *p++ = color;
            case 4: *p++ = color;
            case 3: *p++ = color;
            case 2: *p++ = color;
                    *p = color;
                    break;
        }
    } else {
        // Large fill: use optimized memory operations
        uint32_t* dest = &row[x_start];
        
        // Write 4 pixels at a time when count is large
        int i = 0;
        int count_div_4 = count / 4;
        for (; i < count_div_4; i++) {
            dest[0] = color;
            dest[1] = color;
            dest[2] = color;
            dest[3] = color;
            dest += 4;
        }
        
        // Handle remaining pixels (0-3)
        int remaining = count - (count_div_4 * 4);
        for (i = 0; i < remaining; i++) {
            *dest++ = color;
        }
    }
#endif
}

// Fast rectangle fill
void fast_fill_rect(SurfaceData* surface, int x, int y,
                   int width, int height, uint32_t color) {
    if (width <= 0 || height <= 0) {
        return; // Nothing to fill
    }
    
    // Clip to surface bounds
    if (x < 0) {
        width += x;
        x = 0;
    }
    if (y < 0) {
        height += y;
        y = 0;
    }
    if (x + width > (int)surface->width) {
        width = (int)surface->width - x;
    }
    if (y + height > (int)surface->height) {
        height = (int)surface->height - y;
    }
    
    if (width <= 0 || height <= 0) {
        return; // Clipped out entirely
    }
    
    uint32_t* framebuffer = (uint32_t*)(uintptr_t)surface->ptr;
    uint32_t stride = surface->stride / 4;
    
    int x_end = x + width;
    int y_end = y + height;
    
    log_trace("fast_fill_rect: [%d,%d] %dx%d (color=0x%08X)", x, y, width, height, color);
    
    // Strategy: Different approaches based on rectangle size
    if (width == (int)surface->width && x == 0) {
        // Full-width rows: can use single large fill
        // This is optimal for large clears (e.g., clear entire surface)
        int total_pixels = width * height;
        uint32_t* start = &framebuffer[y * stride];
        
        // Fill using scanline fill (will use optimized bulk ops)
        for (int i = 0; i < total_pixels; i++) {
            start[i] = color;
        }
    } else {
        // Partial-width rows: fill scanline by scanline
        // This is more common for UI elements and shapes
        for (int j = y; j < y_end; j++) {
            fast_fill_scanline(framebuffer, j, stride, x, x_end, color);
        }
    }
}

#ifdef AWTEA_DEBUG_BUILD
// Debug pattern fills for testing
void fast_fill_pattern(SurfaceData* surface, int x, int y,
                      int width, int height, int pattern_type) {
    uint32_t* framebuffer = (uint32_t*)(uintptr_t)surface->ptr;
    uint32_t stride = surface->stride / 4;
    
    for (int j = 0; j < height; j++) {
        for (int i = 0; i < width; i++) {
            uint32_t color;
            switch (pattern_type) {
                case 0: // Checkerboard
                    color = ((i + j) & 1) ? 0xFFFFFFFF : 0xFF000000;
                    break;
                case 1: // Horizontal gradient
                    color = 0xFF000000 | ((i * 255 / width) * 0x010101);
                    break;
                case 2: // Vertical gradient
                    color = 0xFF000000 | ((j * 255 / height) * 0x010101);
                    break;
                default:
                    color = 0xFFFF00FF; // Magenta
                    break;
            }
            framebuffer[(y + j) * stride + (x + i)] = color;
        }
    }
}
#endif
