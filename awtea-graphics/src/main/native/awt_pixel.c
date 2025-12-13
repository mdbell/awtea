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

void set_pixel_generic(Surface* surface, int x, int y, PixelFormat srcFormat,
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

void set_pixel_same_format(Surface* surface, int x, int y, PixelFormat srcFormat,
    uint32_t pixel) {
    uint32_t* framebuffer = (uint32_t*)(uintptr_t)surface->ptr;
    uint32_t stride = surface->stride / 4; // in pixels

    framebuffer[y * stride + x] = pixel;
}

void set_pixel_no_alpha_src(Surface* surface, int x, int y, PixelFormat srcFormat,
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

void blend_pixel(Surface* surface,
                        int x, int y,
                        PixelFormat srcFormat,
                        uint32_t srcPixel) {
    uint32_t* framebuffer = (uint32_t*)(uintptr_t)surface->ptr;
    uint32_t stride = surface->stride / 4;

    const PixelFormatInfo* srcInfo = &g_pixel_format_info[srcFormat];
    const PixelFormatInfo* dstInfo = &g_pixel_format_info[surface->format];

    // 1) Decode src
    uint8_t srcR, srcG, srcB, srcA;
    unpack_pixel(srcInfo, srcPixel, &srcR, &srcG, &srcB, &srcA);

    // Fast path: fully transparent
    if (srcA == 0) {
        return;
    }

    // 2) Load and decode dst
    uint32_t dstPixel = framebuffer[y * stride + x];
    uint8_t dstR, dstG, dstB, dstA;
    unpack_pixel(dstInfo, dstPixel, &dstR, &dstG, &dstB, &dstA);

    // 3) Blend (integer math, 0..255)
    // out = src + dst * (1 - a)
    uint32_t a  = srcA;
    uint32_t ia = 255 - a;

    uint8_t outR = (uint8_t)((srcR * a + dstR * ia + 127) / 255);
    uint8_t outG = (uint8_t)((srcG * a + dstG * ia + 127) / 255);
    uint8_t outB = (uint8_t)((srcB * a + dstB * ia + 127) / 255);

    uint8_t outA;
    if (dstInfo->mask_a) {
        // source-over alpha
        outA = (uint8_t)((srcA * 255 + dstA * ia + 127) / 255);
    } else {
        // no alpha in dst format -> keep opaque
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
