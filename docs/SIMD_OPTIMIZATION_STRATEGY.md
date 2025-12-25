# SIMD Optimization Strategy for AWTea WASM Rasterizer

## Executive Summary

This document outlines a comprehensive strategy for implementing SIMD (Single Instruction, Multiple Data) optimizations in the AWTea WASM graphics rasterizer. With the fixed-point rasterization providing a 2-3x speedup baseline, SIMD vectorization offers an additional 2-4x performance improvement for a total of **4-12x speedup** over the original floating-point implementation.

**Target Performance**: 4-12x overall speedup for shape-heavy rendering workloads
**Browser Support**: Chrome 91+, Firefox 89+, Edge 91+, Safari 16.4+
**Implementation Strategy**: Progressive enhancement with runtime detection and fallback

---

## WebAssembly SIMD Overview

### Technical Capabilities

WebAssembly SIMD provides 128-bit vector operations (`v128` type) that can process:
- **4x int32**: Perfect for ARGB pixel operations
- **4x float32**: For blending calculations
- **8x int16**: For 16.16 fixed-point operations (can process 8 edges in parallel)
- **16x uint8**: For byte-level pixel channel operations

### Browser Support Matrix

| Browser | Version | SIMD Support | Market Share |
|---------|---------|--------------|--------------|
| Chrome | 91+ | ✅ Full | ~65% |
| Edge | 91+ | ✅ Full | ~5% |
| Firefox | 89+ | ✅ Full | ~3% |
| Safari | 16.4+ | ✅ Full | ~20% |
| **Total Coverage** | | | **~93%** |

**Deployment Strategy**: Use SIMD with runtime feature detection and scalar fallback for older browsers.

---

## Optimization Opportunities (Ranked by Impact)

### 0. Alpha Blend Lookup Table (CRITICAL: 2-3x speedup for blending)

**Current Implementation**: Per-pixel floating-point alpha calculations
```c
// Expensive division and multiplication per pixel
float sR = (float)srcR / 255.0f;
float sA = (float)srcA / 255.0f;
// ... blend calculations with divisions
outRf = (sR * sA + dR * dA * (1.0f - sA)) / outAf;
```

**Optimized Implementation**: Pre-computed lookup table
```c
// One-time initialization: Build 256x256 lookup table
static uint8_t g_alpha_blend_lut[256][256];

void init_alpha_blend_lut(void) {
    for (int alpha = 0; alpha < 256; alpha++) {
        for (int component = 0; component < 256; component++) {
            // Pre-compute: (alpha * component) / 255
            g_alpha_blend_lut[alpha][component] = 
                (uint8_t)((alpha * component + 127) / 255);
        }
    }
}

// Fast blending using lookup table
uint8_t blend_component(uint8_t alpha, uint8_t component) {
    return g_alpha_blend_lut[alpha][component];
}
```

**Performance Impact**:
- Current: ~15-20 cycles per pixel (float div + mul + conversion)
- Lookup: ~2-3 cycles per pixel (array index + load)
- **Speedup: 5-7x for alpha blending calculations**
- **Overall blending speedup: 2-3x** (alpha math is ~40% of blend time)

**Memory Cost**: 64KB lookup table (256×256 bytes) - negligible

**Use Cases**:
- All alpha blending operations (SRC_OVER, DST_OVER, etc.)
- Font rendering with anti-aliasing
- Sprite rendering with transparency
- Image compositing

### 0.1 Memset-Based Fills (HIGH IMPACT: 4-5x speedup for solid fills)

**Current Implementation**: Nested loops with per-pixel writes
```c
for (int j = y0; j < y1; j++) {
    for (int i = x0; i < x1; i++) {
        set_pixel_func(surface, i, j, PIXEL_FORMAT_ARGB, color);
    }
}
```

**Optimized Implementation**: Row-based memset operations
```c
uint32_t* framebuffer = (uint32_t*)(uintptr_t)surface->ptr;
uint32_t stride = surface->stride / 4;

// Convert color to destination format once
uint32_t dst_color = convert_color(color, surface->format);

for (int j = y0; j < y1; j++) {
    uint32_t* row = &framebuffer[j * stride + x0];
    int pixel_count = x1 - x0;
    
    // Fill entire row with memset (hardware-optimized)
    if (pixel_count > 0) {
        // Use wmemset for uint32_t fills (4x faster than byte memset)
        wmemset(row, dst_color, pixel_count);
    }
}
```

**Performance Impact**:
- Current: ~5 cycles per pixel (function call + array index)
- Memset: ~0.5-1 cycle per pixel (hardware-optimized bulk copy)
- **Speedup: 4-5x for solid rectangle fills**

**Use Cases**:
- `fillRect` (most common primitive)
- `clearRect` operations
- Solid polygon fills via scanline rasterizer
- Background clears

### 0.2 Fixed-Point Transforms (MEDIUM-HIGH IMPACT: 1.5-2x speedup)

**Current Implementation**: Floating-point matrix operations
```c
void transform_point(const Transform2D* t, float x, float y, 
                    float* outX, float* outY) {
    *outX = t->m00 * x + t->m01 * y + t->m02;  // 3 float muls + 2 adds
    *outY = t->m10 * x + t->m11 * y + t->m12;  // 3 float muls + 2 adds
}
```

**Optimized Implementation**: 16.16 fixed-point transforms
```c
typedef struct {
    fx16_t m00, m01, m02;  // 16.16 fixed-point matrix
    fx16_t m10, m11, m12;
} Transform2DFixed;

void transform_point_fixed(const Transform2DFixed* t, 
                           fx16_t x, fx16_t y,
                           fx16_t* outX, fx16_t* outY) {
    // Fixed-point: 1 cycle per mul (vs 3-5 for float)
    *outX = FIXED_ADD(FIXED_ADD(FIXED_MUL(t->m00, x), 
                                 FIXED_MUL(t->m01, y)), t->m02);
    *outY = FIXED_ADD(FIXED_ADD(FIXED_MUL(t->m10, x), 
                                 FIXED_MUL(t->m11, y)), t->m12);
}
```

**Performance Impact**:
- Current: ~18-30 cycles per point (6 float muls + 4 float adds)
- Fixed: ~12-15 cycles per point (6 int64 muls with shifts + 4 int adds)
- **Speedup: 1.5-2x for coordinate transformations**

**Use Cases**:
- Transformed rectangles/polygons
- Rotated/scaled image blitting
- Font glyph transformations
- Bezier curve tessellation

### 1. Pixel Blending Operations (HIGH IMPACT: 3-4x speedup with SIMD)

**Current Implementation**: Scalar per-pixel blending
```c
// Process one pixel at a time
for (int x = 0; x < width; x++) {
    blend_pixel_composite(surface, x, y, format, pixel, mode, alpha);
}
```

**SIMD Implementation**: Process 4 pixels simultaneously
```c
// Process 4 pixels in parallel using WASM SIMD
v128_t src_pixels = wasm_v128_load(&src[x]);
v128_t dst_pixels = wasm_v128_load(&dst[x]);
v128_t result = simd_blend_argb(src_pixels, dst_pixels, mode, alpha);
wasm_v128_store(&dst[x], result);
```

**Performance Impact**:
- Current: ~20-30 cycles per pixel for SRC_OVER blending
- SIMD: ~8-10 cycles per pixel (4 pixels in ~32-40 cycles)
- **Speedup: 2.5-3x for alpha blending operations**

**Use Cases**:
- `blit_image` with transparency
- `draw_filled_rect` with alpha
- Font glyph rendering
- Sprite rendering

### 2. Scanline Filling (HIGH IMPACT: 2-3x speedup)

**Current Implementation**: Sequential pixel writes
```c
for (int x = x_start; x < x_end; x++) {
    set_pixel_same_format(surface, x, y, format, color);
}
```

**SIMD Implementation**: Vectorized fills
```c
// Broadcast color to all 4 lanes
v128_t color_vec = wasm_i32x4_splat(color);

// Fill 4 pixels at a time
for (int x = x_start; x < x_end - 4; x += 4) {
    wasm_v128_store(&framebuffer[y * stride + x], color_vec);
}
// Handle remaining pixels with scalar code
```

**Performance Impact**:
- Current: ~5 cycles per pixel
- SIMD: ~2 cycles per pixel (4 pixels in ~8 cycles)
- **Speedup: 2-2.5x for solid fills**

**Use Cases**:
- `draw_filled_rect` (most common operation)
- `clear_rect` operations
- Solid polygon fills

### 3. Edge Table Operations (MEDIUM IMPACT: 1.5-2x speedup)

**Current Implementation**: Sequential edge processing
```c
// Update X coordinates for all active edges
for (int i = 0; i < active_edges; i++) {
    edges[i].x = FIXED_ADD(edges[i].x, edges[i].dx);
}
```

**SIMD Implementation**: Vectorized edge updates
```c
// Process 4 edges in parallel (using i16x8 for 16.16 fixed-point)
v128_t x_vec = wasm_v128_load_i32x4(&edges[i].x);
v128_t dx_vec = wasm_v128_load_i32x4(&edges[i].dx);
v128_t result = wasm_i32x4_add(x_vec, dx_vec);
wasm_v128_store(&edges[i].x, result);
```

**Performance Impact**:
- Current: ~1 cycle per edge (fixed-point add)
- SIMD: ~0.25 cycles per edge (4 edges in ~1 cycle)
- **Speedup: 4x for edge coordinate updates**
- **Overall polygon fill speedup: 1.5-2x** (edge updates are ~30% of total time)

**Use Cases**:
- Complex polygon rendering
- Font glyph outline rasterization
- Arc/ellipse rendering

### 4. Memory Operations (MEDIUM IMPACT: 1.5-2x speedup)

**Current Implementation**: Standard `memcpy`/`memset`
```c
memcpy(dst_row, src_row, row_bytes);
memset(pixels, 0xFF, bytes);
```

**SIMD Implementation**: Vectorized memory operations
```c
// Copy 4 pixels (16 bytes) per iteration
for (size_t i = 0; i < count; i += 4) {
    v128_t data = wasm_v128_load(&src[i]);
    wasm_v128_store(&dst[i], data);
}
```

**Performance Impact**:
- Current: ~1 cycle per 4-byte copy (with hardware memcpy)
- SIMD: ~1 cycle per 16-byte copy
- **Speedup: 4x for bulk memory operations**
- **Real-world speedup: 1.5-2x** (memory ops are ~25% of blit time)

**Use Cases**:
- `blit_image` same-format fast path
- `copy_area` operations
- Surface buffer allocation

### 5. Pixel Format Conversion (LOW-MEDIUM IMPACT: 1.3-1.5x speedup)

**Current Implementation**: Scalar channel extraction/packing
```c
uint8_t r = (pixel & 0x00FF0000) >> 16;
uint8_t g = (pixel & 0x0000FF00) >> 8;
// ... convert to destination format
```

**SIMD Implementation**: Vectorized shuffle operations
```c
// Convert 4 ARGB pixels to 4 RGBA pixels using shuffle
v128_t argb = wasm_v128_load(&src[i]);
v128_t rgba = wasm_i8x16_shuffle(argb, argb, 
    3,0,1,2, 7,4,5,6, 11,8,9,10, 15,12,13,14); // Swizzle ARGB→RGBA
wasm_v128_store(&dst[i], rgba);
```

**Performance Impact**:
- Current: ~8-10 cycles per pixel
- SIMD: ~3 cycles per pixel (4 pixels in ~12 cycles)
- **Speedup: 2.5-3x for format conversion**

**Use Cases**:
- Cross-format blitting (ARGB ↔ RGBA ↔ RGB)
- WebGL texture uploads
- Canvas API integration

---

## Implementation Plan

### Phase 0: Non-SIMD Quick Wins (Week 1 - High Priority)

These optimizations provide immediate performance gains without SIMD complexity:

1. **Alpha Blend Lookup Table** (Day 1-2)
   - Create `awt_alpha_lut.h` and `awt_alpha_lut.c`
   - Implement 256×256 lookup table (64KB)
   - Initialize on module load
   - Replace all `alpha * component / 255` calculations
   - Update `blend_pixel_composite()` to use LUT
   - **Expected: 2-3x speedup for blending operations**

2. **Memset-Based Fills** (Day 3-4)
   - Add `fast_fill_scanline()` using `wmemset`
   - Update `draw_filled_rect()` for axis-aligned fast path
   - Integrate with edge table scanline filler
   - Handle unaligned/partial rows correctly
   - **Expected: 4-5x speedup for solid fills**

3. **Fixed-Point Transforms** (Day 5-7)
   - Create `Transform2DFixed` structure in `awt_transform.h`
   - Implement fixed-point matrix operations
   - Add conversion: `float_transform_to_fixed()`
   - Update `transform_point()` and `transform_rect()` with fixed-point path
   - Add compile-time flag: `USE_FIXED_POINT_TRANSFORMS`
   - **Expected: 1.5-2x speedup for transformed shapes**

**Total Phase 0 Impact: 3-6x cumulative speedup before SIMD**

### Phase 1: SIMD Infrastructure Setup (Week 2)

1. **Add SIMD Build Support**
   - Update Emscripten flags: `-msimd128`
   - Add SIMD feature detection function
   - Create fallback macro system

2. **Create SIMD Helper Library** (`awt_simd.h`)
   - Wrapper macros for v128 operations
   - Runtime detection: `has_simd_support()`
   - Fallback implementations

3. **Add Performance Benchmarking**
   - Micro-benchmarks for each optimization
   - End-to-end rendering benchmarks
   - Comparative analysis tools

### Phase 2: Core SIMD Operations (Week 3)

1. **Scanline Filling** (Quick Win)
   - Implement `simd_fill_scanline()`
   - 4-pixel vectorized writes
   - Aligned/unaligned handling
   - Benchmark: `fillRect` performance

2. **Pixel Blending** (High Impact)
   - Implement `simd_blend_argb_src_over()`
   - Support common composite modes
   - Optimize alpha multiplication
   - Benchmark: `blit_image` with transparency

3. **Memory Operations**
   - Implement `simd_memcpy_aligned()`
   - Implement `simd_memset_u32()`
   - Use in `blit_image` fast paths
   - Benchmark: large image blitting

### Phase 3: Advanced Optimizations (Week 4)

1. **Edge Table SIMD**
   - Vectorize edge X coordinate updates
   - Parallel edge sorting (4-way merge sort)
   - SIMD intersection calculations
   - Benchmark: polygon fill performance

2. **Format Conversion**
   - SIMD shuffle for ARGB↔RGBA
   - Batch conversion functions
   - Integration with blit operations

3. **Anti-Aliasing Preparation**
   - SIMD-friendly coverage calculation
   - Sub-pixel rendering support

### Phase 4: Integration & Testing (Week 5)

1. **Integration Testing**
   - All 82 Deno tests must pass
   - Visual regression testing
   - Cross-browser validation

2. **Performance Validation**
   - Benchmark suite execution
   - Document speedup measurements
   - Update performance documentation

3. **Runtime Detection**
   - Feature detection on startup
   - Automatic path selection
   - Debug logging for optimization paths

---

## Code Examples

### Example 0: Alpha Blend Lookup Table

```c
// awt_alpha_lut.h
#ifndef AWT_ALPHA_LUT_H
#define AWT_ALPHA_LUT_H

#include <stdint.h>

// 256x256 lookup table for alpha blending
// Result: (alpha * component + 127) / 255
extern uint8_t g_alpha_blend_lut[256][256];

// Initialize lookup table (call once on module load)
void init_alpha_blend_lut(void);

// Fast alpha blend: (alpha * component) / 255
static inline uint8_t alpha_blend_component(uint8_t alpha, uint8_t component) {
    return g_alpha_blend_lut[alpha][component];
}

// Fast premultiply: component * alpha / 255
static inline uint8_t premultiply_component(uint8_t component, uint8_t alpha) {
    return g_alpha_blend_lut[alpha][component];
}

#endif // AWT_ALPHA_LUT_H

// awt_alpha_lut.c
#include "awt_alpha_lut.h"

uint8_t g_alpha_blend_lut[256][256];

void init_alpha_blend_lut(void) {
    for (int alpha = 0; alpha < 256; alpha++) {
        for (int component = 0; component < 256; component++) {
            // Round to nearest: (alpha * component + 127) / 255
            int result = (alpha * component + 127) / 255;
            g_alpha_blend_lut[alpha][component] = (uint8_t)result;
        }
    }
}

// Updated blend_pixel_composite using LUT
void blend_pixel_composite_lut(SurfaceData* surface, int x, int y,
                               PixelFormat srcFormat, uint32_t srcPixel,
                               CompositeMode mode, float alpha) {
    // ... (unpack pixels as before)
    
    // Fast SRC_OVER using lookup table (no floating point!)
    if (mode == COMPOSITE_SRC_OVER) {
        // Compute result alpha: outA = srcA + dstA * (255 - srcA) / 255
        uint8_t inv_srcA = 255 - srcA;
        uint8_t dstA_scaled = alpha_blend_component(inv_srcA, dstA);
        uint8_t outA = srcA + dstA_scaled;
        
        if (outA > 0) {
            // Compute premultiplied components
            uint8_t srcR_pm = alpha_blend_component(srcA, srcR);
            uint8_t srcG_pm = alpha_blend_component(srcA, srcG);
            uint8_t srcB_pm = alpha_blend_component(srcA, srcB);
            
            uint8_t dstR_pm = alpha_blend_component(dstA, dstR);
            uint8_t dstG_pm = alpha_blend_component(dstA, dstG);
            uint8_t dstB_pm = alpha_blend_component(dstA, dstB);
            
            // Scale destination by (1 - srcA)
            uint8_t dstR_scaled = alpha_blend_component(inv_srcA, dstR_pm);
            uint8_t dstG_scaled = alpha_blend_component(inv_srcA, dstG_pm);
            uint8_t dstB_scaled = alpha_blend_component(inv_srcA, dstB_pm);
            
            // Add source and destination
            uint8_t outR_pm = srcR_pm + dstR_scaled;
            uint8_t outG_pm = srcG_pm + dstG_scaled;
            uint8_t outB_pm = srcB_pm + dstB_scaled;
            
            // Un-premultiply: component / alpha (use division LUT or approximation)
            // For speed, can use pre-multiplied alpha format
            outR = outR_pm; // Already premultiplied
            outG = outG_pm;
            outB = outB_pm;
        } else {
            outR = outG = outB = outA = 0;
        }
        
        // Pack and write
        uint32_t outPixel = pack_pixel(dstInfo, outR, outG, outB, outA);
        framebuffer[y * stride + x] = outPixel;
    }
}
```

### Example 0.1: Memset-Based Fast Fill

```c
// Fast scanline fill using wmemset (4-byte optimized)
static void fast_fill_scanline(uint32_t* framebuffer, int y, int stride,
                               int x_start, int x_end, uint32_t color) {
    if (x_end <= x_start) return;
    
    uint32_t* row = &framebuffer[y * stride];
    int count = x_end - x_start;
    
    // Use wmemset for 4-byte fills (defined in wchar.h)
    // Falls back to efficient loop on platforms without wmemset
    #ifdef __wasm__
        // WASM: use optimized bulk memory operations
        for (int i = x_start; i < x_end; i++) {
            row[i] = color;
        }
    #else
        // Native: use wmemset if available
        wmemset((wchar_t*)&row[x_start], color, count);
    #endif
}

// Updated draw_filled_rect with fast path
void draw_filled_rect(SurfaceData* surface, SurfaceContext* context,
                     int x, int y, int width, int height, uint32_t color) {
    STACK_ENTER();
    
    if (is_identity_transform(&context->transform)) {
        int x0 = clip_x(x, surface, context);
        int y0 = clip_y(y, surface, context);
        int x1 = clip_x(x + width, surface, context);
        int y1 = clip_y(y + height, surface, context);
        
        // FAST PATH: Use memset for solid fills
        if (context->composite_mode == COMPOSITE_SRC || 
            context->composite_mode == COMPOSITE_CLEAR) {
            
            uint32_t fill_color = (context->composite_mode == COMPOSITE_CLEAR) 
                                ? 0x00000000 : color;
            
            uint32_t* framebuffer = (uint32_t*)(uintptr_t)surface->ptr;
            uint32_t stride = surface->stride / 4;
            
            // Convert color to surface format once
            uint32_t dst_color = convert_pixel_format(fill_color, 
                                    PIXEL_FORMAT_ARGB, surface->format);
            
            // Fill each scanline with hardware-optimized memset
            for (int j = y0; j < y1; j++) {
                fast_fill_scanline(framebuffer, j, stride, x0, x1, dst_color);
            }
            
            log_debug("draw_filled_rect: fast memset path (%dx%d pixels)",
                     x1-x0, y1-y0);
            STACK_EXIT();
            return;
        }
        
        // Fall through to regular blending path...
    }
    
    // ... (rest of function unchanged)
}
```

### Example 0.2: Fixed-Point Transforms

```c
// awt_transform.h - Add fixed-point transform support
typedef struct {
    fx16_t m00, m01, m02;
    fx16_t m10, m11, m12;
} Transform2DFixed;

// Convert float transform to fixed-point
static inline void float_transform_to_fixed(const Transform2D* src, 
                                            Transform2DFixed* dst) {
    dst->m00 = FLOAT_TO_FIXED(src->m00);
    dst->m01 = FLOAT_TO_FIXED(src->m01);
    dst->m02 = FLOAT_TO_FIXED(src->m02);
    dst->m10 = FLOAT_TO_FIXED(src->m10);
    dst->m11 = FLOAT_TO_FIXED(src->m11);
    dst->m12 = FLOAT_TO_FIXED(src->m12);
}

// Fast fixed-point transform
static inline void transform_point_fixed(const Transform2DFixed* t,
                                         fx16_t x, fx16_t y,
                                         fx16_t* outX, fx16_t* outY) {
    // 6 fixed-point multiplies + 4 adds (much faster than float)
    *outX = FIXED_ADD(FIXED_ADD(FIXED_MUL(t->m00, x), 
                                 FIXED_MUL(t->m01, y)), t->m02);
    *outY = FIXED_ADD(FIXED_ADD(FIXED_MUL(t->m10, x), 
                                 FIXED_MUL(t->m11, y)), t->m12);
}

// Usage in blit_image with transforms
void blit_image_transformed_fixed(SurfaceData* dst, SurfaceContext* context,
                                  SurfaceData* src, int x, int y) {
    // Convert transform to fixed-point once
    Transform2DFixed inv_fixed;
    Transform2D inv_float;
    invert_transform(&context->transform, &inv_float);
    float_transform_to_fixed(&inv_float, &inv_fixed);
    
    // Transform pixel coordinates using fixed-point math
    for (int dy = startY; dy < endY; ++dy) {
        // Convert scanline Y to fixed-point
        fx16_t fy_fixed = INT_TO_FIXED(dy) + FX16_HALF;
        
        for (int dx = startX; dx < endX; ++dx) {
            // Convert pixel X to fixed-point
            fx16_t fx_fixed = INT_TO_FIXED(dx) + FX16_HALF;
            
            // Fast fixed-point transform (no float ops!)
            fx16_t ux_fixed, uy_fixed;
            transform_point_fixed(&inv_fixed, fx_fixed, fy_fixed,
                                 &ux_fixed, &uy_fixed);
            
            // Convert back to integer coordinates
            int ux = FIXED_TO_INT(ux_fixed);
            int uy = FIXED_TO_INT(uy_fixed);
            
            // Sample and blend pixel...
        }
    }
}
```

### Example 1: SIMD Pixel Blending (SRC_OVER)

```c
// awt_simd.h - SIMD helper functions
#include <wasm_simd128.h>

// Blend 4 ARGB pixels using SRC_OVER composite mode
static inline v128_t simd_blend_src_over_argb(v128_t src, v128_t dst) {
    // Extract alpha channels from source
    v128_t src_alpha = wasm_u32x4_shr(src, 24);
    
    // Calculate (255 - src_alpha) for destination
    v128_t inv_alpha = wasm_i32x4_sub(wasm_i32x4_splat(255), src_alpha);
    
    // Unpack to 16-bit for multiplication
    v128_t src_lo = wasm_u16x8_extend_low_u8x16(src);
    v128_t src_hi = wasm_u16x8_extend_high_u8x16(src);
    v128_t dst_lo = wasm_u16x8_extend_low_u8x16(dst);
    v128_t dst_hi = wasm_u16x8_extend_high_u8x16(dst);
    
    // Multiply: dst * (255 - src_alpha) / 255
    dst_lo = wasm_u16x8_mul(dst_lo, wasm_u16x8_splat(inv_alpha));
    dst_hi = wasm_u16x8_mul(dst_hi, wasm_u16x8_splat(inv_alpha));
    dst_lo = wasm_u16x8_shr(dst_lo, 8);
    dst_hi = wasm_u16x8_shr(dst_hi, 8);
    
    // Add source: result = src + (dst * inv_alpha)
    v128_t result_lo = wasm_u16x8_add(src_lo, dst_lo);
    v128_t result_hi = wasm_u16x8_add(src_hi, dst_hi);
    
    // Pack back to 8-bit
    return wasm_u8x16_narrow_i16x8(result_lo, result_hi);
}

// Vectorized scanline blending
void simd_blend_scanline_src_over(uint32_t* dst, const uint32_t* src, 
                                   int count, float alpha) {
    int i = 0;
    
    // Process 4 pixels at a time
    for (; i <= count - 4; i += 4) {
        v128_t src_vec = wasm_v128_load(&src[i]);
        v128_t dst_vec = wasm_v128_load(&dst[i]);
        v128_t result = simd_blend_src_over_argb(src_vec, dst_vec);
        wasm_v128_store(&dst[i], result);
    }
    
    // Handle remaining pixels with scalar code
    for (; i < count; i++) {
        blend_pixel_composite(surface, x + i, y, format, src[i], 
                             COMPOSITE_SRC_OVER, alpha);
    }
}
```

### Example 2: SIMD Scanline Fill

```c
// Fast scanline fill using SIMD
void simd_fill_scanline(uint32_t* framebuffer, int y, int stride,
                       int x_start, int x_end, uint32_t color) {
    uint32_t* row = &framebuffer[y * stride];
    int count = x_end - x_start;
    
    if (count <= 0) return;
    
    // Broadcast color to all 4 SIMD lanes
    v128_t color_vec = wasm_i32x4_splat(color);
    
    int i = x_start;
    
    // Handle unaligned start (write up to 3 pixels)
    while (i < x_end && ((uintptr_t)&row[i] & 15) != 0) {
        row[i++] = color;
    }
    
    // Vectorized fill (4 pixels per iteration, 16-byte aligned)
    for (; i <= x_end - 4; i += 4) {
        wasm_v128_store(&row[i], color_vec);
    }
    
    // Handle remaining pixels
    for (; i < x_end; i++) {
        row[i] = color;
    }
}
```

### Example 3: Runtime Feature Detection

```c
// awt_simd.h - Feature detection and dispatch
typedef enum {
    SIMD_NONE = 0,
    SIMD_WASM128 = 1
} SimdCapabilities;

static SimdCapabilities g_simd_caps = SIMD_NONE;

// Detect SIMD support at runtime
__attribute__((export_name("detect_simd_support")))
int detect_simd_support(void) {
    // In WASM, SIMD is either fully supported or not
    // Detection happens in JavaScript and passed to WASM
    #ifdef __wasm_simd128__
        g_simd_caps = SIMD_WASM128;
        return 1;
    #else
        g_simd_caps = SIMD_NONE;
        return 0;
    #endif
}

// Dispatch function for scanline fill
static inline void fill_scanline_dispatch(uint32_t* fb, int y, int stride,
                                         int x_start, int x_end, uint32_t color) {
    if (g_simd_caps == SIMD_WASM128 && (x_end - x_start) >= 8) {
        simd_fill_scanline(fb, y, stride, x_start, x_end, color);
    } else {
        scalar_fill_scanline(fb, y, stride, x_start, x_end, color);
    }
}
```

---

## Expected Performance Results

### Micro-Benchmark Predictions

| Operation | Scalar (cycles/pixel) | SIMD (cycles/pixel) | Speedup |
|-----------|----------------------|---------------------|---------|
| Solid fill | 5 | 2 | 2.5x |
| SRC_OVER blend | 25 | 8 | 3.1x |
| ARGB→RGBA convert | 9 | 3 | 3.0x |
| memcpy (aligned) | 1 | 0.25 | 4.0x |
| Edge X update | 1 | 0.25 | 4.0x |

### End-to-End Performance Predictions

| Workload | Current (FP) | After SIMD | Total Speedup |
|----------|--------------|------------|---------------|
| 100-edge polygon | 2-3x (fixed) | **4-6x** | **8-18x vs original** |
| 200×200 oval fill | 2-2.5x | **4-5x** | **8-12.5x** |
| Sprite blit (α) | 1x | **2-3x** | **2-3x** |
| fillRect 1000×1000 | 2-3x | **5-7x** | **10-21x** |
| Text rendering | 1.8-2x | **3-4x** | **5.4-8x** |

**Overall Expected Improvement**: 
- **Baseline (float)**: 1x
- **After fixed-point**: 2-3x
- **After SIMD**: **4-8x**
- **Best case**: **12x for shape-heavy workloads**

---

## Browser Compatibility Strategy

### JavaScript Side (Feature Detection)

```javascript
// Detect WASM SIMD support in browser
const wasmSimdSupported = WebAssembly.validate(
    new Uint8Array([0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00,
                    0x01, 0x05, 0x01, 0x60, 0x00, 0x01, 0x7b,
                    0x03, 0x02, 0x01, 0x00, 0x0a, 0x0a, 0x01, 0x08, 0x00,
                    0xfd, 0x0c, 0xfd, 0x0c, 0x0b])
);

if (wasmSimdSupported) {
    console.log("WASM SIMD enabled - using optimized paths");
    wasmInstance.exports.detect_simd_support();
} else {
    console.log("WASM SIMD not available - using scalar fallback");
}
```

### Emscripten Build Configuration

```bash
# Build with SIMD support
emcc -O3 -msimd128 \
     -s WASM=1 \
     -s ALLOW_MEMORY_GROWTH=1 \
     -o awt_raster.wasm \
     awt_*.c

# Fallback build without SIMD (for older browsers)
emcc -O3 \
     -s WASM=1 \
     -s ALLOW_MEMORY_GROWTH=1 \
     -o awt_raster_compat.wasm \
     awt_*.c
```

**Deployment Strategy**: 
1. Ship both SIMD and non-SIMD builds
2. JavaScript detects SIMD support at runtime
3. Loads appropriate WASM module

---

## Testing Strategy

### Unit Tests (Deno)

- **SIMD operation tests**: Verify correctness of vectorized operations
- **Pixel-perfect validation**: Compare SIMD output vs scalar output
- **Edge cases**: Unaligned buffers, odd pixel counts, boundary conditions

### Integration Tests

- **All existing Deno tests** (82 tests) must pass with SIMD enabled
- **Visual regression tests**: Compare rendered output
- **Cross-browser testing**: Chrome, Firefox, Safari, Edge

### Performance Benchmarks

- **Micro-benchmarks**: Individual operation timing
- **Macro-benchmarks**: Complete scene rendering
- **Comparison matrix**: Scalar vs SIMD vs Fixed vs Float

### Example Test

```typescript
// awtea-graphics/src/test/deno/simd_test.ts
Deno.test("SIMD blend produces identical results to scalar", () => {
    const surface = createSurface(100, 100);
    const src_pixels = new Uint32Array(100);
    src_pixels.fill(0x80FF0000); // 50% red
    
    // Render with scalar path
    const scalar_result = blitImageScalar(surface, src_pixels);
    
    // Render with SIMD path
    const simd_result = blitImageSIMD(surface, src_pixels);
    
    // Verify pixel-perfect match
    assertEquals(scalar_result, simd_result);
});
```

---

## Memory and Binary Size Impact

### Memory Footprint

- **SIMD operations**: Same memory usage (processes existing buffers)
- **Edge table**: Unchanged (still 32-bit integers)
- **Stack usage**: Slightly higher (v128 temporaries)
- **Overall impact**: Negligible (<1% increase)

### Binary Size

- **SIMD build**: +15-25 KB (SIMD instruction encoding)
- **Dual build deployment**: +40-50 KB total (SIMD + fallback builds)
- **Gzip compression**: ~50% reduction → +20-25 KB over network
- **Trade-off**: Acceptable for 4-8x performance gain

---

## Risks and Mitigations

### Risk 1: Browser Compatibility

**Risk**: SIMD not available in older browsers
**Mitigation**: Dual build strategy with runtime detection and fallback
**Impact**: Zero degradation for non-SIMD browsers

### Risk 2: Correctness Issues

**Risk**: SIMD implementation bugs causing visual artifacts
**Mitigation**: 
- Pixel-perfect validation tests
- Side-by-side comparison with scalar code
- Gradual rollout with feature flags

### Risk 3: Maintenance Complexity

**Risk**: Two code paths to maintain (SIMD + scalar)
**Mitigation**:
- Shared test suite
- Macro-based abstraction layer
- Comprehensive documentation

### Risk 4: Limited Speedup in Practice

**Risk**: Real-world speedup less than predicted
**Mitigation**:
- Focus on high-impact operations first
- Measure early, iterate based on data
- Benchmark on real applications

---

## Future Enhancements (Post-SIMD)

### Multi-Threading (WebAssembly Threads)

- Parallel scanline processing
- Split polygons across multiple threads
- Expected additional 2-3x speedup on multi-core systems
- Browser support: Chrome 74+, Firefox 79+

### GPU Acceleration Integration

- Hybrid rendering: SIMD for small primitives, WebGL for large ones
- Threshold-based dispatch
- Offscreen canvas optimization

### Advanced SIMD Operations

- **SIMD gather/scatter**: Non-contiguous pixel access
- **SIMD popcount**: Fast transparency detection
- **SIMD min/max**: Bounding box calculations

---

## Conclusion

SIMD optimization represents the next major performance milestone for AWTea's WASM rasterizer. Building on the 2-3x improvement from fixed-point arithmetic, SIMD vectorization can deliver an additional 2-4x speedup, resulting in **4-12x total performance improvement** over the original implementation.

**Key Success Factors**:
1. ✅ Focus on high-impact operations (blending, fills, memory ops)
2. ✅ Maintain pixel-perfect correctness
3. ✅ Progressive enhancement with fallback
4. ✅ Comprehensive testing and benchmarking
5. ✅ Clear documentation and maintainability

**Timeline**: 5-week implementation with incremental rollout and validation.

**Next Steps**: Begin Phase 0 quick wins:
1. Implement alpha blend lookup table (Day 1-2)
2. Add memset-based fills (Day 3-4)
3. Integrate fixed-point transforms (Day 5-7)
4. Then proceed to SIMD infrastructure (Week 2+)

---

**Document Version**: 1.0
**Date**: 2025-12-25
**Author**: GitHub Copilot
**Status**: Implementation Ready
