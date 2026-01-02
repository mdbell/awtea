#include "awt_pixel.h"
#include "awt_alpha_lut.h"

const PixelFormatInfo g_pixel_format_info[PIXEL_FORMAT_COUNT] = {
    // PIXEL_FORMAT_ARGB: 0xAARRGGBB
    {
        .mask_r  = 0x00FF0000,
        .mask_g  = 0x0000FF00,
        .mask_b  = 0x000000FF,
        .mask_a  = 0xFF000000,
        .shift_r = 16,
        .shift_g = 8,
        .shift_b = 0,
        .shift_a = 24,
        .alphaVariant = PIXEL_FORMAT_ARGB, // self
#ifdef __wasm_simd128__
        // Identity shuffle (no conversion needed)
        .simd_shuffle_to_argb = {0,1,2,3, 4,5,6,7, 8,9,10,11, 12,13,14,15},
        .simd_shuffle_from_argb = {0,1,2,3, 4,5,6,7, 8,9,10,11, 12,13,14,15},
#endif
    },
    // PIXEL_FORMAT_RGB: 0x00RRGGBB (no alpha)
    {
        .mask_r  = 0x00FF0000,
        .mask_g  = 0x0000FF00,
        .mask_b  = 0x000000FF,
        .mask_a  = 0x00000000, // no alpha
        .shift_r = 16,
        .shift_g = 8,
        .shift_b = 0,
        .shift_a = 0,
        .alphaVariant = PIXEL_FORMAT_ARGB, // use ARGB for alpha operations
#ifdef __wasm_simd128__
        // RGB is same byte order as ARGB but alpha set to 0xFF
        // Byte layout: [A R G B] -> set A to 0xFF
        .simd_shuffle_to_argb = {0,1,2,3, 4,5,6,7, 8,9,10,11, 12,13,14,15}, // load as-is, will set alpha separately
        .simd_shuffle_from_argb = {0,1,2,3, 4,5,6,7, 8,9,10,11, 12,13,14,15}, // copy as-is (alpha ignored)
#endif
    },
    // PIXEL_FORMAT_RGBA: 0xRRGGBBAA
    {
        .mask_r  = 0xFF000000,
        .mask_g  = 0x00FF0000,
        .mask_b  = 0x0000FF00,
        .mask_a  = 0x000000FF,
        .shift_r = 24,
        .shift_g = 16,
        .shift_b = 8,
        .shift_a = 0,
        .alphaVariant = PIXEL_FORMAT_RGBA, // self
#ifdef __wasm_simd128__
        // RGBA to ARGB: rotate each pixel right by 1 byte
        // Pixel bytes: [R G B A] -> [A R G B]
        .simd_shuffle_to_argb = {3,0,1,2, 7,4,5,6, 11,8,9,10, 15,12,13,14},
        // ARGB to RGBA: rotate each pixel left by 1 byte
        // Pixel bytes: [A R G B] -> [R G B A]
        .simd_shuffle_from_argb = {1,2,3,0, 5,6,7,4, 9,10,11,8, 13,14,15,12},
#endif
    },

    // PIXEL_FORMAT_ABGR: 0xAABBGGRR
    {
        .mask_r  = 0x000000FF,
        .mask_g  = 0x0000FF00,
        .mask_b  = 0x00FF0000,
        .mask_a  = 0xFF000000,
        .shift_r = 0,
        .shift_g = 8,
        .shift_b = 16,
        .shift_a = 24,
        .alphaVariant = PIXEL_FORMAT_ABGR, // self
#ifdef __wasm_simd128__
        // ABGR to ARGB: keep A, reverse RGB
        // Pixel bytes: [A B G R] -> [A R G B]
        .simd_shuffle_to_argb = {0,3,2,1, 4,7,6,5, 8,11,10,9, 12,15,14,13},
        // ARGB to ABGR: keep A, reverse RGB
        // Pixel bytes: [A R G B] -> [A B G R]
        .simd_shuffle_from_argb = {0,3,2,1, 4,7,6,5, 8,11,10,9, 12,15,14,13},
#endif
    },
    // PIXEL_FORMAT_BGR: 0x00BBGGRR (no alpha)
    {
        .mask_r  = 0x000000FF,
        .mask_g  = 0x0000FF00,
        .mask_b  = 0x00FF0000,
        .mask_a  = 0x00000000, // no alpha
        .shift_r = 0,
        .shift_g = 8,
        .shift_b = 16,
        .shift_a = 0,
        .alphaVariant = PIXEL_FORMAT_ABGR, // use ABGR for alpha operations
#ifdef __wasm_simd128__
        // BGR to ARGB: reverse RGB, set A to 0xFF
        // Pixel bytes: [0 B G R] -> [A R G B]
        .simd_shuffle_to_argb = {0,3,2,1, 4,7,6,5, 8,11,10,9, 12,15,14,13}, // will set alpha separately
        // ARGB to BGR: reverse RGB
        // Pixel bytes: [A R G B] -> [0 B G R]
        .simd_shuffle_from_argb = {0,3,2,1, 4,7,6,5, 8,11,10,9, 12,15,14,13},
#endif
    }
};

void set_pixel_generic(SurfaceData* surface, int x, int y, PixelFormat srcFormat,
    uint32_t srcPixel) {
    uint32_t* framebuffer = (uint32_t*)(uintptr_t)surface->ptr;
    uint32_t stride = surface->stride / 4; // in pixels

    const PixelFormatInfo* srcInfo = &g_pixel_format_info[srcFormat];
    const PixelFormatInfo* dstInfo = &g_pixel_format_info[surface->format];

    uint8_t srcR, srcG, srcB, srcA;
    unpack_pixel(srcInfo, srcPixel, &srcR, &srcG, &srcB, &srcA);

    uint32_t dstPixel = pack_pixel(dstInfo, srcR, srcG, srcB, srcA);
    framebuffer[y * stride + x] = dstPixel;

}

void set_pixel_same_format(SurfaceData* surface, int x, int y, PixelFormat srcFormat,
    uint32_t pixel) {
    uint32_t* framebuffer = (uint32_t*)(uintptr_t)surface->ptr;
    uint32_t stride = surface->stride / 4; // in pixels

    framebuffer[y * stride + x] = pixel;
}

void set_pixel_no_alpha_src(SurfaceData* surface, int x, int y, PixelFormat srcFormat,
    uint32_t pixel) {

    uint32_t* framebuffer = (uint32_t*)(uintptr_t)surface->ptr;
    uint32_t stride = surface->stride / 4; // in pixels

    const PixelFormatInfo* srcInfo = &g_pixel_format_info[srcFormat];
    const PixelFormatInfo* dstInfo = &g_pixel_format_info[surface->format];

    uint8_t srcR, srcG, srcB, srcA;
    unpack_pixel(srcInfo, pixel, &srcR, &srcG, &srcB, &srcA);

    uint32_t dstPixel = pack_pixel(dstInfo, srcR, srcG, srcB, 0xFF);
    framebuffer[y * stride + x] = dstPixel;
}

void unpack_pixel(const PixelFormatInfo* info,
                                uint32_t pixel,
                                uint8_t* r,
                                uint8_t* g,
                                uint8_t* b,
                                uint8_t* a) {
    uint32_t rr = (pixel & info->mask_r) >> info->shift_r;
    uint32_t gg = (pixel & info->mask_g) >> info->shift_g;
    uint32_t bb = (pixel & info->mask_b) >> info->shift_b;
    uint32_t aa = info->mask_a ? ((pixel & info->mask_a) >> info->shift_a) : 0xFF;

    *r = (uint8_t)rr;
    *g = (uint8_t)gg;
    *b = (uint8_t)bb;
    *a = (uint8_t)aa;
}

uint32_t pack_pixel(const PixelFormatInfo* info,
                                  uint8_t r,
                                  uint8_t g,
                                  uint8_t b,
                                  uint8_t a) {
    uint32_t pixel = 0;
    if (info->mask_r) pixel |= ((uint32_t)r << info->shift_r) & info->mask_r;
    if (info->mask_g) pixel |= ((uint32_t)g << info->shift_g) & info->mask_g;
    if (info->mask_b) pixel |= ((uint32_t)b << info->shift_b) & info->mask_b;
    if (info->mask_a) pixel |= ((uint32_t)a << info->shift_a) & info->mask_a;
    return pixel;
}

void blend_pixel(SurfaceData* surface,
                        int x, int y,
                        PixelFormat srcFormat,
                        uint32_t srcPixel) {
    // Default to SRC_OVER with full alpha for backward compatibility
    blend_pixel_composite(surface, x, y, srcFormat, srcPixel, 
                         COMPOSITE_SRC_OVER, 1.0f);
}

void blend_pixel_composite(SurfaceData* surface,
                           int x, int y,
                           PixelFormat srcFormat,
                           uint32_t srcPixel,
                           CompositeMode mode,
                           float alpha) {
    uint32_t* framebuffer = (uint32_t*)(uintptr_t)surface->ptr;
    uint32_t stride = surface->stride / 4;

    const PixelFormatInfo* srcInfo = &g_pixel_format_info[srcFormat];
    const PixelFormatInfo* dstInfo = &g_pixel_format_info[surface->format];

    // 1) Decode src
    uint8_t srcR, srcG, srcB, srcA;
    unpack_pixel(srcInfo, srcPixel, &srcR, &srcG, &srcB, &srcA);

    // Apply composite alpha to source alpha using LUT
    if (alpha < 1.0f) {
        uint8_t alphaInt = (uint8_t)(alpha * 255.0f + 0.5f);
        srcA = alpha_blend_component(alphaInt, srcA);
    }
    // else srcA unchanged (alpha == 1.0f)

    // Fast path: fully transparent source (no effect except for CLEAR mode)
    if (srcA == 0 && mode != COMPOSITE_CLEAR) {
        if (mode == COMPOSITE_DST) {
            // DST mode: leave destination unchanged regardless of source
            return;
        }
        // For other modes, transparent source has no effect
        return;
    }

    // 2) Load and decode dst
    uint32_t dstPixel = framebuffer[y * stride + x];
    uint8_t dstR, dstG, dstB, dstA;
    unpack_pixel(dstInfo, dstPixel, &dstR, &dstG, &dstB, &dstA);

    // 3) Apply Porter-Duff compositing using alpha blend LUT
    uint8_t outR, outG, outB, outA;
    
    switch (mode) {
        case COMPOSITE_CLEAR:
            // Clear: result is transparent
            outR = outG = outB = outA = 0;
            break;
            
        case COMPOSITE_SRC:
            // Src: copy source, ignore destination
            outR = srcR;
            outG = srcG;
            outB = srcB;
            outA = srcA;
            break;
            
        case COMPOSITE_DST:
            // Dst: keep destination, ignore source
            outR = dstR;
            outG = dstG;
            outB = dstB;
            outA = dstA;
            break;
            
        case COMPOSITE_SRC_OVER: {
            // SrcOver: outA = srcA + dstA * (255 - srcA) / 255
            uint8_t inv_srcA = 255 - srcA;
            uint8_t dstA_scaled = alpha_blend_component(inv_srcA, dstA);
            outA = srcA + dstA_scaled;
            
            if (outA > 0) {
                // Premultiply: srcR * srcA / 255, dstR * dstA / 255
                uint8_t srcR_pm = alpha_blend_component(srcA, srcR);
                uint8_t srcG_pm = alpha_blend_component(srcA, srcG);
                uint8_t srcB_pm = alpha_blend_component(srcA, srcB);
                
                uint8_t dstR_pm = alpha_blend_component(dstA, dstR);
                uint8_t dstG_pm = alpha_blend_component(dstA, dstG);
                uint8_t dstB_pm = alpha_blend_component(dstA, dstB);
                
                // Scale destination by (255 - srcA) / 255
                uint8_t dstR_scaled = alpha_blend_component(inv_srcA, dstR_pm);
                uint8_t dstG_scaled = alpha_blend_component(inv_srcA, dstG_pm);
                uint8_t dstB_scaled = alpha_blend_component(inv_srcA, dstB_pm);
                
                // Add source and destination (clamped to 255)
                uint16_t sumR = (uint16_t)srcR_pm + (uint16_t)dstR_scaled;
                uint16_t sumG = (uint16_t)srcG_pm + (uint16_t)dstG_scaled;
                uint16_t sumB = (uint16_t)srcB_pm + (uint16_t)dstB_scaled;
                
                // Un-premultiply by dividing by outA (approximate with LUT)
                // For exact results: component * 255 / outA
                // We use: component * (255 / outA) where 255/outA is computed once
                if (outA == 255) {
                    outR = (uint8_t)(sumR > 255 ? 255 : sumR);
                    outG = (uint8_t)(sumG > 255 ? 255 : sumG);
                    outB = (uint8_t)(sumB > 255 ? 255 : sumB);
                } else {
                    // Approximate un-premultiply: result / outA * 255
                    outR = (uint8_t)((sumR * 255 + outA/2) / outA);
                    outG = (uint8_t)((sumG * 255 + outA/2) / outA);
                    outB = (uint8_t)((sumB * 255 + outA/2) / outA);
                }
            } else {
                outR = outG = outB = 0;
            }
            break;
        }
            
        case COMPOSITE_DST_OVER: {
            // DstOver: outA = dstA + srcA * (255 - dstA) / 255
            uint8_t inv_dstA = 255 - dstA;
            uint8_t srcA_scaled = alpha_blend_component(inv_dstA, srcA);
            outA = dstA + srcA_scaled;
            
            if (outA > 0) {
                uint8_t dstR_pm = alpha_blend_component(dstA, dstR);
                uint8_t dstG_pm = alpha_blend_component(dstA, dstG);
                uint8_t dstB_pm = alpha_blend_component(dstA, dstB);
                
                uint8_t srcR_pm = alpha_blend_component(srcA, srcR);
                uint8_t srcG_pm = alpha_blend_component(srcA, srcG);
                uint8_t srcB_pm = alpha_blend_component(srcA, srcB);
                
                uint8_t srcR_scaled = alpha_blend_component(inv_dstA, srcR_pm);
                uint8_t srcG_scaled = alpha_blend_component(inv_dstA, srcG_pm);
                uint8_t srcB_scaled = alpha_blend_component(inv_dstA, srcB_pm);
                
                uint16_t sumR = (uint16_t)dstR_pm + (uint16_t)srcR_scaled;
                uint16_t sumG = (uint16_t)dstG_pm + (uint16_t)srcG_scaled;
                uint16_t sumB = (uint16_t)dstB_pm + (uint16_t)srcB_scaled;
                
                if (outA == 255) {
                    outR = (uint8_t)(sumR > 255 ? 255 : sumR);
                    outG = (uint8_t)(sumG > 255 ? 255 : sumG);
                    outB = (uint8_t)(sumB > 255 ? 255 : sumB);
                } else {
                    outR = (uint8_t)((sumR * 255 + outA/2) / outA);
                    outG = (uint8_t)((sumG * 255 + outA/2) / outA);
                    outB = (uint8_t)((sumB * 255 + outA/2) / outA);
                }
            } else {
                outR = outG = outB = 0;
            }
            break;
        }
            
        case COMPOSITE_SRC_IN:
            // SrcIn: outA = srcA * dstA / 255
            outA = alpha_blend_component(srcA, dstA);
            if (outA > 0) {
                outR = srcR;
                outG = srcG;
                outB = srcB;
            } else {
                outR = outG = outB = 0;
            }
            break;
            
        case COMPOSITE_DST_IN:
            // DstIn: outA = dstA * srcA / 255
            outA = alpha_blend_component(dstA, srcA);
            if (outA > 0) {
                outR = dstR;
                outG = dstG;
                outB = dstB;
            } else {
                outR = outG = outB = 0;
            }
            break;
            
        case COMPOSITE_SRC_OUT:
            // SrcOut: outA = srcA * (255 - dstA) / 255
            outA = alpha_blend_component(srcA, 255 - dstA);
            if (outA > 0) {
                outR = srcR;
                outG = srcG;
                outB = srcB;
            } else {
                outR = outG = outB = 0;
            }
            break;
            
        case COMPOSITE_DST_OUT:
            // DstOut: outA = dstA * (255 - srcA) / 255
            outA = alpha_blend_component(dstA, 255 - srcA);
            if (outA > 0) {
                outR = dstR;
                outG = dstG;
                outB = dstB;
            } else {
                outR = outG = outB = 0;
            }
            break;
            
        case COMPOSITE_SRC_ATOP: {
            // SrcAtop: outA = dstA (result has destination alpha)
            outA = dstA;
            if (outA > 0) {
                // outRGB = srcRGB * srcA + dstRGB * (255 - srcA)
                uint8_t inv_srcA = 255 - srcA;
                uint8_t srcR_scaled = alpha_blend_component(srcA, srcR);
                uint8_t srcG_scaled = alpha_blend_component(srcA, srcG);
                uint8_t srcB_scaled = alpha_blend_component(srcA, srcB);
                uint8_t dstR_scaled = alpha_blend_component(inv_srcA, dstR);
                uint8_t dstG_scaled = alpha_blend_component(inv_srcA, dstG);
                uint8_t dstB_scaled = alpha_blend_component(inv_srcA, dstB);
                
                outR = srcR_scaled + dstR_scaled;
                outG = srcG_scaled + dstG_scaled;
                outB = srcB_scaled + dstB_scaled;
            } else {
                outR = outG = outB = 0;
            }
            break;
        }
            
        case COMPOSITE_DST_ATOP: {
            // DstAtop: outA = srcA (result has source alpha)
            outA = srcA;
            if (outA > 0) {
                // outRGB = dstRGB * dstA + srcRGB * (255 - dstA)
                uint8_t inv_dstA = 255 - dstA;
                uint8_t dstR_scaled = alpha_blend_component(dstA, dstR);
                uint8_t dstG_scaled = alpha_blend_component(dstA, dstG);
                uint8_t dstB_scaled = alpha_blend_component(dstA, dstB);
                uint8_t srcR_scaled = alpha_blend_component(inv_dstA, srcR);
                uint8_t srcG_scaled = alpha_blend_component(inv_dstA, srcG);
                uint8_t srcB_scaled = alpha_blend_component(inv_dstA, srcB);
                
                outR = dstR_scaled + srcR_scaled;
                outG = dstG_scaled + srcG_scaled;
                outB = dstB_scaled + srcB_scaled;
            } else {
                outR = outG = outB = 0;
            }
            break;
        }
            
        case COMPOSITE_XOR: {
            // Xor: outA = srcA * (255 - dstA) + dstA * (255 - srcA)
            uint8_t inv_srcA = 255 - srcA;
            uint8_t inv_dstA = 255 - dstA;
            uint8_t srcA_part = alpha_blend_component(srcA, inv_dstA);
            uint8_t dstA_part = alpha_blend_component(dstA, inv_srcA);
            outA = srcA_part + dstA_part;
            
            if (outA > 0) {
                uint8_t srcR_pm = alpha_blend_component(srcA, srcR);
                uint8_t srcG_pm = alpha_blend_component(srcA, srcG);
                uint8_t srcB_pm = alpha_blend_component(srcA, srcB);
                uint8_t srcR_scaled = alpha_blend_component(inv_dstA, srcR_pm);
                uint8_t srcG_scaled = alpha_blend_component(inv_dstA, srcG_pm);
                uint8_t srcB_scaled = alpha_blend_component(inv_dstA, srcB_pm);
                
                uint8_t dstR_pm = alpha_blend_component(dstA, dstR);
                uint8_t dstG_pm = alpha_blend_component(dstA, dstG);
                uint8_t dstB_pm = alpha_blend_component(dstA, dstB);
                uint8_t dstR_scaled = alpha_blend_component(inv_srcA, dstR_pm);
                uint8_t dstG_scaled = alpha_blend_component(inv_srcA, dstG_pm);
                uint8_t dstB_scaled = alpha_blend_component(inv_srcA, dstB_pm);
                
                uint16_t sumR = (uint16_t)srcR_scaled + (uint16_t)dstR_scaled;
                uint16_t sumG = (uint16_t)srcG_scaled + (uint16_t)dstG_scaled;
                uint16_t sumB = (uint16_t)srcB_scaled + (uint16_t)dstB_scaled;
                
                outR = (uint8_t)((sumR * 255 + outA/2) / outA);
                outG = (uint8_t)((sumG * 255 + outA/2) / outA);
                outB = (uint8_t)((sumB * 255 + outA/2) / outA);
            } else {
                outR = outG = outB = 0;
            }
            break;
        }
            
        default: {
            // Unknown mode: fall back to SRC_OVER
            uint8_t inv_srcA = 255 - srcA;
            uint8_t dstA_scaled = alpha_blend_component(inv_srcA, dstA);
            outA = srcA + dstA_scaled;
            
            if (outA > 0) {
                uint8_t srcR_pm = alpha_blend_component(srcA, srcR);
                uint8_t srcG_pm = alpha_blend_component(srcA, srcG);
                uint8_t srcB_pm = alpha_blend_component(srcA, srcB);
                
                uint8_t dstR_pm = alpha_blend_component(dstA, dstR);
                uint8_t dstG_pm = alpha_blend_component(dstA, dstG);
                uint8_t dstB_pm = alpha_blend_component(dstA, dstB);
                
                uint8_t dstR_scaled = alpha_blend_component(inv_srcA, dstR_pm);
                uint8_t dstG_scaled = alpha_blend_component(inv_srcA, dstG_pm);
                uint8_t dstB_scaled = alpha_blend_component(inv_srcA, dstB_pm);
                
                uint16_t sumR = (uint16_t)srcR_pm + (uint16_t)dstR_scaled;
                uint16_t sumG = (uint16_t)srcG_pm + (uint16_t)dstG_scaled;
                uint16_t sumB = (uint16_t)srcB_pm + (uint16_t)dstB_scaled;
                
                if (outA == 255) {
                    outR = (uint8_t)(sumR > 255 ? 255 : sumR);
                    outG = (uint8_t)(sumG > 255 ? 255 : sumG);
                    outB = (uint8_t)(sumB > 255 ? 255 : sumB);
                } else {
                    outR = (uint8_t)((sumR * 255 + outA/2) / outA);
                    outG = (uint8_t)((sumG * 255 + outA/2) / outA);
                    outB = (uint8_t)((sumB * 255 + outA/2) / outA);
                }
            } else {
                outR = outG = outB = 0;
            }
            break;
        }
    }
    
    // Handle destination format alpha
    if (!dstInfo->mask_a) {
        // No alpha in dst format -> keep opaque
        outA = 255;
    }

    uint32_t outPixel = pack_pixel(dstInfo, outR, outG, outB, outA);
    framebuffer[y * stride + x] = outPixel;
}

SetPixelFunc get_set_pixel_func(PixelFormat srcFormat, PixelFormat dstFormat) {
    if( srcFormat < 0 || srcFormat >= PIXEL_FORMAT_COUNT ||
        dstFormat < 0 || dstFormat >= PIXEL_FORMAT_COUNT) {
        return set_pixel_generic;
    }
    if(srcFormat == dstFormat) {
        return set_pixel_same_format;
    }
    PixelFormatInfo srcInfo = g_pixel_format_info[srcFormat];
    if(srcInfo.mask_a == 0) {
        return set_pixel_no_alpha_src;
    }
    return set_pixel_generic;
}
