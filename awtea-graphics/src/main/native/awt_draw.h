#pragma once
#include "awt_raster_internal.h"

void draw_filled_rect(SurfaceData* surface, SurfaceContext* context,
                        int x, int y,
                        int width, int height,
                        uint32_t color);

void clear_rect(SurfaceData* surface, SurfaceContext* context,
                int x, int y,
                int width, int height);

void draw_rect(SurfaceData* surface, SurfaceContext* context,
                int x, int y,
                int width, int height,
                uint32_t color);

void set_color(SurfaceContext* ctx, int which, uint32_t argb);

void draw_line(SurfaceData* surf, SurfaceContext* ctx,
                int x1, int y1,
                int x2, int y2,
                uint32_t color);

void blit_image(SurfaceData* dst, SurfaceContext* context,
                int src_surface_id,
                int x, int y);

// Edge table based fill functions
void fill_polygon(SurfaceData* surface, SurfaceContext* context,
                 int* x_points, int* y_points, int n_points,
                 uint32_t color);

void fill_oval(SurfaceData* surface, SurfaceContext* context,
              int x, int y, int width, int height,
              uint32_t color);

void fill_arc(SurfaceData* surface, SurfaceContext* context,
             int x, int y, int width, int height,
             int start_angle, int arc_angle,
             uint32_t color);

void fill_round_rect(SurfaceData* surface, SurfaceContext* context,
                    int x, int y, int width, int height,
                    int arc_width, int arc_height,
                    uint32_t color);

void fill_rect(SurfaceData* surface, SurfaceContext* context,
              int x, int y, int width, int height,
              uint32_t color);
// Draw outline functions
void draw_oval(SurfaceData* surface, SurfaceContext* context,
              int x, int y, int width, int height,
              uint32_t color);

void draw_arc(SurfaceData* surface, SurfaceContext* context,
             int x, int y, int width, int height,
             int start_angle, int arc_angle,
             uint32_t color);

void draw_round_rect(SurfaceData* surface, SurfaceContext* context,
                    int x, int y, int width, int height,
                    int arc_width, int arc_height,
                    uint32_t color);

void draw_polyline(SurfaceData* surface, SurfaceContext* context,
                  int* x_points, int* y_points, int n_points,
                  uint32_t color);

void copy_area(SurfaceData* surface, SurfaceContext* context,
              int x, int y, int width, int height,
              int dx, int dy);

// Edge table pool lifecycle management
// Cleanup function for global edge table pool (call during shutdown/module unload)
void cleanup_edge_table_pool(void);

// Debug/stats export for pool state (returns number of tables in pool)
int get_edge_table_pool_size(void);
