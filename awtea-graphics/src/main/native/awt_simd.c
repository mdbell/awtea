#include "awt_simd.h"
#include "awt_log.h"
#include <string.h>

// Feature detection for SIMD support
// In WASM, if compiled with -msimd128, SIMD is always available
// JavaScript should verify browser support before calling
int has_simd_support(void) {
#ifdef __wasm_simd128__
    return 1;
#else
    return 0;
#endif
}

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
    
    // Handle remaining pixels with scalar code
    // (would use the LUT-based blending here in production)
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
        
        // Simple alpha blend (using scalar for remaining pixels)
        uint8_t sr = (s >> 16) & 0xFF;
        uint8_t sg = (s >> 8) & 0xFF;
        uint8_t sb = s & 0xFF;
        
        uint8_t da = (d >> 24) & 0xFF;
        uint8_t dr = (d >> 16) & 0xFF;
        uint8_t dg = (d >> 8) & 0xFF;
        uint8_t db = d & 0xFF;
        
        uint16_t inv_sa = 255 - sa;
        
        uint8_t out_a = sa + ((da * inv_sa + 127) / 255);
        uint8_t out_r = sr + ((dr * inv_sa + 127) / 255);
        uint8_t out_g = sg + ((dg * inv_sa + 127) / 255);
        uint8_t out_b = sb + ((db * inv_sa + 127) / 255);
        
        dst[i] = (out_a << 24) | (out_r << 16) | (out_g << 8) | out_b;
    }
}

// SIMD-optimized memory copy
void simd_memcpy_aligned(void* dst, const void* src, size_t bytes) {
    uint8_t* d = (uint8_t*)dst;
    const uint8_t* s = (const uint8_t*)src;
    size_t i = 0;
    
    // Copy 16 bytes (128 bits) at a time
    for (; i + 16 <= bytes; i += 16) {
        v128_t data = wasm_v128_load(&s[i]);
        wasm_v128_store(&d[i], data);
    }
    
    // Handle remaining bytes with scalar copy
    for (; i < bytes; i++) {
        d[i] = s[i];
    }
}

// SIMD-optimized ARGB to RGBA conversion
void simd_convert_argb_to_rgba(uint32_t* dst, const uint32_t* src, int count) {
    int i = 0;
    
    // SIMD shuffle mask to convert ARGB to RGBA
    // ARGB bytes: [A0 R0 G0 B0  A1 R1 G1 B1  A2 R2 G2 B2  A3 R3 G3 B3]
    // RGBA bytes: [R0 G0 B0 A0  R1 G1 B1 A1  R2 G2 B2 A2  R3 G3 B3 A3]
    // Shuffle indices: swap byte 0 with byte 3, byte 1 with byte 2, etc.
    
    // Process 4 pixels at a time
    for (; i <= count - 4; i += 4) {
        v128_t argb = wasm_v128_load(&src[i]);
        
        // Shuffle: move A from position 0 to position 3 for each pixel
        // Byte indices for 4 pixels (16 bytes total):
        // Pixel 0: A=0, R=1, G=2, B=3 -> want R=1, G=2, B=3, A=0
        // Pixel 1: A=4, R=5, G=6, B=7 -> want R=5, G=6, B=7, A=4
        // etc.
        v128_t rgba = wasm_i8x16_shuffle(argb, argb,
            1, 2, 3, 0,    // Pixel 0: R, G, B, A
            5, 6, 7, 4,    // Pixel 1: R, G, B, A
            9, 10, 11, 8,  // Pixel 2: R, G, B, A
            13, 14, 15, 12 // Pixel 3: R, G, B, A
        );
        
        wasm_v128_store(&dst[i], rgba);
    }
    
    // Handle remaining pixels with scalar code
    for (; i < count; i++) {
        uint32_t argb = src[i];
        uint8_t a = (argb >> 24) & 0xFF;
        uint8_t r = (argb >> 16) & 0xFF;
        uint8_t g = (argb >> 8) & 0xFF;
        uint8_t b = argb & 0xFF;
        dst[i] = (r << 24) | (g << 16) | (b << 8) | a;
    }
}
