#include "awt_pixel.h"

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

void blend_pixel_composite(RenderSurface* surface,
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

    // Apply composite alpha to source alpha
    float srcAlphaF = ((float)srcA / 255.0f) * alpha;
    srcA = (uint8_t)(srcAlphaF * 255.0f);

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

    // 3) Apply Porter-Duff compositing formula
    uint8_t outR, outG, outB, outA;
    
    // Convert to normalized floats for blending
    float sR = (float)srcR / 255.0f;
    float sG = (float)srcG / 255.0f;
    float sB = (float)srcB / 255.0f;
    float sA = (float)srcA / 255.0f;
    
    float dR = (float)dstR / 255.0f;
    float dG = (float)dstG / 255.0f;
    float dB = (float)dstB / 255.0f;
    float dA = (float)dstA / 255.0f;
    
    float outRf, outGf, outBf, outAf;
    
    switch (mode) {
        case COMPOSITE_CLEAR:
            // Clear: result is transparent
            outRf = 0.0f;
            outGf = 0.0f;
            outBf = 0.0f;
            outAf = 0.0f;
            break;
            
        case COMPOSITE_SRC:
            // Src: copy source, ignore destination
            outRf = sR;
            outGf = sG;
            outBf = sB;
            outAf = sA;
            break;
            
        case COMPOSITE_DST:
            // Dst: keep destination, ignore source
            outRf = dR;
            outGf = dG;
            outBf = dB;
            outAf = dA;
            break;
            
        case COMPOSITE_SRC_OVER:
            // SrcOver: Ar = As + Ad*(1-As)
            outAf = sA + dA * (1.0f - sA);
            if (outAf > 0.0f) {
                outRf = (sR * sA + dR * dA * (1.0f - sA)) / outAf;
                outGf = (sG * sA + dG * dA * (1.0f - sA)) / outAf;
                outBf = (sB * sA + dB * dA * (1.0f - sA)) / outAf;
            } else {
                outRf = outGf = outBf = 0.0f;
            }
            break;
            
        case COMPOSITE_DST_OVER:
            // DstOver: Ar = Ad + As*(1-Ad)
            outAf = dA + sA * (1.0f - dA);
            if (outAf > 0.0f) {
                outRf = (dR * dA + sR * sA * (1.0f - dA)) / outAf;
                outGf = (dG * dA + sG * sA * (1.0f - dA)) / outAf;
                outBf = (dB * dA + sB * sA * (1.0f - dA)) / outAf;
            } else {
                outRf = outGf = outBf = 0.0f;
            }
            break;
            
        case COMPOSITE_SRC_IN:
            // SrcIn: Ar = As * Ad
            outAf = sA * dA;
            if (outAf > 0.0f) {
                outRf = sR;
                outGf = sG;
                outBf = sB;
            } else {
                outRf = outGf = outBf = 0.0f;
            }
            break;
            
        case COMPOSITE_DST_IN:
            // DstIn: Ar = Ad * As
            outAf = dA * sA;
            if (outAf > 0.0f) {
                outRf = dR;
                outGf = dG;
                outBf = dB;
            } else {
                outRf = outGf = outBf = 0.0f;
            }
            break;
            
        case COMPOSITE_SRC_OUT:
            // SrcOut: Ar = As * (1-Ad)
            outAf = sA * (1.0f - dA);
            if (outAf > 0.0f) {
                outRf = sR;
                outGf = sG;
                outBf = sB;
            } else {
                outRf = outGf = outBf = 0.0f;
            }
            break;
            
        case COMPOSITE_DST_OUT:
            // DstOut: Ar = Ad * (1-As)
            outAf = dA * (1.0f - sA);
            if (outAf > 0.0f) {
                outRf = dR;
                outGf = dG;
                outBf = dB;
            } else {
                outRf = outGf = outBf = 0.0f;
            }
            break;
            
        case COMPOSITE_SRC_ATOP:
            // SrcAtop: Ar = As*Ad + Ad*(1-As) = Ad
            outAf = dA;
            if (outAf > 0.0f) {
                outRf = (sR * sA + dR * (1.0f - sA));
                outGf = (sG * sA + dG * (1.0f - sA));
                outBf = (sB * sA + dB * (1.0f - sA));
            } else {
                outRf = outGf = outBf = 0.0f;
            }
            break;
            
        case COMPOSITE_DST_ATOP:
            // DstAtop: Ar = Ad*As + As*(1-Ad) = As
            outAf = sA;
            if (outAf > 0.0f) {
                outRf = (dR * dA + sR * (1.0f - dA));
                outGf = (dG * dA + sG * (1.0f - dA));
                outBf = (dB * dA + sB * (1.0f - dA));
            } else {
                outRf = outGf = outBf = 0.0f;
            }
            break;
            
        case COMPOSITE_XOR:
            // Xor: Ar = As*(1-Ad) + Ad*(1-As)
            outAf = sA * (1.0f - dA) + dA * (1.0f - sA);
            if (outAf > 0.0f) {
                outRf = (sR * sA * (1.0f - dA) + dR * dA * (1.0f - sA)) / outAf;
                outGf = (sG * sA * (1.0f - dA) + dG * dA * (1.0f - sA)) / outAf;
                outBf = (sB * sA * (1.0f - dA) + dB * dA * (1.0f - sA)) / outAf;
            } else {
                outRf = outGf = outBf = 0.0f;
            }
            break;
            
        default:
            // Unknown mode: fall back to SRC_OVER
            outAf = sA + dA * (1.0f - sA);
            if (outAf > 0.0f) {
                outRf = (sR * sA + dR * dA * (1.0f - sA)) / outAf;
                outGf = (sG * sA + dG * dA * (1.0f - sA)) / outAf;
                outBf = (sB * sA + dB * dA * (1.0f - sA)) / outAf;
            } else {
                outRf = outGf = outBf = 0.0f;
            }
            break;
    }
    
    // Clamp and convert back to uint8_t
    outR = (uint8_t)(outRf * 255.0f + 0.5f);
    outG = (uint8_t)(outGf * 255.0f + 0.5f);
    outB = (uint8_t)(outBf * 255.0f + 0.5f);
    
    if (dstInfo->mask_a) {
        // Destination has alpha channel
        outA = (uint8_t)(outAf * 255.0f + 0.5f);
    } else {
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
