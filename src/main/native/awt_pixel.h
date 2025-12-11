#pragma once
#include "awt_raster_internal.h"

extern const PixelFormatInfo g_pixel_format_info[PIXEL_FORMAT_COUNT];

typedef void (*SetPixelFunc)(Surface*, int, int, PixelFormat, uint32_t);

SetPixelFunc get_set_pixel_func(PixelFormat srcFormat, PixelFormat dstFormat);

void set_pixel_generic(Surface* surface, int x, int y, PixelFormat srcFormat, uint32_t pixel);

void set_pixel_same_format(Surface* surface, int x, int y, PixelFormat srcFormat, uint32_t pixel);

void set_pixel_no_alpha_src(Surface* surface, int x, int y, PixelFormat srcFormat, uint32_t pixel);

uint32_t pack_pixel(const PixelFormatInfo* info,
                    uint8_t r,
                    uint8_t g,
                    uint8_t b,
                    uint8_t a);

void unpack_pixel(const PixelFormatInfo* info,
                    uint32_t pixel,
                    uint8_t* r,
                    uint8_t* g,
                    uint8_t* b,
                    uint8_t* a);

void blend_pixel(Surface* surface,
                    int x, int y,
                    PixelFormat srcFormat,
                    uint32_t srcPixel);