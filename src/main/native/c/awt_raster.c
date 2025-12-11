#include <stdlib.h>
#include <string.h>

#include "awt_raster.h"

// global state

ImageData g_images[MAX_IMAGES];
Surface g_surfaces[NUM_SURFACES];


static void set_pixel_generic(Surface* surface, int x, int y, PixelFormat srcFormat,
    uint32_t pixel) {
    uint32_t* framebuffer = (uint32_t*)(uintptr_t)surface->ptr;
    uint32_t stride = surface->stride / 4; // in pixels

    PixelFormatInfo* srcInfo = &g_pixel_format_info[srcFormat];
    PixelFormatInfo* dstInfo = &g_pixel_format_info[surface->pixel_format];

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
static void set_pixel_rgb_src(Surface* surface, int x, int y, PixelFormat srcFormat,
    uint32_t pixel) {
        pixel |= 0xFF000000; // set alpha to opaque
        set_pixel_generic(surface, x, y, PIXEL_FORMAT_ARGB, pixel);
}

// Utility functions

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

static inline ImageData* get_image_data(int id) {
    if (id < 0 || id >= MAX_IMAGES) return NULL;
    return &g_images[id];
}

// render functions

static inline void draw_filled_rect(Surface* surface, int x, int y, int width, int height, uint32_t color) {

    int x0 = clip_x(x, surface);
    int y0 = clip_y(y, surface);
    int x1 = clip_x(x + width, surface);
    int y1 = clip_y(y + height, surface);

    if (x0 >= x1 || y0 >= y1) {
        return;
    }

    uint32_t stride = surface->stride / 4; // in pixels

    uint32_t* framebuffer = (uint32_t*)(uintptr_t)surface->ptr;
    for (int j = y0; j < y1; j++) {
        for (int i = x0; i < x1; i++) {
            framebuffer[j * stride + i] = color;
        }
    }
}

static inline void clear_rect(Surface* surface,
    int x, int y,
    int width, int height) {
    draw_filled_rect(surface, x, y, width, height, surface->rgba[COLOR_BG]);
}

static inline void draw_rect(Surface* surface,
    int x, int y,
    int width, int height,
    uint32_t color) {
    // Top edge
    draw_filled_rect(surface, x, y, width, 1, color);
    // Bottom edge
    draw_filled_rect(surface, x, y + height - 1, width, 1, color);
    // Left edge
    draw_filled_rect(surface, x, y, 1, height, color);
    // Right edge
    draw_filled_rect(surface, x + width - 1, y, 1, height, color);
}

static inline void set_color(Surface* surface, int which, uint32_t rgba) {
    which = clamp_int(which, COLOR_MIN, COLOR_MAX);
    surface->rgba[which] = rgba;
}

static inline SetPixelFunc get_set_pixel_func(PixelFormat srcFormat, PixelFormat dstFormat) {
    if( srcFormat < 0 || srcFormat >= PIXEL_FORMAT_COUNT ||
        dstFormat < 0 || dstFormat >= PIXEL_FORMAT_COUNT) {
        return set_pixel_generic;
    }
    return g_set_pixel_funcs[srcFormat][dstFormat];
}

static inline void blit_image(Surface* surface, int image_id, int x, int y) {
    ImageData* img = get_image_data(image_id);
    if (!img || !img->ptr || img->width == 0 || img->height == 0) {
        return;
    }

    SetPixelFunc set_pixel_func = get_set_pixel_func(img->format, surface->pixel_format);

    // Compute clipped region in destination coords
    int startX = clip_x(x, surface);
    int startY = clip_y(y, surface);
    int endX   = clip_x(x + img->width, surface);
    int endY   = clip_y(y + img->height, surface);

    if (startX >= endX || startY >= endY) {
        return; // fully clipped
    }

    uint32_t* img_pixels = (uint32_t*)(uintptr_t)img->ptr;
    uint32_t img_stride  = img->stride / 4; // in pixels

    // memcpy hotpath for same format
    if (set_pixel_func == set_pixel_same_format) {
        uint32_t* framebuffer = (uint32_t*)(uintptr_t)surface->ptr;
        uint32_t surface_stride = surface->stride / 4; // in pixels
        for (int dst_y = startY; dst_y < endY; ++dst_y) {
            int src_y = dst_y - y; // since dst_y = y + src_y
            uint32_t* src_row = &img_pixels[src_y * img_stride + (startX - x)];
            uint32_t* dst_row = &framebuffer[dst_y * surface_stride + startX];
            size_t row_bytes = (size_t)(endX - startX) * sizeof(uint32_t);
            memcpy(dst_row, src_row, row_bytes);
        }
        return;
    }

    // Otherwise, per-pixel copy

    // For each dst pixel in clipped region, compute corresponding src coords
    for (int dst_y = startY; dst_y < endY; ++dst_y) {
        int src_y = dst_y - y; // since dst_y = y + src_y
        for (int dst_x = startX; dst_x < endX; ++dst_x) {
            int src_x = dst_x - x;
            uint32_t srcPixel = img_pixels[src_y * img_stride + src_x];
            set_pixel_func(surface, dst_x, dst_y, img->format, srcPixel);
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
            return i;
        }
    }
    return -1; // no free surface
}

__attribute__((export_name("reset_surface")))
int reset_surface(int surface_id, int width, int height, int pixel_format) {

    if (surface_id < 0 || surface_id >= NUM_SURFACES)
    {
        return -2;
    }

    Surface* surface = &g_surfaces[surface_id];

    if(surface->ptr) {
        free((void*)(uintptr_t)surface->ptr);
    }

    memset(surface, 0, sizeof(Surface));
    surface->width = width;
    surface->height = height;
    surface->stride = width * sizeof(uint32_t);
    surface->pixel_format = pixel_format;

    if(width == 0 || height == 0) {
        return 0; // zero-sized surface (freeing the surface)
    }

    size_t bytes = (size_t)width * (size_t)height * sizeof(uint32_t);
    void* p = malloc(bytes);
    if (!p) {
        surface->ptr = 0;
        surface->width = 0;
        surface->height = 0;
        return -1;
    }
    surface->ptr = (uint32_t)(uintptr_t)p;

    surface->rgba[COLOR_FG] = DEFAULT_FG_COLOR; // default to opaque black
    surface->rgba[COLOR_BG] = DEFAULT_BG_COLOR; // default

    // identity transform
    surface->transform[0] = 1.0f;
    surface->transform[3] = 1.0f;

    // clip_rect to full surface
    surface->clip.x = 0;
    surface->clip.y = 0;
    surface->clip.width = width;
    surface->clip.height = height;

    return 0;
}

__attribute__((export_name("get_surface_pixels_ptr")))
uint32_t get_surface_pixels_ptr(int surface_id) {
    CHECK_SURFACE_ID(surface_id);
    return g_surfaces[surface_id].ptr;
}

__attribute__((export_name("get_surface_width")))
int get_surface_width(int surface_id) {
    CHECK_SURFACE_ID(surface_id);
    return g_surfaces[surface_id].width;
}

__attribute__((export_name("get_surface_height")))
int get_surface_height(int surface_id) {
    CHECK_SURFACE_ID(surface_id);
    return g_surfaces[surface_id].height;
}

__attribute__((export_name("get_surface_stride")))
int get_surface_stride(int surface_id) {
    CHECK_SURFACE_ID(surface_id);
    return g_surfaces[surface_id].stride;
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

// pTHe plan is for the JS side to call this to allocate pixel buffers
// and pass the pointer back to us for rendering.
// that way we can have shared memory between the two sides.
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

    CHECK_SURFACE_ID_RET(surface_id, -1);

    Surface* surface = &g_surfaces[surface_id];

    ASSERT_SURFACE_VALID_RET(surface, -2);

    SurfaceCommand* cmds = (SurfaceCommand*)(uintptr_t)cmdPtr;
    for (int i = 0; i < cmdCount; i++) {
        SurfaceCommand* cmd = &cmds[i];
        switch (cmd->operation) {
            case CMD_SET_COLOR:
                 set_color(surface, cmd->set_color.which, cmd->set_color.rgba);
                break;
            // case CMD_SET_TRANSFORM:
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
                          surface->rgba[COLOR_FG]);
                break;
            case CMD_FILL_RECT:
                draw_filled_rect(surface, cmd->x, cmd->y, cmd->width, cmd->height,
                                 surface->rgba[COLOR_FG]);
                break;
            case CMD_CLEAR_RECT:
                clear_rect(surface, cmd->x, cmd->y, cmd->width, cmd->height);
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