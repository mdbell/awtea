#include "awt_draw.h"
#include "awt_surface.h"
#include "awt_transform.h"
#include "awt_pixel.h"
#include "awt_util.h"
#include "awt_log.h"
#include "awt_stack.h"
#include "awt_edge_table.h"
#include "awt_memory.h"

// Global edge table pool (lazy initialization)
static EdgeTablePool* g_edge_table_pool = NULL;

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
            SetPixelFunc set_pixel_func =
                get_set_pixel_func(PIXEL_FORMAT_ARGB, surface->format);

            uint32_t* framebuffer = (uint32_t*)(uintptr_t)surface->ptr;
            uint32_t stride = surface->stride / 4;

            for (int j = y0; j < y1; j++) {
                for (int i = x0; i < x1; i++) {
                    set_pixel_func(surface, i, j, PIXEL_FORMAT_ARGB, color);
                }
            }
            log_debug("draw_filled_rect: wrote %d pixels", (x1-x0)*(y1-y0));
            STACK_EXIT();
            return;
        }

        // destination has alpha -> use composite blending
        // Check if we can skip blending for certain modes
        int needsBlending = (context->composite_mode != COMPOSITE_SRC) && 
                           (context->composite_mode != COMPOSITE_DST) &&
                           (context->composite_mode != COMPOSITE_CLEAR);
        
        if (!needsBlending) {
            // Fast path for SRC/DST/CLEAR modes
            if (context->composite_mode == COMPOSITE_SRC) {
                SetPixelFunc set_pixel_func =
                    get_set_pixel_func(PIXEL_FORMAT_ARGB, surface->format);
                for (int j = y0; j < y1; j++) {
                    for (int i = x0; i < x1; i++) {
                        set_pixel_func(surface, i, j, PIXEL_FORMAT_ARGB, color);
                    }
                }
            } else if (context->composite_mode == COMPOSITE_CLEAR) {
                // For CLEAR mode, write transparent pixels
                uint32_t clearColor = 0x00000000;
                SetPixelFunc set_pixel_func =
                    get_set_pixel_func(PIXEL_FORMAT_ARGB, surface->format);
                for (int j = y0; j < y1; j++) {
                    for (int i = x0; i < x1; i++) {
                        set_pixel_func(surface, i, j, PIXEL_FORMAT_ARGB, clearColor);
                    }
                }
            }
            // For DST mode, don't draw anything
            log_debug("draw_filled_rect: used fast path for mode %d", context->composite_mode);
            return;
        }
        
        // Normal blending path
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

        // Source *has* alpha → blend with composite mode
        for (int dst_y = startY; dst_y < endY; ++dst_y) {
            int src_y = dst_y - y;
            for (int dst_x = startX; dst_x < endX; ++dst_x) {
                int src_x = dst_x - x;
                uint32_t srcPixel = src_pixels[src_y * src_stride + src_x];
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
        g_edge_table_pool = edge_table_pool_create(4);
        log_debug("Initialized global edge table pool");
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
    
    // Create array of 4 corner points
    int x_points[4];
    int y_points[4];
    
    // Apply transform if necessary
    if (!is_identity_transform(&context->transform)) {
        // Transform all four corners
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
    
    // Clip bounding box to surface
    min_y = clamp_int(min_y, 0, (int)surface->height - 1);
    max_y = clamp_int(max_y, 0, (int)surface->height - 1);
    
    if (min_y >= max_y) {
        log_debug("fill_rect: clipped out entirely");
        STACK_EXIT();
        return;
    }
    
    // Get edge table from pool
    ensure_edge_table_pool();
    EdgeTable* et = edge_table_pool_acquire(g_edge_table_pool, min_y, max_y,
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
    
    log_debug("fill_rect: completed");
    STACK_EXIT();
}
