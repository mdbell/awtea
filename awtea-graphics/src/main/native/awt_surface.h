#pragma once
#include "awt_raster_internal.h"

extern Surface g_surfaces[NUM_SURFACES];


Surface* get_surface_data(int id);

__attribute__((export_name("find_free_surface")))
int  find_free_surface(void);

__attribute__((export_name("reset_surface")))
int  reset_surface(int surface_id, int layer, int width, int height, PixelFormat format);

__attribute__((export_name("get_surface_width")))
int  get_surface_width(int surface_id);

__attribute__((export_name("get_surface_height")))
int  get_surface_height(int surface_id);

__attribute__((export_name("get_surface_stride")))
int  get_surface_stride(int surface_id);

__attribute__((export_name("get_surface_pixels_ptr")))
uint32_t get_surface_pixels_ptr(int surface_id);


int clip_x(int x, const Surface* surf);
int clip_y(int y, const Surface* surf);