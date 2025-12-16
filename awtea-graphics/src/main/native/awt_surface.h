#pragma once
#include "awt_raster_internal.h"

extern SurfaceData g_surfaces[NUM_SURFACES];
extern SurfaceContext g_contexts[NUM_CONTEXTS];

SurfaceData* get_surface_data(int id);
SurfaceContext* get_context_data(int id);

__attribute__((export_name("init_surface_system")))
void init_surface_system(void);

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

// Context management functions
__attribute__((export_name("find_free_context")))
int find_free_context(void);

__attribute__((export_name("create_context")))
int create_context(int surface_id);

__attribute__((export_name("clone_context")))
int clone_context(int context_id);

__attribute__((export_name("destroy_context")))
int destroy_context(int context_id);

__attribute__((export_name("create_reference")))
int create_reference(int surface_id);

__attribute__((export_name("release_reference")))
int release_reference(int surface_id);

__attribute__((export_name("get_context_surface_id")))
int get_context_surface_id(int context_id);

__attribute__((export_name("get_context_buffer_size_words")))
int get_context_buffer_size_words(void);

__attribute__((export_name("get_context_buffer_ptr")))
uint32_t get_context_buffer_ptr(int context_id);

int clip_x(int x, const SurfaceData* surf, SurfaceContext* context);
int clip_y(int y, const SurfaceData* surf, SurfaceContext* context);