#include "awt_draw.h"
#include "awt_image.h"
#include "awt_surface.h"
#include "awt_transform.h"
#include "awt_pixel.h"
#include "awt_util.h"

static inline ImageView* lookup_by_id(int id) {
    if(id >= START_IMAGE_ID && id < END_IMAGE_ID) {
        return get_image_data(id);
    }
    if(id >= START_SURFACE_ID && id < END_SURFACE_ID) {
        return (ImageView*)get_surface_data(id);
    }
    return NULL;
}

void draw_filled_rect(Surface* surface,
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

void clear_rect(Surface* surface,
    int x, int y,
    int width, int height) {
    draw_filled_rect(surface, x, y, width, height, surface->argb[COLOR_BG]);
}

void draw_rect(Surface* surface,
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

void set_color(Surface* surface, int which, uint32_t argb) {
    which = clamp_int(which, COLOR_MIN, COLOR_MAX);
    surface->argb[which] = argb;
}

void draw_line(Surface* surf,
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

void blit_image(Surface* dst, int image_id, int x, int y) {
    ImageView* img = lookup_by_id(image_id);
    if (!img || !img->ptr || img->width == 0 || img->height == 0) {
        return;
    }
    blit_from_view(dst, img, x, y);
}

void blit_from_view(Surface* dst,
                                  const ImageView* src,
                                  int x, int y) {
    if (!src || !src->ptr || src->width == 0 || src->height == 0) {
        return;
    }

    const PixelFormatInfo* srcInfo = &g_pixel_format_info[src->format];
    uint32_t* src_pixels = (uint32_t*)(uintptr_t)src->ptr;
    uint32_t  src_stride = src->stride / 4; // in pixels

    // Identity transform blit: simple copy
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