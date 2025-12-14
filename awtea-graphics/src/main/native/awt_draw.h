#pragma once
#include "awt_raster_internal.h"

// Helper to construct a RenderSurface from data + context
static inline RenderSurface make_render_surface(const SurfaceData* data, const SurfaceContext* ctx) {
    RenderSurface surf;
    surf.ptr = data->ptr;
    surf.format = data->format;
    surf.width = data->width;
    surf.height = data->height;
    surf.stride = data->stride;
    surf.layer = data->layer;
    surf.argb[COLOR_FG] = ctx->argb[COLOR_FG];
    surf.argb[COLOR_BG] = ctx->argb[COLOR_BG];
    surf.transform = ctx->transform;
    surf.clip = ctx->clip;
    return surf;
}

void draw_filled_rect(RenderSurface* surface,
                        int x, int y,
                        int width, int height,
                        uint32_t color);

void clear_rect(RenderSurface* surface,
                int x, int y,
                int width, int height);

void draw_rect(RenderSurface* surface,
                int x, int y,
                int width, int height,
                uint32_t color);

void set_color(SurfaceContext* ctx, int which, uint32_t argb);

void draw_line(RenderSurface* surf,
                        int x1, int y1,
                        int x2, int y2,
                        uint32_t color);

void blit_image(RenderSurface* dst, int image_id, int x, int y);

void blit_from_view(RenderSurface* dst,
                        const ImageView* src,
                        int x, int y);