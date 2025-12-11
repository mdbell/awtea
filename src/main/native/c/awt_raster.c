#include <math.h>
#include <stdlib.h>
#include <string.h>

#include "awt_raster.h"

// global state

static const PixelFormatInfo g_pixel_format_info[PIXEL_FORMAT_COUNT] = {
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
        .alphaVariant = PIXEL_FORMAT_ARGB,
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
        .alphaVariant = PIXEL_FORMAT_ABGR,
    }
};

ImageView g_images[MAX_IMAGES];
Surface g_surfaces[NUM_SURFACES];

static inline int is_identity_transform(const Transform2D* t) {
    return  t->m00 == 1.0f && t->m11 == 1.0f &&
            t->m01 == 0.0f && t->m10 == 0.0f &&
            t->m02 == 0.0f && t->m12 == 0.0f;
}

static inline int invert_transform(const Transform2D* t, Transform2D* out) {
    float det = t->m00 * t->m11 - t->m01 * t->m10;
    if (det == 0.0f) {
        return 0; // non-invertible
    }
    float invDet = 1.0f / det;

    out->m00 =  t->m11 * invDet;
    out->m01 = -t->m01 * invDet;
    out->m10 = -t->m10 * invDet;
    out->m11 =  t->m00 * invDet;

    // translation part: -R⁻¹ * t
    out->m02 = -(out->m00 * t->m02 + out->m01 * t->m12);
    out->m12 = -(out->m10 * t->m02 + out->m11 * t->m12);

    return 1;
}


static inline float u32_to_float(uint32_t v) {
    union { uint32_t u; float f; } u;
    u.u = v;
    return u.f;
}


static inline void transform_point(const Transform2D* t,
                                   float x, float y,
                                   float* outX, float* outY) {
    *outX = t->m00 * x + t->m01 * y + t->m02;
    *outY = t->m10 * x + t->m11 * y + t->m12;
}


static inline void transform_rect(
        const Transform2D* t,
        int x, int y, int w, int h,
        int* outX, int* outY,
        int* outW, int* outH) {

    if (is_identity_transform(t)) {
        *outX = x;
        *outY = y;
        *outW = w;
        *outH = h;
        return;
    }

    float x0 = (float)x;
    float y0 = (float)y;
    float x1 = (float)(x + w);
    float y1 = (float)(y + h);

    // transform four corners
    float tx0 = t->m00 * x0 + t->m01 * y0 + t->m02;
    float ty0 = t->m10 * x0 + t->m11 * y0 + t->m12;

    float tx1 = t->m00 * x1 + t->m01 * y0 + t->m02;
    float ty1 = t->m10 * x1 + t->m11 * y0 + t->m12;

    float tx2 = t->m00 * x1 + t->m01 * y1 + t->m02;
    float ty2 = t->m10 * x1 + t->m11 * y1 + t->m12;

    float tx3 = t->m00 * x0 + t->m01 * y1 + t->m02;
    float ty3 = t->m10 * x0 + t->m11 * y1 + t->m12;

    float minX = fminf(fminf(tx0, tx1), fminf(tx2, tx3));
    float minY = fminf(fminf(ty0, ty1), fminf(ty2, ty3));
    float maxX = fmaxf(fmaxf(tx0, tx1), fmaxf(tx2, tx3));
    float maxY = fmaxf(fmaxf(ty0, ty1), fmaxf(ty2, ty3));

    int ix = (int)floorf(minX);
    int iy = (int)floorf(minY);
    int iw = (int)ceilf(maxX) - ix;
    int ih = (int)ceilf(maxY) - iy;

    *outX = ix;
    *outY = iy;
    *outW = iw;
    *outH = ih;
}

static void set_pixel_generic(Surface* surface, int x, int y, PixelFormat srcFormat,
    uint32_t pixel) {
    uint32_t* framebuffer = (uint32_t*)(uintptr_t)surface->ptr;
    uint32_t stride = surface->stride / 4; // in pixels

    const PixelFormatInfo* srcInfo = &g_pixel_format_info[srcFormat];
    const PixelFormatInfo* dstInfo = &g_pixel_format_info[surface->format];

    uint32_t r = (pixel & srcInfo->mask_r) >> srcInfo->shift_r;
    uint32_t g = (pixel & srcInfo->mask_g) >> srcInfo->shift_g;
    uint32_t b = (pixel & srcInfo->mask_b) >> srcInfo->shift_b;
    uint32_t a = (pixel & srcInfo->mask_a) >> srcInfo->shift_a;

    uint32_t dst_pixel = 
        ((r << dstInfo->shift_r) & dstInfo->mask_r) |
        ((g << dstInfo->shift_g) & dstInfo->mask_g) |
        ((b << dstInfo->shift_b) & dstInfo->mask_b) |
        ((a << dstInfo->shift_a) & dstInfo->mask_a);

    framebuffer[y * stride + x] = dst_pixel;
}

static void set_pixel_same_format(Surface* surface, int x, int y, PixelFormat srcFormat,
    uint32_t pixel) {
    uint32_t* framebuffer = (uint32_t*)(uintptr_t)surface->ptr;
    uint32_t stride = surface->stride / 4; // in pixels

    framebuffer[y * stride + x] = pixel;
}

// rgb has no alpha, so it needs a special case
static void set_pixel_no_alpha_src(Surface* surface, int x, int y, PixelFormat srcFormat,
    uint32_t pixel) {

        PixelFormat alphaVariant = g_pixel_format_info[srcFormat].alphaVariant;
        pixel |= g_pixel_format_info[alphaVariant].mask_a; // set alpha to opaque

        set_pixel_generic(surface, x, y, alphaVariant, pixel);
}


// Utility functions

static inline void unpack_pixel(const PixelFormatInfo* info,
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

static inline uint32_t pack_pixel(const PixelFormatInfo* info,
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

static void blend_pixel(Surface* surface,
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


static inline int clamp_int(int v, int lo, int hi) {
    if (v < lo){
        return lo;
    }
    if (v > hi) {
        return hi;
    }
    return v;
}

static inline int clip_x(int x, const Surface* surf) {
    x = clamp_int(x, 0, surf->width);
    if (surf->clip.width > 0) {
        x = clamp_int(x, surf->clip.x, surf->clip.x + surf->clip.width);
    }
    return x;
}

static inline int clip_y(int y, const Surface* surf) {
    y = clamp_int(y, 0, surf->height);
    if (surf->clip.height > 0) {
        y = clamp_int(y, surf->clip.y, surf->clip.y + surf->clip.height);
    }
    return y;
}

static inline ImageView* get_image_data(int id) {
    if (id < START_IMAGE_ID || id >= END_IMAGE_ID) return NULL;
    return &g_images[id];
}

static inline Surface* get_surface_data(int id) {
    if (id < START_SURFACE_ID || id >= END_SURFACE_ID) return NULL;
    return &g_surfaces[id - START_SURFACE_ID];
}

static inline ImageView* lookup_by_id(int id) {
    if(id >= START_IMAGE_ID && id < END_IMAGE_ID) {
        return get_image_data(id);
    }
    if(id >= START_SURFACE_ID && id < END_SURFACE_ID) {
        return (ImageView*)get_surface_data(id);
    }
    return NULL;
}

// render functions

static inline void draw_filled_rect(Surface* surface,
                                    int x, int y,
                                    int width, int height,
                                    uint32_t color) {

    if (is_identity_transform(&surface->transform)) {
        int x0 = clip_x(x, surface);
        int y0 = clip_y(y, surface);
        int x1 = clip_x(x + width, surface);
        int y1 = clip_y(y + height, surface);

        if (x0 >= x1 || y0 >= y1) {
            return;
        }

        PixelFormat format = surface->format;
        const PixelFormatInfo* dstInfo = &g_pixel_format_info[format];

        // destination has no alpha -> convert/write directly
        if (dstInfo->mask_a == 0) {
            SetPixelFunc set_pixel_func =
                get_set_pixel_func(PIXEL_FORMAT_ARGB, surface->format);

            uint32_t* framebuffer = (uint32_t*)(uintptr_t)surface->ptr;
            uint32_t stride = surface->stride / 4;

            for (int j = y0; j < y1; j++) {
                for (int i = x0; i < x1; i++) {
                    set_pixel_func(surface, i, j, PIXEL_FORMAT_ARGB, color);
                }
            }
            return;
        }

        // destination has alpha -> blend
        for (int j = y0; j < y1; j++) {
            for (int i = x0; i < x1; i++) {
                blend_pixel(surface, i, j, PIXEL_FORMAT_ARGB, color);
            }
        }
        return;
    }

    // Compute bounding box of transformed rect
    int tx, ty, tw, th;
    transform_rect(&surface->transform, x, y, width, height,
                   &tx, &ty, &tw, &th);

    int x0 = clip_x(tx, surface);
    int y0 = clip_y(ty, surface);
    int x1 = clip_x(tx + tw, surface);
    int y1 = clip_y(ty + th, surface);

    if (x0 >= x1 || y0 >= y1) {
        return;
    }

    // Invert the transform
    Transform2D inv;
    if (!invert_transform(&surface->transform, &inv)) {
        return; // non-invertible
    }

    const PixelFormatInfo* dstInfo = &g_pixel_format_info[surface->format];
    int dstHasAlpha = (dstInfo->mask_a != 0);

    uint32_t* framebuffer = (uint32_t*)(uintptr_t)surface->ptr;
    uint32_t stride = surface->stride / 4;

    for (int dy = y0; dy < y1; ++dy) {
        for (int dx = x0; dx < x1; ++dx) {
            // center of dest pixel in device coords
            float fx = (float)dx + 0.5f;
            float fy = (float)dy + 0.5f;

            // map back to user space via inverse transform
            float ux = inv.m00 * fx + inv.m01 * fy + inv.m02;
            float uy = inv.m10 * fx + inv.m11 * fy + inv.m12;

            // check if that user-space point lies inside the original rect
            if (ux < (float)x || uy < (float)y ||
                ux >= (float)(x + width) || uy >= (float)(y + height)) {
                continue;
            }

            if (!dstHasAlpha) {
                // opaque dst: just convert+write color
                set_pixel_generic(surface, dx, dy, PIXEL_FORMAT_ARGB, color);
            } else {
                // alpha dst: reuse existing blending logic
                blend_pixel(surface, dx, dy, PIXEL_FORMAT_ARGB, color);
            }
        }
    }
}


static inline void clear_rect(Surface* surface,
    int x, int y,
    int width, int height) {
    draw_filled_rect(surface, x, y, width, height, surface->argb[COLOR_BG]);
}

static inline void draw_rect(Surface* surface,
    int x, int y,
    int width, int height,
    uint32_t color) {
    // Top edge: width+1 pixels
    draw_filled_rect(surface, x,         y,          width + 1, 1,          color);
    // Bottom edge
    draw_filled_rect(surface, x,         y + height, width + 1, 1,          color);
    // Left edge: height+1 pixels
    draw_filled_rect(surface, x,         y,          1,         height + 1, color);
    // Right edge
    draw_filled_rect(surface, x + width, y,          1,         height + 1, color);
}

static inline void set_color(Surface* surface, int which, uint32_t argb) {
    which = clamp_int(which, COLOR_MIN, COLOR_MAX);
    surface->argb[which] = argb;
}

static inline SetPixelFunc get_set_pixel_func(PixelFormat srcFormat, PixelFormat dstFormat) {
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

static inline void blit_from_view(Surface* dst,
                                  const ImageView* src,
                                  int x, int y) {
    if (!src || !src->ptr || src->width == 0 || src->height == 0) {
        return;
    }

    const PixelFormatInfo* srcInfo = &g_pixel_format_info[src->format];
    uint32_t* src_pixels = (uint32_t*)(uintptr_t)src->ptr;
    uint32_t  src_stride = src->stride / 4; // in pixels

    // Idenity transform blit: simple copy
    if (is_identity_transform(&dst->transform)) {

        int startX = clip_x(x, dst);
        int startY = clip_y(y, dst);
        int endX   = clip_x(x + (int)src->width, dst);
        int endY   = clip_y(y + (int)src->height, dst);

        if (startX >= endX || startY >= endY) {
            return; // fully clipped
        }

        // If source has no alpha at all -> copy (old behavior)
        if (srcInfo->mask_a == 0) {
            SetPixelFunc set_pixel_func = get_set_pixel_func(src->format, dst->format);

            if (set_pixel_func == set_pixel_same_format) {
                // memcpy row fast path
                uint32_t* dst_pixels = (uint32_t*)(uintptr_t)dst->ptr;
                uint32_t  dst_stride = dst->stride / 4;
                for (int dst_y = startY; dst_y < endY; ++dst_y) {
                    int src_y = dst_y - y;
                    uint32_t* src_row = &src_pixels[src_y * src_stride + (startX - x)];
                    uint32_t* dst_row = &dst_pixels[dst_y * dst_stride + startX];
                    size_t row_bytes = (size_t)(endX - startX) * sizeof(uint32_t);
                    memcpy(dst_row, src_row, row_bytes);
                }
                return;
            }

            // non-alpha, non-same-format
            for (int dst_y = startY; dst_y < endY; ++dst_y) {
                int src_y = dst_y - y;
                for (int dst_x = startX; dst_x < endX; ++dst_x) {
                    int src_x = dst_x - x;
                    uint32_t srcPixel = src_pixels[src_y * src_stride + src_x];
                    set_pixel_func(dst, dst_x, dst_y, src->format, srcPixel);
                }
            }
            return;
        }

        // Source *has* alpha → blend
        for (int dst_y = startY; dst_y < endY; ++dst_y) {
            int src_y = dst_y - y;
            for (int dst_x = startX; dst_x < endX; ++dst_x) {
                int src_x = dst_x - x;
                uint32_t srcPixel = src_pixels[src_y * src_stride + src_x];
                blend_pixel(dst, dst_x, dst_y, src->format, srcPixel);
            }
        }
        return;
    }

    // Compute bounding box of transformed image in dest coords
    int tx, ty, tw, th;
    transform_rect(&dst->transform, x, y, (int)src->width, (int)src->height,
                   &tx, &ty, &tw, &th);

    int startX = clip_x(tx, dst);
    int startY = clip_y(ty, dst);
    int endX   = clip_x(tx + tw, dst);
    int endY   = clip_y(ty + th, dst);

    if (startX >= endX || startY >= endY) {
        return; // fully clipped
    }

    // Invert the transform
    Transform2D inv;
    if (!invert_transform(&dst->transform, &inv)) {
        return; // non-invertible, nothing to draw
    }

    // We will sample source per destination pixel
    int hasAlpha = (srcInfo->mask_a != 0);

    for (int dy = startY; dy < endY; ++dy) {
        for (int dx = startX; dx < endX; ++dx) {

            // Center of the destination pixel in device space
            float fx = (float)dx + 0.5f;
            float fy = (float)dy + 0.5f;

            // Map back into user space
            float ux = inv.m00 * fx + inv.m01 * fy + inv.m02;
            float uy = inv.m10 * fx + inv.m11 * fy + inv.m12;

            // Convert user-space → source pixel coordinates
            float sx = ux - (float)x;
            float sy = uy - (float)y;

            if (sx < 0.0f || sy < 0.0f ||
                sx >= (float)src->width || sy >= (float)src->height) {
                continue; // outside source
            }

            // Nearest neighbor
            int isx = (int)floorf(sx);
            int isy = (int)floorf(sy);

            uint32_t srcPixel = src_pixels[isy * src_stride + isx];

            if (!hasAlpha) {
                // no alpha in src -> direct write/convert
                SetPixelFunc set_pixel_func = get_set_pixel_func(src->format, dst->format);
                set_pixel_func(dst, dx, dy, src->format, srcPixel);
            } else {
                // has alpha -> blend
                blend_pixel(dst, dx, dy, src->format, srcPixel);
            }
        }
    }
}

static inline void blit_image(Surface* dst, int image_id, int x, int y) {
    ImageView* img = lookup_by_id(image_id);
    if (!img || !img->ptr || img->width == 0 || img->height == 0) {
        return;
    }
    blit_from_view(dst, img, x, y);
}

static inline void draw_line(Surface* surf,
                             int x1, int y1,
                             int x2, int y2,
                             uint32_t color) {
    // Transform endpoints into destination space
    float fx1 = (float)x1, fy1 = (float)y1;
    float fx2 = (float)x2, fy2 = (float)y2;

    if (!is_identity_transform(&surf->transform)) {
        transform_point(&surf->transform, fx1, fy1, &fx1, &fy1);
        transform_point(&surf->transform, fx2, fy2, &fx2, &fy2);
    }

    int x0 = (int)fx1;
    int y0 = (int)fy1;
    int x1i = (int)fx2;
    int y1i = (int)fy2;

    int dx = abs(x1i - x0);
    int sx = x0 < x1i ? 1 : -1;
    int dy = -abs(y1i - y0);
    int sy = y0 < y1i ? 1 : -1;
    int err = dx + dy;

    PixelFormatInfo dstInfo = g_pixel_format_info[surf->format];
    int hasAlpha = (dstInfo.mask_a != 0);

    SetPixelFunc set_pixel_func = get_set_pixel_func(PIXEL_FORMAT_ARGB,
                                        surf->format);

    for (;;) {
        if (x0 >= 0 && x0 < (int)surf->width &&
            y0 >= 0 && y0 < (int)surf->height) {

            if (hasAlpha) {
                blend_pixel(surf, x0, y0, PIXEL_FORMAT_ARGB, color);
            } else {
                set_pixel_func(surf, x0, y0, PIXEL_FORMAT_ARGB, color);
            }
        }

        if (x0 == x1i && y0 == y1i){
            break;
        }
        int e2 = 2 * err;
        if (e2 >= dy) {
            err += dy; x0 += sx;
        }
        if (e2 <= dx) {
            err += dx; y0 += sy;
        }
    }
}


// Exported functions

__attribute__((export_name("get_command_size")))
int get_command_size() {
    return sizeof(SurfaceCommand);
}

__attribute__((export_name("find_free_surface")))
int find_free_surface() {
    for (int i = 0; i < NUM_SURFACES; i++) {
        if (g_surfaces[i].ptr == 0) {
            return i + START_SURFACE_ID;
        }
    }
    return -1; // no free surface
}

__attribute__((export_name("reset_surface")))
int reset_surface(int surface_id, int layer, int width, int height, PixelFormat format) {

    if (surface_id < START_SURFACE_ID || surface_id >= END_SURFACE_ID)
    {
        return -3;
    }

    Surface* surface = get_surface_data(surface_id);

    if(!surface) {
        return -2;
    }

    if(surface->ptr) {
        free((void*)(uintptr_t)surface->ptr);
    }

    memset(surface, 0, sizeof(Surface));

    if(width == 0 || height == 0 || layer < 0) {
        return 0; // zero-sized surface (freeing the surface)
    }

    surface->layer = (uint32_t)layer;
    surface->width = width;
    surface->height = height;
    surface->stride = width * sizeof(uint32_t);
    surface->format = format;

    size_t bytes = (size_t)width * (size_t)height * sizeof(uint32_t);
    void* p = malloc(bytes);
    if (!p) {
        surface->ptr = 0;
        surface->width = 0;
        surface->height = 0;
        return -1;
    }
    surface->ptr = (uint32_t)(uintptr_t)p;

    surface->argb[COLOR_FG] = DEFAULT_FG_COLOR; // default to opaque black
    surface->argb[COLOR_BG] = DEFAULT_BG_COLOR; // default

    // identity transform
    surface->transform.m00 = 1.0f;
    surface->transform.m01 = 0.0f;
    surface->transform.m02 = 0.0f;
    surface->transform.m10 = 0.0f;
    surface->transform.m11 = 1.0f;
    surface->transform.m12 = 0.0f;

    // clip_rect to full surface
    surface->clip.x = 0;
    surface->clip.y = 0;
    surface->clip.width = width;
    surface->clip.height = height;

    return 0;
}

__attribute__((export_name("get_surface_pixels_ptr")))
uint32_t get_surface_pixels_ptr(int surface_id) {
    Surface* surface = get_surface_data(surface_id);
    if (!surface) {
        return 0;
    }
    return surface->ptr;
}

__attribute__((export_name("get_surface_width")))
int get_surface_width(int surface_id) {
    Surface* surface = get_surface_data(surface_id);
    if (!surface) {
        return 0;
    }
    return surface->width;
}

__attribute__((export_name("get_surface_height")))
int get_surface_height(int surface_id) {
    Surface* surface = get_surface_data(surface_id);
    if (!surface) {
        return 0;
    }
    return surface->height;
}

__attribute__((export_name("get_surface_stride")))
int get_surface_stride(int surface_id) {
    Surface* surface = get_surface_data(surface_id);
    if (!surface) {
        return 0;
    }
    return surface->stride;
}

__attribute__((export_name("register_image")))
void register_image(int id, uint32_t ptr, int format, int width, int height, int stride) {
    if (id < 0 || id >= MAX_IMAGES) return;
    g_images[id].ptr    = ptr;
    g_images[id].format = format;
    g_images[id].width  = width;
    g_images[id].height = height;
    g_images[id].stride = stride;
}

__attribute__((export_name("alloc_pixels")))
uint32_t alloc_pixels(int width, int height) {
    size_t bytes = (size_t)width * (size_t)height * sizeof(uint32_t);
    void* p = malloc(bytes);
    if (!p){
        return 0;
    }
    memset(p, 0, bytes);
    return (uint32_t)(uintptr_t)p;
}

__attribute__((export_name("free_pixels")))
void free_pixels(uint32_t ptr) {
    free((void*)(uintptr_t)ptr);
}

__attribute__((export_name("render_awt")))
int render_awt(int surface_id, uint32_t cmdPtr, int cmdCount) {

    Surface* surface = get_surface_data(surface_id);

    if( !surface || !surface->ptr ) {
        return -1;
    }

    SurfaceCommand* cmds = (SurfaceCommand*)(uintptr_t)cmdPtr;
    for (int i = 0; i < cmdCount; i++) {
        SurfaceCommand* cmd = &cmds[i];
        switch (cmd->operation) {
            case CMD_SET_COLOR:
                 set_color(surface, cmd->set_color.which, cmd->set_color.argb);
                break;
            case CMD_SET_TRANSFORM:
                surface->transform.m00 = u32_to_float(cmd->x);
                surface->transform.m01 = u32_to_float(cmd->y);
                surface->transform.m02 = u32_to_float(cmd->width);
                surface->transform.m10 = u32_to_float(cmd->height);
                surface->transform.m11 = u32_to_float(cmd->args[0]);
                surface->transform.m12 = u32_to_float(cmd->args[1]);
                break;
            case CMD_SET_CLIP_RECT:
                surface->clip.x = cmd->x;
                surface->clip.y = cmd->y;
                surface->clip.width = cmd->width;
                surface->clip.height = cmd->height;
            break;    

            // Drawing commands
            case CMD_BLIT_IMAGE:
                blit_image(surface, cmd->blit.image_id, cmd->x, cmd->y);
            break;
            case CMD_DRAW_RECT:
                draw_rect(surface, cmd->x, cmd->y, cmd->width, cmd->height,
                          surface->argb[COLOR_FG]);
                break;
            case CMD_FILL_RECT:
                draw_filled_rect(surface, cmd->x, cmd->y, cmd->width, cmd->height,
                                 surface->argb[COLOR_FG]);
                break;
            case CMD_CLEAR_RECT:
                clear_rect(surface, cmd->x, cmd->y, cmd->width, cmd->height);
                break;
            case CMD_DRAW_LINE:
                draw_line(surface, cmd->x, cmd->y,
                          cmd->width, cmd->height,
                          surface->argb[COLOR_FG]);
                break;

            // No-op or unknown command

            case CMD_NO_OP:
            default:
                // do nothing
                break;
        }
    }
    return 0;
}

int main(void) {
    return 0;
}