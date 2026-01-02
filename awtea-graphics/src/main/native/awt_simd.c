#include "awt_simd.h"
#include "awt_log.h"
#include "awt_alpha_lut.h"
#include "awt_raster_internal.h"
#include <string.h>

// SIMD-optimized scanline fill
void simd_fill_scanline(uint32_t* framebuffer, int y, int stride,
                       int x_start, int x_end, uint32_t color) {
    if (x_end <= x_start) {
        return;
    }
    
    uint32_t* row = &framebuffer[y * stride];
    int count = x_end - x_start;
    int i = x_start;
    
    // Broadcast color to all 4 SIMD lanes
    v128_t color_vec = wasm_i32x4_splat(color);
    
    // Handle unaligned start (write up to 3 pixels)
    while (i < x_end && ((uintptr_t)&row[i] & 15) != 0) {
        row[i++] = color;
    }
    
    // SIMD fill: 4 pixels per iteration (16-byte aligned)
    int simd_end = x_start + ((x_end - i) & ~3); // Round down to multiple of 4
    for (; i < simd_end; i += 4) {
        wasm_v128_store(&row[i], color_vec);
    }
    
    // Handle remaining pixels (0-3)
    for (; i < x_end; i++) {
        row[i] = color;
    }
}

// SIMD-optimized SRC_OVER alpha blending
// Formula: dst = src + dst * (255 - src.a) / 255
void simd_blend_src_over_argb(uint32_t* dst, const uint32_t* src, int count) {
    int i = 0;
    
    // Constants for alpha blending
    v128_t const_255 = wasm_i16x8_splat(255);
    v128_t const_1 = wasm_i16x8_splat(1);
    
    // Process 4 pixels at a time
    for (; i <= count - 4; i += 4) {
        // Load 4 source and 4 destination pixels
        v128_t src_pixels = wasm_v128_load(&src[i]);
        v128_t dst_pixels = wasm_v128_load(&dst[i]);
        
        // Extract alpha channel from source (shift right by 24 bits)
        // src.a = (src >> 24) & 0xFF
        v128_t src_alpha_32 = wasm_u32x4_shr(src_pixels, 24);
        
        // Calculate inverse alpha: 255 - src.a
        v128_t inv_alpha_32 = wasm_i32x4_sub(wasm_i32x4_splat(255), src_alpha_32);
        
        // Unpack pixels to 16-bit for multiplication (to avoid overflow)
        // Low 4 bytes (2 pixels) -> 8 x 16-bit values (ARGB ARGB)
        v128_t src_lo = wasm_u16x8_extend_low_u8x16(src_pixels);
        v128_t src_hi = wasm_u16x8_extend_high_u8x16(src_pixels);
        v128_t dst_lo = wasm_u16x8_extend_low_u8x16(dst_pixels);
        v128_t dst_hi = wasm_u16x8_extend_high_u8x16(dst_pixels);
        
        // Broadcast inverse alpha to 16-bit lanes for multiplication
        // For each pixel, we need the same inv_alpha value for all 4 channels (ARGB)
        // inv_alpha_32 has: [inv_a0, inv_a1, inv_a2, inv_a3]
        // We need: [inv_a0, inv_a0, inv_a0, inv_a0, inv_a1, inv_a1, inv_a1, inv_a1]
        
        // For simplicity and best performance, we'll use a different approach:
        // Multiply dst by inv_alpha, then add src
        // dst' = dst * inv_alpha / 255 + src
        
        // Extract inv_alpha for each pixel and replicate across channels
        uint32_t inv_a0 = wasm_i32x4_extract_lane(inv_alpha_32, 0);
        uint32_t inv_a1 = wasm_i32x4_extract_lane(inv_alpha_32, 1);
        uint32_t inv_a2 = wasm_i32x4_extract_lane(inv_alpha_32, 2);
        uint32_t inv_a3 = wasm_i32x4_extract_lane(inv_alpha_32, 3);
        
        // Create 16-bit vectors with replicated alpha values
        // Low half has pixels 0 and 1, each with 4 channels (A,R,G,B) x 2 pixels = 8 values
        v128_t inv_alpha_lo = wasm_i16x8_make(
            inv_a0, inv_a0, inv_a0, inv_a0,  // Pixel 0: A, R, G, B
            inv_a1, inv_a1, inv_a1, inv_a1   // Pixel 1: A, R, G, B
        );
        v128_t inv_alpha_hi = wasm_i16x8_make(
            inv_a2, inv_a2, inv_a2, inv_a2,  // Pixel 2: A, R, G, B
            inv_a3, inv_a3, inv_a3, inv_a3   // Pixel 3: A, R, G, B
        );
        
        // Multiply: dst * inv_alpha
        dst_lo = wasm_i16x8_mul(dst_lo, inv_alpha_lo);
        dst_hi = wasm_i16x8_mul(dst_hi, inv_alpha_hi);
        
        // Divide by 255: (value * inv_alpha + 128) / 255
        // Approximate with: (value + 1) >> 8
        dst_lo = wasm_i16x8_add(dst_lo, const_1);
        dst_hi = wasm_i16x8_add(dst_hi, const_1);
        dst_lo = wasm_u16x8_shr(dst_lo, 8);
        dst_hi = wasm_u16x8_shr(dst_hi, 8);
        
        // Add source pixels: result = src + (dst * inv_alpha / 255)
        v128_t result_lo = wasm_i16x8_add(src_lo, dst_lo);
        v128_t result_hi = wasm_i16x8_add(src_hi, dst_hi);
        
        // Clamp to 8-bit and pack back to 32-bit pixels
        // narrow_i16x8_u8x16 saturates to 0-255
        v128_t result = wasm_u8x16_narrow_i16x8(result_lo, result_hi);
        
        // Store result
        wasm_v128_store(&dst[i], result);
    }
    
    // Handle remaining pixels with scalar code using alpha LUT
    for (; i < count; i++) {
        uint32_t s = src[i];
        uint32_t d = dst[i];
        
        uint8_t sa = (s >> 24) & 0xFF;
        if (sa == 0) continue; // Transparent source
        if (sa == 255) {
            // Opaque source - just copy
            dst[i] = s;
            continue;
        }
        
        // SRC_OVER alpha blend using LUT
        uint8_t sr = (s >> 16) & 0xFF;
        uint8_t sg = (s >> 8) & 0xFF;
        uint8_t sb = s & 0xFF;
        
        uint8_t da = (d >> 24) & 0xFF;
        uint8_t dr = (d >> 16) & 0xFF;
        uint8_t dg = (d >> 8) & 0xFF;
        uint8_t db = d & 0xFF;
        
        uint8_t inv_sa = 255 - sa;
        
        // Use alpha LUT instead of division
        uint8_t out_a = sa + alpha_blend_component(inv_sa, da);
        uint8_t out_r = sr + alpha_blend_component(inv_sa, dr);
        uint8_t out_g = sg + alpha_blend_component(inv_sa, dg);
        uint8_t out_b = sb + alpha_blend_component(inv_sa, db);
        
        dst[i] = (out_a << 24) | (out_r << 16) | (out_g << 8) | out_b;
    }
}

// SIMD-optimized pixel format conversion between any two formats
// Uses shuffle indices from PixelFormatInfo for generic conversion
void simd_convert_pixels(uint32_t* dst, const uint32_t* src, int count,
                        PixelFormat src_format, PixelFormat dst_format) {
    // If formats are the same, just copy
    if (src_format == dst_format) {
        memcpy(dst, src, count * sizeof(uint32_t));
        return;
    }
    
    // Get format info
    extern const PixelFormatInfo g_pixel_format_info[PIXEL_FORMAT_COUNT];
    const PixelFormatInfo* src_info = &g_pixel_format_info[src_format];
    const PixelFormatInfo* dst_info = &g_pixel_format_info[dst_format];
    
    int i = 0;
    
    // Generic SIMD conversion using shuffle indices from PixelFormatInfo
    // Strategy: src -> ARGB -> dst using pre-computed shuffle patterns
    
    // Load shuffle patterns into SIMD registers
    v128_t to_argb_shuffle = wasm_v128_load(src_info->simd_shuffle_to_argb);
    v128_t from_argb_shuffle = wasm_v128_load(dst_info->simd_shuffle_from_argb);
    
    // Process 4 pixels at a time with SIMD
    for (; i <= count - 4; i += 4) {
        // Load 4 source pixels
        v128_t pixels = wasm_v128_load(&src[i]);
        
        // Convert to ARGB intermediate format
        v128_t argb = wasm_i8x16_swizzle(pixels, to_argb_shuffle);
        
        // Convert from ARGB to destination format
        v128_t result = wasm_i8x16_swizzle(argb, from_argb_shuffle);
        
        // Store result
        wasm_v128_store(&dst[i], result);
    }
    
    // Handle remaining pixels with scalar code
    for (; i < count; i++) {
        uint32_t pixel = src[i];
        
        // Extract components from source format
        uint8_t r = (pixel & src_info->mask_r) >> src_info->shift_r;
        uint8_t g = (pixel & src_info->mask_g) >> src_info->shift_g;
        uint8_t b = (pixel & src_info->mask_b) >> src_info->shift_b;
        uint8_t a = src_info->mask_a ? ((pixel & src_info->mask_a) >> src_info->shift_a) : 0xFF;
        
        // Pack into destination format
        dst[i] = ((uint32_t)r << dst_info->shift_r) |
                 ((uint32_t)g << dst_info->shift_g) |
                 ((uint32_t)b << dst_info->shift_b) |
                 ((uint32_t)a << dst_info->shift_a);
    }
}
