#pragma once
#include "awt_raster_internal.h"

uint32_t alloc_pixels(int width, int height);

__attribute__((export_name("free_pixels")))
void free_pixels(uint32_t ptr);
