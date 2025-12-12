#pragma once
#include "awt_raster_internal.h"

extern ImageView g_images[MAX_IMAGES];

ImageView* get_image_data(int id);

uint32_t alloc_pixels(int width, int height);

__attribute__((export_name("register_image")))
int register_image(int format, int width, int height, int stride);


int free_image(int id);

__attribute__((export_name("get_image_pixels_ptr")))
uint32_t get_image_pixels_ptr(int id);

__attribute__((export_name("free_pixels")))
void free_pixels(uint32_t ptr);
