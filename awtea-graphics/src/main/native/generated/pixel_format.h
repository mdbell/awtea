/**
 * AUTO-GENERATED FILE - DO NOT EDIT
 * Generated from: schemas/pixel-format.yaml
 * 
 * Pixel format types for surfaces and images
 */
#pragma once

#include <stdint.h>

typedef enum {
    // Alpha-Red-Green-Blue format
    PIXEL_FORMAT_ARGB = 0,

    // Red-Green-Blue format (no alpha)
    PIXEL_FORMAT_RGB,
    // Red-Green-Blue-Alpha format
    PIXEL_FORMAT_RGBA,
    // Alpha-Blue-Green-Red format
    PIXEL_FORMAT_ABGR,
    // Blue-Green-Red format (no alpha)
    PIXEL_FORMAT_BGR,
    // Number of pixel formats
    PIXEL_FORMAT_COUNT,
} PixelFormat;
