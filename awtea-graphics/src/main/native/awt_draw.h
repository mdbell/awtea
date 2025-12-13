#pragma once
#include "awt_raster_internal.h"

void draw_filled_rect(Surface* surface,
                        int x, int y,
                        int width, int height,
                        uint32_t color);

void clear_rect(Surface* surface,
                int x, int y,
                int width, int height);

void draw_rect(Surface* surface,
                int x, int y,
                int width, int height,
                uint32_t color);

void set_color(Surface* surface, int which, uint32_t argb);

void draw_line(Surface* surf,
                        int x1, int y1,
                        int x2, int y2,
                        uint32_t color);

void blit_image(Surface* dst, int image_id, int x, int y);

void blit_from_view(Surface* dst,
                        const ImageView* src,
                        int x, int y);