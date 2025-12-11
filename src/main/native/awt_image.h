#pragma once
#include "awt_raster_internal.h"

extern ImageView g_images[MAX_IMAGES];

ImageView* get_image_data(int id);

__attribute__((export_name("register_image")))
void register_image(int id, uint32_t ptr, int format, int width, int height, int stride);

__attribute__((export_name("alloc_pixels")))
uint32_t alloc_pixels(int width, int height);

__attribute__((export_name("free_pixels")))
void free_pixels(uint32_t ptr);