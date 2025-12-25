#include "awt_draw.h"
#include "awt_surface.h"
#include "awt_transform.h"
#include "awt_pixel.h"
#include "awt_util.h"
#include "awt_log.h"
#include "awt_stack.h"
#include "awt_edge_table.h"
#include "awt_edge_table_fixed.h"
#include "awt_memory.h"
#include "awt_fast_fill.h"

// Default edge table pool initial capacity
#define EDGE_TABLE_POOL_INITIAL_CAPACITY 4

// Compile-time flag to choose between float and fixed-point rasterizer
// Set to 1 for fixed-point (default), 0 for float
#ifndef USE_FIXED_POINT_RASTERIZER
#define USE_FIXED_POINT_RASTERIZER 1
#endif

// Global edge table pool (lazy initialization)
// Use fixed-point version by default for performance
#if USE_FIXED_POINT_RASTERIZER
static EdgeTableFixedPool* g_edge_table_pool = NULL;
#else
static EdgeTablePool* g_edge_table_pool = NULL;
#endif

void draw_filled_rect(SurfaceData* surface, SurfaceContext* context,
                                    int x, int y,
                                    int width, int height,
                                    uint32_t color) {
    STACK_ENTER();

    if (is_identity_transform(&context->transform)) {
        int x0 = clip_x(x, surface, context);
        int y0 = clip_y(y, surface, context);
        int x1 = clip_x(x + width, surface, context);
        int y1 = clip_y(y + height, surface, context);

        log_debug("draw_filled_rect: requested [%d,%d,%d,%d], clipped to [%d,%d] - [%d,%d]",
                  x, y, width, height, x0, y0, x1, y1);

        if (x0 >= x1 || y0 >= y1) {
            log_debug("draw_filled_rect: clipped out entirely (x0=%d >= x1=%d or y0=%d >= y1=%d)",
                      x0, x1, y0, y1);
            STACK_EXIT();
            return;
        }

        PixelFormat format = surface->format;
        const PixelFormatInfo* dstInfo = &g_pixel_format_info[format];

        // destination has no alpha -> convert/write directly
        if (dstInfo->mask_a == 0) {
            uint32_t* framebuffer = (uint32_t*)(uintptr_t)surface->ptr;
            uint32_t stride = surface->stride / 4;
            
            // Convert color to destination format once
            uint8_t r, g, b, a;
            unpack_pixel(&g_pixel_format_info[PIXEL_FORMAT_ARGB], color, &r, &g, &b, &a);
            uint32_t dst_color = pack_pixel(dstInfo, r, g, b, a);
            
            // FAST PATH: Use optimized scanline fills
            for (int j = y0; j < y1; j++) {
                fast_fill_scanline(framebuffer, j, stride, x0, x1, dst_color);
            }
            log_debug("draw_filled_rect: fast fill %d pixels", (x1-x0)*(y1-y0));
            STACK_EXIT();
            return;
        }

        // destination has alpha -> use composite blending
        // Check if we can skip blending for certain modes
        int needsBlending = (context->composite_mode != COMPOSITE_SRC) && 
                           (context->composite_mode != COMPOSITE_DST) &&
                           (context->composite_mode != COMPOSITE_CLEAR);
        
        if (!needsBlending) {
            uint32_t* framebuffer = (uint32_t*)(uintptr_t)surface->ptr;
            uint32_t stride = surface->stride / 4;
            
            // Fast path for SRC/DST/CLEAR modes
            if (context->composite_mode == COMPOSITE_SRC) {
                // Convert color to destination format once
                uint8_t r, g, b, a;
                unpack_pixel(&g_pixel_format_info[PIXEL_FORMAT_ARGB], color, &r, &g, &b, &a);
                uint32_t dst_color = pack_pixel(dstInfo, r, g, b, a);
                
                // FAST PATH: Use optimized scanline fills
                for (int j = y0; j < y1; j++) {
                    fast_fill_scanline(framebuffer, j, stride, x0, x1, dst_color);
                }
            } else if (context->composite_mode == COMPOSITE_CLEAR) {
                // For CLEAR mode, write transparent pixels (all zeros)
                uint32_t clear_color = 0x00000000;
                
                // FAST PATH: Use optimized scanline fills
                for (int j = y0; j < y1; j++) {
                    fast_fill_scanline(framebuffer, j, stride, x0, x1, clear_color);
                }
            }
            // For DST mode, don't draw anything
            log_debug("draw_filled_rect: used fast fill for mode %d", context->composite_mode);
            STACK_EXIT();
            return;
        }
        
        // Normal blending path (still needs per-pixel blending)
        for (int j = y0; j < y1; j++) {
            for (int i = x0; i < x1; i++) {
                blend_pixel_composite(surface, i, j, PIXEL_FORMAT_ARGB, color,
                                     context->composite_mode, context->composite_alpha);
            }
        }
        log_debug("draw_filled_rect: blended %d pixels", (x1-x0)*(y1-y0));
        STACK_EXIT();
        return;
    }

    // Compute bounding box of transformed rect
    int tx, ty, tw, th;
    transform_rect(&context->transform, x, y, width, height,
                   &tx, &ty, &tw, &th);

    int x0 = clip_x(tx, surface, context);
    int y0 = clip_y(ty, surface, context);
    int x1 = clip_x(tx + tw, surface, context);
    int y1 = clip_y(ty + th, surface, context);

    if (x0 >= x1 || y0 >= y1) {
        STACK_EXIT();
        return;
    }

    // Invert the transform
    Transform2D inv;
    if (!invert_transform(&context->transform, &inv)) {
        STACK_EXIT();
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
                // alpha dst: use composite blending
                blend_pixel_composite(surface, dx, dy, PIXEL_FORMAT_ARGB, color,
                                     context->composite_mode, context->composite_alpha);
            }
        }
    }
    STACK_EXIT();
}

void clear_rect(SurfaceData* surface, SurfaceContext* context,
    int x, int y,
    int width, int height) {
    draw_filled_rect(surface, context, x, y, width, height, context->argb[COLOR_BG]);
}

void draw_rect(SurfaceData* surface, SurfaceContext* context,
    int x, int y,
    int width, int height,
    uint32_t color) {
    // Top edge: width+1 pixels
    draw_filled_rect(surface, context, x,         y,          width + 1, 1,          color);
    // Bottom edge
    draw_filled_rect(surface, context, x,         y + height, width + 1, 1,          color);
    // Left edge: height+1 pixels
    draw_filled_rect(surface, context, x,         y,          1,         height + 1, color);
    // Right edge
    draw_filled_rect(surface, context, x + width, y,          1,         height + 1, color);
}

void set_color(SurfaceContext* ctx, int which, uint32_t argb) {
    which = clamp_int(which, COLOR_MIN, COLOR_MAX);
    ctx->argb[which] = argb;
}

void draw_line(SurfaceData* surf, SurfaceContext* context,
                             int x1, int y1,
                             int x2, int y2,
                             uint32_t color) {
    // Transform endpoints into destination space
    float fx1 = (float)x1, fy1 = (float)y1;
    float fx2 = (float)x2, fy2 = (float)y2;

    if (!is_identity_transform(&context->transform)) {
        transform_point(&context->transform, fx1, fy1, &fx1, &fy1);
        transform_point(&context->transform, fx2, fy2, &fx2, &fy2);
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
                blend_pixel_composite(surf, x0, y0, PIXEL_FORMAT_ARGB, color,
                                     context->composite_mode, context->composite_alpha);
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

void blit_image(SurfaceData* dst, SurfaceContext* context, int src_surface_id, int x, int y) {
    STACK_ENTER();
    
    // Get source surface data
    SurfaceData* src = get_surface_data(src_surface_id);
    if (!src || !src->ptr || src->width == 0 || src->height == 0) {
        STACK_EXIT();
        return;
    }

    const PixelFormatInfo* srcInfo = &g_pixel_format_info[src->format];
    uint32_t* src_pixels = (uint32_t*)(uintptr_t)src->ptr;
    uint32_t  src_stride = src->stride / 4; // in pixels

    // Identity transform blit: simple copy
    if (is_identity_transform(&context->transform)) {

        int startX = clip_x(x, dst, context);
        int startY = clip_y(y, dst, context);
        int endX   = clip_x(x + (int)src->width, dst, context);
        int endY   = clip_y(y + (int)src->height, dst, context);

        if (startX >= endX || startY >= endY) {
            STACK_EXIT();
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
                STACK_EXIT();
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
            STACK_EXIT();
            return;
        }

        // Source *has* alpha → check for fast paths before generic blending
        
        // Fast path 1: COMPOSITE_SRC mode (copy source, ignore destination)
        if (context->composite_mode == COMPOSITE_SRC && context->composite_alpha >= 1.0f) {
            SetPixelFunc set_pixel_func = get_set_pixel_func(src->format, dst->format);
            
            if (set_pixel_func == set_pixel_same_format) {
                // Same format: use memcpy for rows
                uint32_t* dst_pixels = (uint32_t*)(uintptr_t)dst->ptr;
                uint32_t  dst_stride = dst->stride / 4;
                for (int dst_y = startY; dst_y < endY; ++dst_y) {
                    int src_y = dst_y - y;
                    uint32_t* src_row = &src_pixels[src_y * src_stride + (startX - x)];
                    uint32_t* dst_row = &dst_pixels[dst_y * dst_stride + startX];
                    size_t row_bytes = (size_t)(endX - startX) * sizeof(uint32_t);
                    memcpy(dst_row, src_row, row_bytes);
                }
            } else {
                // Different formats: direct set without blending
                for (int dst_y = startY; dst_y < endY; ++dst_y) {
                    int src_y = dst_y - y;
                    for (int dst_x = startX; dst_x < endX; ++dst_x) {
                        int src_x = dst_x - x;
                        uint32_t srcPixel = src_pixels[src_y * src_stride + src_x];
                        set_pixel_func(dst, dst_x, dst_y, src->format, srcPixel);
                    }
                }
            }
            STACK_EXIT();
            return;
        }
        
        // Fast path 2: SRC_OVER with fully opaque source
        // When source is fully opaque (all pixels have alpha=255), SRC_OVER becomes a simple copy
        if (context->composite_mode == COMPOSITE_SRC_OVER && context->composite_alpha >= 1.0f) {
            // Check if all source pixels are opaque
            int all_opaque = 1;
            uint32_t alpha_mask = srcInfo->mask_a;
            uint32_t alpha_max = alpha_mask; // All alpha bits set = opaque
            
            // Quick scan: check a sampling of pixels to determine if source is likely all opaque
            // For large images, checking every pixel would be expensive, so we sample
            int sample_stride = (src->width > 32 || src->height > 32) ? 4 : 1;
            for (int sy = 0; sy < (int)src->height && all_opaque; sy += sample_stride) {
                for (int sx = 0; sx < (int)src->width && all_opaque; sx += sample_stride) {
                    uint32_t pixel = src_pixels[sy * src_stride + sx];
                    if ((pixel & alpha_mask) != alpha_max) {
                        all_opaque = 0;
                        break;
                    }
                }
            }
            
            // If source is fully opaque, use fast copy path
            if (all_opaque) {
                SetPixelFunc set_pixel_func = get_set_pixel_func(src->format, dst->format);
                
                if (set_pixel_func == set_pixel_same_format) {
                    // Same format: use memcpy for rows
                    uint32_t* dst_pixels = (uint32_t*)(uintptr_t)dst->ptr;
                    uint32_t  dst_stride = dst->stride / 4;
                    for (int dst_y = startY; dst_y < endY; ++dst_y) {
                        int src_y = dst_y - y;
                        uint32_t* src_row = &src_pixels[src_y * src_stride + (startX - x)];
                        uint32_t* dst_row = &dst_pixels[dst_y * dst_stride + startX];
                        size_t row_bytes = (size_t)(endX - startX) * sizeof(uint32_t);
                        memcpy(dst_row, src_row, row_bytes);
                    }
                } else {
                    // Different formats: direct set without blending
                    for (int dst_y = startY; dst_y < endY; ++dst_y) {
                        int src_y = dst_y - y;
                        for (int dst_x = startX; dst_x < endX; ++dst_x) {
                            int src_x = dst_x - x;
                            uint32_t srcPixel = src_pixels[src_y * src_stride + src_x];
                            set_pixel_func(dst, dst_x, dst_y, src->format, srcPixel);
                        }
                    }
                }
                STACK_EXIT();
                return;
            }
        }
        
        // Generic blending path with row-based processing for better cache locality
        uint32_t* dst_pixels = (uint32_t*)(uintptr_t)dst->ptr;
        uint32_t  dst_stride = dst->stride / 4;
        const PixelFormatInfo* dstInfo = &g_pixel_format_info[dst->format];
        
        for (int dst_y = startY; dst_y < endY; ++dst_y) {
            int src_y = dst_y - y;
            uint32_t* src_row = &src_pixels[src_y * src_stride];
            uint32_t* dst_row = &dst_pixels[dst_y * dst_stride];
            
            for (int dst_x = startX; dst_x < endX; ++dst_x) {
                int src_x = dst_x - x;
                uint32_t srcPixel = src_row[src_x];
                
                // Inline fast check for transparent pixels (common case to skip)
                uint8_t srcAlphaRaw = (srcPixel & srcInfo->mask_a) >> srcInfo->shift_a;
                if (srcAlphaRaw == 0 && context->composite_mode != COMPOSITE_CLEAR) {
                    // Fully transparent source has no effect in most modes
                    continue;
                }
                
                blend_pixel_composite(dst, dst_x, dst_y, src->format, srcPixel,
                                     context->composite_mode, context->composite_alpha);
            }
        }
        STACK_EXIT();
        return;
    }

    // Compute bounding box of transformed image in dest coords
    int tx, ty, tw, th;
    transform_rect(&context->transform, x, y, (int)src->width, (int)src->height,
                   &tx, &ty, &tw, &th);

    int startX = clip_x(tx, dst, context);
    int startY = clip_y(ty, dst, context);
    int endX   = clip_x(tx + tw, dst, context);
    int endY   = clip_y(ty + th, dst, context);

    if (startX >= endX || startY >= endY) {
        STACK_EXIT();
        return; // fully clipped
    }

    // Invert the transform
    Transform2D inv;
    if (!invert_transform(&context->transform, &inv)) {
        STACK_EXIT();
        return; // non-invertible, nothing to draw
    }

    // We will sample source per destination pixel
    int hasAlpha = (srcInfo->mask_a != 0);

    SetPixelFunc set_pixel_func = get_set_pixel_func(src->format, dst->format);

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
                // has alpha -> blend with composite mode
                blend_pixel_composite(dst, dx, dy, src->format, srcPixel,
                                     context->composite_mode, context->composite_alpha);
            }
        }
    }
    STACK_EXIT();
}

// Helper function to ensure edge table pool is initialized
static void ensure_edge_table_pool(void) {
    if (g_edge_table_pool == NULL) {
#if USE_FIXED_POINT_RASTERIZER
        g_edge_table_pool = edge_table_fixed_pool_create(EDGE_TABLE_POOL_INITIAL_CAPACITY);
        log_debug("Initialized global FIXED-POINT edge table pool with capacity=%d",
                  EDGE_TABLE_POOL_INITIAL_CAPACITY);
#else
        g_edge_table_pool = edge_table_pool_create(EDGE_TABLE_POOL_INITIAL_CAPACITY);
        log_debug("Initialized global edge table pool with capacity=%d", 
                  EDGE_TABLE_POOL_INITIAL_CAPACITY);
#endif
    }
}

// Fill polygon using edge table
void fill_polygon(SurfaceData* surface, SurfaceContext* context,
                 int* x_points, int* y_points, int n_points,
                 uint32_t color) {
    STACK_ENTER();
    
    if (!surface || !context || !x_points || !y_points || n_points < 3) {
        log_error("fill_polygon: invalid parameters");
        STACK_EXIT();
        return;
    }
    
    log_debug("fill_polygon: n_points=%d, color=0x%08X", n_points, color);
    
    // Apply transform to points if necessary
    int* transformed_x = x_points;
    int* transformed_y = y_points;
    int* allocated_x = NULL;
    int* allocated_y = NULL;
    
    if (!is_identity_transform(&context->transform)) {
        // Need to transform points - allocate temporary arrays
        allocated_x = (int*)tracked_malloc(sizeof(int) * n_points);
        allocated_y = (int*)tracked_malloc(sizeof(int) * n_points);
        
        if (!allocated_x || !allocated_y) {
            log_error("fill_polygon: failed to allocate transform arrays");
            if (allocated_x) tracked_free(allocated_x);
            if (allocated_y) tracked_free(allocated_y);
            STACK_EXIT();
            return;
        }
        
        // Transform all points
        for (int i = 0; i < n_points; i++) {
            float fx = (float)x_points[i];
            float fy = (float)y_points[i];
            float tx, ty;
            transform_point(&context->transform, fx, fy, &tx, &ty);
            allocated_x[i] = (int)(tx + 0.5f); // Round to nearest integer
            allocated_y[i] = (int)(ty + 0.5f);
        }
        
        transformed_x = allocated_x;
        transformed_y = allocated_y;
    }
    
    // Calculate bounding box using transformed points
    int min_x = transformed_x[0];
    int max_x = transformed_x[0];
    int min_y = transformed_y[0];
    int max_y = transformed_y[0];
    
    for (int i = 1; i < n_points; i++) {
        if (transformed_x[i] < min_x) min_x = transformed_x[i];
        if (transformed_x[i] > max_x) max_x = transformed_x[i];
        if (transformed_y[i] < min_y) min_y = transformed_y[i];
        if (transformed_y[i] > max_y) max_y = transformed_y[i];
    }
    
    // Clip bounding box to surface
    min_y = clamp_int(min_y, 0, (int)surface->height - 1);
    max_y = clamp_int(max_y, 0, (int)surface->height - 1);
    
    if (min_y >= max_y) {
        log_debug("fill_polygon: clipped out entirely");
        if (allocated_x) tracked_free(allocated_x);
        if (allocated_y) tracked_free(allocated_y);
        STACK_EXIT();
        return;
    }
    
    // Get edge table from pool
    ensure_edge_table_pool();
#if USE_FIXED_POINT_RASTERIZER
    EdgeTableFixed* et = edge_table_fixed_pool_acquire(g_edge_table_pool, min_y, max_y,
                                                        surface->width, surface->height);
    if (!et) {
        log_error("fill_polygon: failed to acquire fixed-point edge table");
        if (allocated_x) tracked_free(allocated_x);
        if (allocated_y) tracked_free(allocated_y);
        STACK_EXIT();
        return;
    }
    
    // Add all edges of the polygon using transformed points
    for (int i = 0; i < n_points; i++) {
        int next = (i + 1) % n_points;
        edge_table_fixed_add_line(et, transformed_x[i], transformed_y[i],
                                  transformed_x[next], transformed_y[next]);
    }
    
    // Fill using edge table with even-odd rule
    edge_table_fixed_fill(et, surface, context, color, FILL_RULE_EVENODD);
    
    // Return edge table to pool
    edge_table_fixed_pool_release(g_edge_table_pool, et);
#else
    EdgeTable* et = edge_table_pool_acquire(g_edge_table_pool, min_y, max_y,
                                            surface->width, surface->height);
    if (!et) {
        log_error("fill_polygon: failed to acquire edge table");
        if (allocated_x) tracked_free(allocated_x);
        if (allocated_y) tracked_free(allocated_y);
        STACK_EXIT();
        return;
    }
    
    // Add all edges of the polygon using transformed points
    for (int i = 0; i < n_points; i++) {
        int next = (i + 1) % n_points;
        edge_table_add_line(et, transformed_x[i], transformed_y[i], 
                           transformed_x[next], transformed_y[next]);
    }
    
    // Fill using edge table with even-odd rule
    edge_table_fill(et, surface, context, color, FILL_RULE_EVENODD);
    
    // Return edge table to pool
    edge_table_pool_release(g_edge_table_pool, et);
#endif
    
    // Free allocated transform arrays if any
    if (allocated_x) tracked_free(allocated_x);
    if (allocated_y) tracked_free(allocated_y);
    
    log_debug("fill_polygon: completed");
    STACK_EXIT();
}

// Fill oval using edge table
void fill_oval(SurfaceData* surface, SurfaceContext* context,
              int x, int y, int width, int height,
              uint32_t color) {
    STACK_ENTER();
    
    if (!surface || !context || width <= 0 || height <= 0) {
        log_error("fill_oval: invalid parameters");
        STACK_EXIT();
        return;
    }
    
    log_debug("fill_oval: x=%d, y=%d, w=%d, h=%d, color=0x%08X", 
             x, y, width, height, color);
    
    // Apply transform if necessary
    if (!is_identity_transform(&context->transform)) {
        float fx = (float)x;
        float fy = (float)y;
        float tx, ty;
        transform_point(&context->transform, fx, fy, &tx, &ty);
        x = (int)(tx + 0.5f);
        y = (int)(ty + 0.5f);
        // Note: For proper transform, width/height should also be transformed
        // but for translation-only (current case), this is sufficient
    }
    
    // Calculate center and radii
    int cx = x + width / 2;
    int cy = y + height / 2;
    int rx = width / 2;
    int ry = height / 2;
    
    // Calculate bounding box
    int min_y = clamp_int(y, 0, (int)surface->height - 1);
    int max_y = clamp_int(y + height, 0, (int)surface->height - 1);
    
    if (min_y >= max_y) {
        log_debug("fill_oval: clipped out entirely");
        STACK_EXIT();
        return;
    }
    
    // Get edge table from pool
    ensure_edge_table_pool();
#if USE_FIXED_POINT_RASTERIZER
    EdgeTableFixed* et = edge_table_fixed_pool_acquire(g_edge_table_pool, min_y, max_y,
                                                        surface->width, surface->height);
    if (!et) {
        log_error("fill_oval: failed to acquire fixed-point edge table");
        STACK_EXIT();
        return;
    }
    
    // Add arc for full ellipse (0 to 2*PI)
    edge_table_fixed_add_arc(et, cx, cy, rx, ry, 0.0, 2.0 * M_PI);
    
    // Fill using edge table
    edge_table_fixed_fill(et, surface, context, color, FILL_RULE_EVENODD);
    
    // Return edge table to pool
    edge_table_fixed_pool_release(g_edge_table_pool, et);
#else
    EdgeTable* et = edge_table_pool_acquire(g_edge_table_pool, min_y, max_y,
                                            surface->width, surface->height);
    if (!et) {
        log_error("fill_oval: failed to acquire edge table");
        STACK_EXIT();
        return;
    }
    
    // Add arc for full ellipse (0 to 2*PI)
    edge_table_add_arc(et, cx, cy, rx, ry, 0.0, 2.0 * M_PI);
    
    // Fill using edge table
    edge_table_fill(et, surface, context, color, FILL_RULE_EVENODD);
    
    // Return edge table to pool
    edge_table_pool_release(g_edge_table_pool, et);
#endif
    
    log_debug("fill_oval: completed");
    STACK_EXIT();
}

// Fill arc using edge table
void fill_arc(SurfaceData* surface, SurfaceContext* context,
             int x, int y, int width, int height,
             int start_angle, int arc_angle,
             uint32_t color) {
    STACK_ENTER();
    
    if (!surface || !context || width <= 0 || height <= 0) {
        log_error("fill_arc: invalid parameters");
        STACK_EXIT();
        return;
    }
    
    log_debug("fill_arc: x=%d, y=%d, w=%d, h=%d, start=%d, arc=%d, color=0x%08X",
             x, y, width, height, start_angle, arc_angle, color);
    
    // Apply transform if necessary
    if (!is_identity_transform(&context->transform)) {
        float fx = (float)x;
        float fy = (float)y;
        float tx, ty;
        transform_point(&context->transform, fx, fy, &tx, &ty);
        x = (int)(tx + 0.5f);
        y = (int)(ty + 0.5f);
    }
    
    // Calculate center and radii
    int cx = x + width / 2;
    int cy = y + height / 2;
    int rx = width / 2;
    int ry = height / 2;
    
    // Convert angles from degrees to radians
    // Java AWT uses degrees with 0 at 3 o'clock, positive = counter-clockwise
    double start_rad = -start_angle * M_PI / 180.0;
    double end_rad = -(start_angle + arc_angle) * M_PI / 180.0;
    
    // Normalize to standard angles (0 at 3 o'clock, positive = counter-clockwise)
    start_rad = -start_rad;
    end_rad = -end_rad;
    
    // Calculate bounding box
    int min_y = clamp_int(y, 0, (int)surface->height - 1);
    int max_y = clamp_int(y + height, 0, (int)surface->height - 1);
    
    if (min_y >= max_y) {
        log_debug("fill_arc: clipped out entirely");
        STACK_EXIT();
        return;
    }
    
    // Get edge table from pool
    ensure_edge_table_pool();
#if USE_FIXED_POINT_RASTERIZER
    EdgeTableFixed* et = edge_table_fixed_pool_acquire(g_edge_table_pool, min_y, max_y,
                                                        surface->width, surface->height);
    if (!et) {
        log_error("fill_arc: failed to acquire fixed-point edge table");
        STACK_EXIT();
        return;
    }
    
    // Add arc
    edge_table_fixed_add_arc(et, cx, cy, rx, ry, start_rad, end_rad);
    
    // Close the arc by adding lines from endpoints to center (pie slice)
    int start_x = cx + (int)(rx * cos(start_rad));
    int start_y = cy + (int)(ry * sin(start_rad));
    int end_x = cx + (int)(rx * cos(end_rad));
    int end_y = cy + (int)(ry * sin(end_rad));
    
    edge_table_fixed_add_line(et, end_x, end_y, cx, cy);
    edge_table_fixed_add_line(et, cx, cy, start_x, start_y);
    
    // Fill using edge table
    edge_table_fixed_fill(et, surface, context, color, FILL_RULE_EVENODD);
    
    // Return edge table to pool
    edge_table_fixed_pool_release(g_edge_table_pool, et);
#else
    EdgeTable* et = edge_table_pool_acquire(g_edge_table_pool, min_y, max_y,
                                            surface->width, surface->height);
    if (!et) {
        log_error("fill_arc: failed to acquire edge table");
        STACK_EXIT();
        return;
    }
    
    // Add arc
    edge_table_add_arc(et, cx, cy, rx, ry, start_rad, end_rad);
    
    // Close the arc by adding lines from endpoints to center (pie slice)
    int start_x = cx + (int)(rx * cos(start_rad));
    int start_y = cy + (int)(ry * sin(start_rad));
    int end_x = cx + (int)(rx * cos(end_rad));
    int end_y = cy + (int)(ry * sin(end_rad));
    
    edge_table_add_line(et, end_x, end_y, cx, cy);
    edge_table_add_line(et, cx, cy, start_x, start_y);
    
    // Fill using edge table
    edge_table_fill(et, surface, context, color, FILL_RULE_EVENODD);
    
    // Return edge table to pool
    edge_table_pool_release(g_edge_table_pool, et);
#endif
    
    log_debug("fill_arc: completed");
    STACK_EXIT();
}

// Fill rounded rectangle using edge table
void fill_round_rect(SurfaceData* surface, SurfaceContext* context,
                    int x, int y, int width, int height,
                    int arc_width, int arc_height,
                    uint32_t color) {
    STACK_ENTER();
    
    if (!surface || !context || width <= 0 || height <= 0) {
        log_error("fill_round_rect: invalid parameters");
        STACK_EXIT();
        return;
    }
    
    log_debug("fill_round_rect: x=%d, y=%d, w=%d, h=%d, arc_w=%d, arc_h=%d, color=0x%08X",
             x, y, width, height, arc_width, arc_height, color);
    
    // Apply transform if necessary
    if (!is_identity_transform(&context->transform)) {
        float fx = (float)x;
        float fy = (float)y;
        float tx, ty;
        transform_point(&context->transform, fx, fy, &tx, &ty);
        x = (int)(tx + 0.5f);
        y = (int)(ty + 0.5f);
    }
    
    // Clamp arc dimensions
    if (arc_width > width) arc_width = width;
    if (arc_height > height) arc_height = height;
    
    int rx = arc_width / 2;
    int ry = arc_height / 2;
    
    // Calculate bounding box
    int min_y = clamp_int(y, 0, (int)surface->height - 1);
    int max_y = clamp_int(y + height, 0, (int)surface->height - 1);
    
    if (min_y >= max_y) {
        log_debug("fill_round_rect: clipped out entirely");
        STACK_EXIT();
        return;
    }
    
    // Get edge table from pool
    ensure_edge_table_pool();
#if USE_FIXED_POINT_RASTERIZER
    EdgeTableFixed* et = edge_table_fixed_pool_acquire(g_edge_table_pool, min_y, max_y,
                                                        surface->width, surface->height);
    if (!et) {
        log_error("fill_round_rect: failed to acquire fixed-point edge table");
        STACK_EXIT();
        return;
    }
    
    // Add four corner arcs and four straight edges
    // Top edge
    edge_table_fixed_add_line(et, x + rx, y, x + width - rx, y);
    
    // Top-right corner arc (0 to 90 degrees, or 0 to PI/2 radians)
    edge_table_fixed_add_arc(et, x + width - rx, y + ry, rx, ry,
                             -M_PI / 2.0, 0.0);
    
    // Right edge
    edge_table_fixed_add_line(et, x + width, y + ry, x + width, y + height - ry);
    
    // Bottom-right corner arc (90 to 180 degrees, or PI/2 to PI radians)
    edge_table_fixed_add_arc(et, x + width - rx, y + height - ry, rx, ry,
                             0.0, M_PI / 2.0);
    
    // Bottom edge
    edge_table_fixed_add_line(et, x + width - rx, y + height, x + rx, y + height);
    
    // Bottom-left corner arc (180 to 270 degrees, or PI to 3*PI/2 radians)
    edge_table_fixed_add_arc(et, x + rx, y + height - ry, rx, ry,
                             M_PI / 2.0, M_PI);
    
    // Left edge
    edge_table_fixed_add_line(et, x, y + height - ry, x, y + ry);
    
    // Top-left corner arc (270 to 360 degrees, or 3*PI/2 to 2*PI radians)
    edge_table_fixed_add_arc(et, x + rx, y + ry, rx, ry,
                             M_PI, 3.0 * M_PI / 2.0);
    
    // Fill using edge table
    edge_table_fixed_fill(et, surface, context, color, FILL_RULE_EVENODD);
    
    // Return edge table to pool
    edge_table_fixed_pool_release(g_edge_table_pool, et);
#else
    EdgeTable* et = edge_table_pool_acquire(g_edge_table_pool, min_y, max_y,
                                            surface->width, surface->height);
    if (!et) {
        log_error("fill_round_rect: failed to acquire edge table");
        STACK_EXIT();
        return;
    }
    
    // Add four corner arcs and four straight edges
    // Top edge
    edge_table_add_line(et, x + rx, y, x + width - rx, y);
    
    // Top-right corner arc (0 to 90 degrees, or 0 to PI/2 radians)
    edge_table_add_arc(et, x + width - rx, y + ry, rx, ry, 
                      -M_PI / 2.0, 0.0);
    
    // Right edge
    edge_table_add_line(et, x + width, y + ry, x + width, y + height - ry);
    
    // Bottom-right corner arc (90 to 180 degrees, or PI/2 to PI radians)
    edge_table_add_arc(et, x + width - rx, y + height - ry, rx, ry,
                      0.0, M_PI / 2.0);
    
    // Bottom edge
    edge_table_add_line(et, x + width - rx, y + height, x + rx, y + height);
    
    // Bottom-left corner arc (180 to 270 degrees, or PI to 3*PI/2 radians)
    edge_table_add_arc(et, x + rx, y + height - ry, rx, ry,
                      M_PI / 2.0, M_PI);
    
    // Left edge
    edge_table_add_line(et, x, y + height - ry, x, y + ry);
    
    // Top-left corner arc (270 to 360 degrees, or 3*PI/2 to 2*PI radians)
    edge_table_add_arc(et, x + rx, y + ry, rx, ry,
                      M_PI, 3.0 * M_PI / 2.0);
    
    // Fill using edge table
    edge_table_fill(et, surface, context, color, FILL_RULE_EVENODD);
    
    // Return edge table to pool
    edge_table_pool_release(g_edge_table_pool, et);
#endif
    
    log_debug("fill_round_rect: completed");
    STACK_EXIT();
}

// Fill rectangle using edge table (supports transforms)
void fill_rect(SurfaceData* surface, SurfaceContext* context,
              int x, int y, int width, int height,
              uint32_t color) {
    STACK_ENTER();
    
    if (!surface || !context || width <= 0 || height <= 0) {
        log_error("fill_rect: invalid parameters");
        STACK_EXIT();
        return;
    }
    
    log_debug("fill_rect: x=%d, y=%d, w=%d, h=%d, color=0x%08X",
             x, y, width, height, color);
    
    // In AWT, fillRect(x, y, width, height) fills pixels from (x,y) to (x+width-1, y+height-1) inclusive
    // For the polygon, we need corners that, when rasterized, produce this result
    // The polygon corners are: (x, y), (x+width, y), (x+width, y+height), (x, y+height)
    // When rasterized, scanlines from y to y+height-1 will be filled from x to x+width-1
    
    int x_points[4];
    int y_points[4];
    
    // Apply transform if necessary
    if (!is_identity_transform(&context->transform)) {
        // Transform all four corners
        // Use x+width and y+height (exclusive upper bounds) for correct polygon
        float corners_x[4] = {(float)x, (float)(x + width), (float)(x + width), (float)x};
        float corners_y[4] = {(float)y, (float)y, (float)(y + height), (float)(y + height)};
        
        for (int i = 0; i < 4; i++) {
            float tx, ty;
            transform_point(&context->transform, corners_x[i], corners_y[i], &tx, &ty);
            x_points[i] = (int)(tx + 0.5f);
            y_points[i] = (int)(ty + 0.5f);
        }
    } else {
        // No transform - simple rectangle corners
        x_points[0] = x;
        y_points[0] = y;
        x_points[1] = x + width;
        y_points[1] = y;
        x_points[2] = x + width;
        y_points[2] = y + height;
        x_points[3] = x;
        y_points[3] = y + height;
    }
    
    // Calculate bounding box
    int min_x = x_points[0];
    int max_x = x_points[0];
    int min_y = y_points[0];
    int max_y = y_points[0];
    
    for (int i = 1; i < 4; i++) {
        if (x_points[i] < min_x) min_x = x_points[i];
        if (x_points[i] > max_x) max_x = x_points[i];
        if (y_points[i] < min_y) min_y = y_points[i];
        if (y_points[i] > max_y) max_y = y_points[i];
    }
    
    // Clamp min_y but DON'T clamp max_y yet - the edge table needs the full range
    // to properly include all edges
    if (min_y < 0) min_y = 0;
    if (min_y >= (int)surface->height) {
        log_debug("fill_rect: clipped out entirely (min_y >= height)");
        STACK_EXIT();
        return;
    }
    
    // For edge table, we need max_y to include the last scanline that has pixels
    // If max_y is the exclusive upper bound, the last scanline with pixels is max_y-1
    // But edge table expects inclusive range, so we use max_y-1 as the max
    int edge_table_max_y = max_y - 1;
    if (edge_table_max_y > (int)surface->height - 1) {
        edge_table_max_y = (int)surface->height - 1;
    }
    
    if (min_y > edge_table_max_y) {
        log_debug("fill_rect: clipped out entirely (min_y > max_y)");
        STACK_EXIT();
        return;
    }
    
    // Get edge table from pool
    ensure_edge_table_pool();
#if USE_FIXED_POINT_RASTERIZER
    EdgeTableFixed* et = edge_table_fixed_pool_acquire(g_edge_table_pool, min_y, edge_table_max_y,
                                                        surface->width, surface->height);
    if (!et) {
        log_error("fill_rect: failed to acquire fixed-point edge table");
        STACK_EXIT();
        return;
    }
    
    // Add all four edges of the rectangle
    for (int i = 0; i < 4; i++) {
        int next = (i + 1) % 4;
        edge_table_fixed_add_line(et, x_points[i], y_points[i],
                                  x_points[next], y_points[next]);
    }
    
    // Fill using edge table
    edge_table_fixed_fill(et, surface, context, color, FILL_RULE_EVENODD);
    
    // Return edge table to pool
    edge_table_fixed_pool_release(g_edge_table_pool, et);
#else
    EdgeTable* et = edge_table_pool_acquire(g_edge_table_pool, min_y, edge_table_max_y,
                                            surface->width, surface->height);
    if (!et) {
        log_error("fill_rect: failed to acquire edge table");
        STACK_EXIT();
        return;
    }
    
    // Add all four edges of the rectangle
    for (int i = 0; i < 4; i++) {
        int next = (i + 1) % 4;
        edge_table_add_line(et, x_points[i], y_points[i], 
                           x_points[next], y_points[next]);
    }
    
    // Fill using edge table
    edge_table_fill(et, surface, context, color, FILL_RULE_EVENODD);
    
    // Return edge table to pool
    edge_table_pool_release(g_edge_table_pool, et);
#endif
    
    log_debug("fill_rect: completed");
    STACK_EXIT();
}

// Helper function to plot a single pixel with clipping and blending
static void plot_pixel(SurfaceData* surface, SurfaceContext* context,
                      int x, int y, uint32_t color) {
    // Clip to surface bounds
    if (x < 0 || x >= (int)surface->width || y < 0 || y >= (int)surface->height) {
        return;
    }
    
    // Apply clip rectangle if set
    if (context->clip.width >= 0 && context->clip.height >= 0) {
        if (x < context->clip.x || x >= context->clip.x + context->clip.width ||
            y < context->clip.y || y >= context->clip.y + context->clip.height) {
            return;
        }
    }
    
    // Set pixel with appropriate blending
    blend_pixel_composite(surface, x, y, PIXEL_FORMAT_ARGB, color,
                         context->composite_mode, context->composite_alpha);
}

// Helper function to draw an ellipse outline using midpoint algorithm
static void draw_ellipse(SurfaceData* surface, SurfaceContext* context,
                        int cx, int cy, int rx, int ry, uint32_t color) {
    if (rx <= 0 || ry <= 0) return;
    
    int x = 0;
    int y = ry;
    int rx2 = rx * rx;
    int ry2 = ry * ry;
    int twoRx2 = 2 * rx2;
    int twoRy2 = 2 * ry2;
    
    // Region 1
    int p1 = (int)(ry2 - rx2 * ry + 0.25 * rx2);
    int px = 0;
    int py = twoRx2 * y;
    
    while (px < py) {
        // Plot 4 symmetric points
        plot_pixel(surface, context, cx + x, cy + y, color);
        plot_pixel(surface, context, cx - x, cy + y, color);
        plot_pixel(surface, context, cx + x, cy - y, color);
        plot_pixel(surface, context, cx - x, cy - y, color);
        
        x++;
        px += twoRy2;
        if (p1 < 0) {
            p1 += ry2 + px;
        } else {
            y--;
            py -= twoRx2;
            p1 += ry2 + px - py;
        }
    }
    
    // Region 2
    int p2 = (int)(ry2 * (x + 0.5) * (x + 0.5) + rx2 * (y - 1) * (y - 1) - rx2 * ry2);
    while (y >= 0) {
        // Plot 4 symmetric points
        plot_pixel(surface, context, cx + x, cy + y, color);
        plot_pixel(surface, context, cx - x, cy + y, color);
        plot_pixel(surface, context, cx + x, cy - y, color);
        plot_pixel(surface, context, cx - x, cy - y, color);
        
        y--;
        py -= twoRx2;
        if (p2 > 0) {
            p2 += rx2 - py;
        } else {
            x++;
            px += twoRy2;
            p2 += rx2 - py + px;
        }
    }
}

// Helper function to draw an arc segment using parametric equations
static void draw_arc_segment(SurfaceData* surface, SurfaceContext* context,
                            int cx, int cy, int rx, int ry,
                            double start_angle, double end_angle, uint32_t color) {
    // Normalize angles
    while (start_angle < 0) start_angle += 2 * M_PI;
    while (end_angle < 0) end_angle += 2 * M_PI;
    while (start_angle > 2 * M_PI) start_angle -= 2 * M_PI;
    while (end_angle > 2 * M_PI) end_angle -= 2 * M_PI;
    
    // Handle wrapping
    if (end_angle < start_angle) {
        end_angle += 2 * M_PI;
    }
    
    // Calculate step size based on arc length
    double angle_diff = fabs(end_angle - start_angle);
    double arc_length = angle_diff * (rx > ry ? rx : ry);
    int steps = (int)fmax(10, ceil(arc_length / 2.0)); // At least 10 steps
    
    double angle_step = (end_angle - start_angle) / steps;
    int prev_x = cx + (int)round(rx * cos(start_angle));
    int prev_y = cy + (int)round(ry * sin(start_angle));
    
    for (int i = 1; i <= steps; i++) {
        double angle = start_angle + i * angle_step;
        int x = cx + (int)round(rx * cos(angle));
        int y = cy + (int)round(ry * sin(angle));
        draw_line(surface, context, prev_x, prev_y, x, y, color);
        prev_x = x;
        prev_y = y;
    }
}

void draw_oval(SurfaceData* surface, SurfaceContext* context,
              int x, int y, int width, int height,
              uint32_t color) {
    STACK_ENTER();
    
    if (width <= 0 || height <= 0) {
        log_error("draw_oval: invalid dimensions");
        STACK_EXIT();
        return;
    }
    
    log_debug("draw_oval: x=%d, y=%d, w=%d, h=%d", x, y, width, height);
    
    // Calculate center and radii
    int cx = x + width / 2;
    int cy = y + height / 2;
    int rx = width / 2;
    int ry = height / 2;
    
    // Apply transform
    if (!is_identity_transform(&context->transform)) {
        float fx = cx, fy = cy;
        transform_point(&context->transform, fx, fy, &fx, &fy);
        cx = (int)roundf(fx);
        cy = (int)roundf(fy);
    }
    
    // Draw ellipse outline
    draw_ellipse(surface, context, cx, cy, rx, ry, color);
    
    log_debug("draw_oval: completed");
    STACK_EXIT();
}

void draw_arc(SurfaceData* surface, SurfaceContext* context,
             int x, int y, int width, int height,
             int start_angle, int arc_angle,
             uint32_t color) {
    STACK_ENTER();
    
    if (width <= 0 || height <= 0) {
        log_error("draw_arc: invalid dimensions");
        STACK_EXIT();
        return;
    }
    
    log_debug("draw_arc: x=%d, y=%d, w=%d, h=%d, start=%d, arc=%d",
             x, y, width, height, start_angle, arc_angle);
    
    // Calculate center and radii
    int cx = x + width / 2;
    int cy = y + height / 2;
    int rx = width / 2;
    int ry = height / 2;
    
    // Don't apply transform here - draw_line will handle it
    
    // Convert angles from degrees to radians
    double start_rad = start_angle * M_PI / 180.0;
    double end_rad = (start_angle + arc_angle) * M_PI / 180.0;
    
    // Draw arc segment
    draw_arc_segment(surface, context, cx, cy, rx, ry, start_rad, end_rad, color);
    
    log_debug("draw_arc: completed");
    STACK_EXIT();
}

void draw_round_rect(SurfaceData* surface, SurfaceContext* context,
                    int x, int y, int width, int height,
                    int arc_width, int arc_height,
                    uint32_t color) {
    STACK_ENTER();
    
    if (width <= 0 || height <= 0) {
        log_error("draw_round_rect: invalid dimensions");
        STACK_EXIT();
        return;
    }
    
    log_debug("draw_round_rect: x=%d, y=%d, w=%d, h=%d, arcW=%d, arcH=%d",
             x, y, width, height, arc_width, arc_height);
    
    // Clamp arc dimensions
    if (arc_width > width) arc_width = width;
    if (arc_height > height) arc_height = height;
    
    int rx = arc_width / 2;
    int ry = arc_height / 2;
    
    // Don't apply transform here - draw_line and draw_arc_segment will handle it
    
    // Draw four edges
    draw_line(surface, context, x + rx, y, x + width - rx, y, color); // Top
    draw_line(surface, context, x + width, y + ry, x + width, y + height - ry, color); // Right
    draw_line(surface, context, x + width - rx, y + height, x + rx, y + height, color); // Bottom
    draw_line(surface, context, x, y + height - ry, x, y + ry, color); // Left
    
    // Draw four corner arcs
    draw_arc_segment(surface, context, x + width - rx, y + ry, rx, ry, 
                    -M_PI / 2.0, 0.0, color); // Top-right
    draw_arc_segment(surface, context, x + width - rx, y + height - ry, rx, ry,
                    0.0, M_PI / 2.0, color); // Bottom-right
    draw_arc_segment(surface, context, x + rx, y + height - ry, rx, ry,
                    M_PI / 2.0, M_PI, color); // Bottom-left
    draw_arc_segment(surface, context, x + rx, y + ry, rx, ry,
                    M_PI, 3.0 * M_PI / 2.0, color); // Top-left
    
    log_debug("draw_round_rect: completed");
    STACK_EXIT();
}

void draw_polyline(SurfaceData* surface, SurfaceContext* context,
                  int* x_points, int* y_points, int n_points,
                  uint32_t color) {
    STACK_ENTER();
    
    if (!x_points || !y_points || n_points < 2) {
        log_error("draw_polyline: invalid parameters");
        STACK_EXIT();
        return;
    }
    
    log_debug("draw_polyline: npoints=%d", n_points);
    
    // Draw lines connecting consecutive points (but don't close the polygon)
    for (int i = 1; i < n_points; i++) {
        draw_line(surface, context, x_points[i - 1], y_points[i - 1],
                 x_points[i], y_points[i], color);
    }
    
    log_debug("draw_polyline: completed");
    STACK_EXIT();
}

void copy_area(SurfaceData* surface, SurfaceContext* context,
              int x, int y, int width, int height,
              int dx, int dy) {
    STACK_ENTER();
    
    if (width <= 0 || height <= 0) {
        log_error("copy_area: invalid dimensions");
        STACK_EXIT();
        return;
    }
    
    log_debug("copy_area: x=%d, y=%d, w=%d, h=%d, dx=%d, dy=%d",
             x, y, width, height, dx, dy);
    
    // Apply transform to source coordinates
    int src_x = x;
    int src_y = y;
    if (!is_identity_transform(&context->transform)) {
        float fx = x, fy = y;
        transform_point(&context->transform, fx, fy, &fx, &fy);
        src_x = (int)roundf(fx);
        src_y = (int)roundf(fy);
    }
    
    // Destination is source + delta (in device space)
    int dst_x = src_x + dx;
    int dst_y = src_y + dy;
    
    // Clip source and destination to surface bounds
    int surf_width = surface->width;
    int surf_height = surface->height;
    
    int copy_width = width;
    int copy_height = height;
    
    // Clip source rectangle
    if (src_x < 0) {
        copy_width += src_x;
        dst_x -= src_x;
        src_x = 0;
    }
    if (src_y < 0) {
        copy_height += src_y;
        dst_y -= src_y;
        src_y = 0;
    }
    if (src_x + copy_width > surf_width) {
        copy_width = surf_width - src_x;
    }
    if (src_y + copy_height > surf_height) {
        copy_height = surf_height - src_y;
    }
    
    // Clip destination rectangle
    if (dst_x < 0) {
        copy_width += dst_x;
        src_x -= dst_x;
        dst_x = 0;
    }
    if (dst_y < 0) {
        copy_height += dst_y;
        src_y -= dst_y;
        dst_y = 0;
    }
    if (dst_x + copy_width > surf_width) {
        copy_width = surf_width - dst_x;
    }
    if (dst_y + copy_height > surf_height) {
        copy_height = surf_height - dst_y;
    }
    
    // Apply clip rectangle to destination
    if (context->clip.width >= 0 && context->clip.height >= 0) {
        int clip_right = context->clip.x + context->clip.width;
        int clip_bottom = context->clip.y + context->clip.height;
        
        if (dst_x < context->clip.x) {
            int diff = context->clip.x - dst_x;
            src_x += diff;
            copy_width -= diff;
            dst_x = context->clip.x;
        }
        if (dst_y < context->clip.y) {
            int diff = context->clip.y - dst_y;
            src_y += diff;
            copy_height -= diff;
            dst_y = context->clip.y;
        }
        if (dst_x + copy_width > clip_right) {
            copy_width = clip_right - dst_x;
        }
        if (dst_y + copy_height > clip_bottom) {
            copy_height = clip_bottom - dst_y;
        }
    }
    
    if (copy_width <= 0 || copy_height <= 0) {
        log_debug("copy_area: clipped out entirely");
        STACK_EXIT();
        return;
    }
    
    // Get pixel data
    uint32_t* pixels = (uint32_t*)(uintptr_t)surface->ptr;
    if (!pixels) {
        log_error("copy_area: null pixel data");
        STACK_EXIT();
        return;
    }
    
    // Copy pixels - need to handle overlapping regions
    int overlaps = !((dst_x + copy_width <= src_x) || (dst_x >= src_x + copy_width) ||
                     (dst_y + copy_height <= src_y) || (dst_y >= src_y + copy_height));
    
    if (overlaps && (dy > 0 || (dy == 0 && dx > 0))) {
        // Copy from bottom-right to top-left
        for (int row = copy_height - 1; row >= 0; row--) {
            for (int col = copy_width - 1; col >= 0; col--) {
                int src_idx = (src_y + row) * surf_width + (src_x + col);
                int dst_idx = (dst_y + row) * surf_width + (dst_x + col);
                pixels[dst_idx] = pixels[src_idx];
            }
        }
    } else {
        // Copy from top-left to bottom-right (normal case)
        for (int row = 0; row < copy_height; row++) {
            for (int col = 0; col < copy_width; col++) {
                int src_idx = (src_y + row) * surf_width + (src_x + col);
                int dst_idx = (dst_y + row) * surf_width + (dst_x + col);
                pixels[dst_idx] = pixels[src_idx];
            }
        }
    }
    
    log_debug("copy_area: completed");
    STACK_EXIT();
}

// Edge table pool lifecycle management functions

// Cleanup function for global edge table pool
// Should be called during module unload or application shutdown
void cleanup_edge_table_pool(void) {
    if (g_edge_table_pool != NULL) {
#if USE_FIXED_POINT_RASTERIZER
        log_info("Cleaning up global FIXED-POINT edge table pool (count=%d)",
                 g_edge_table_pool->count);
        edge_table_fixed_pool_destroy(g_edge_table_pool);
#else
        log_info("Cleaning up global edge table pool (count=%d)", 
                 g_edge_table_pool->count);
        edge_table_pool_destroy(g_edge_table_pool);
#endif
        g_edge_table_pool = NULL;
    }
}

// Export for WASM: Get current pool size (for debugging/monitoring)
__attribute__((export_name("get_edge_table_pool_size")))
int get_edge_table_pool_size(void) {
    return (g_edge_table_pool != NULL) ? g_edge_table_pool->count : 0;
}
