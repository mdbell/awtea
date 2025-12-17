/**
 * AUTO-GENERATED FILE - DO NOT EDIT
 * Generated from: schemas/surface-operation.yaml
 * 
 * Operations that can be performed on a surface
 */
#pragma once

#include <stdint.h>

typedef enum {
    // No operation
    CMD_NO_OP = 0,

    // Set foreground or background color
    CMD_SET_COLOR,
    // Set affine transformation matrix
    CMD_SET_TRANSFORM,
    // Set clipping rectangle
    CMD_SET_CLIP_RECT,
    // Set compositing mode (Porter-Duff alpha blending)
    CMD_SET_COMPOSITE,
    // Copy image to surface
    CMD_BLIT_IMAGE,
    // Draw rectangle outline
    CMD_DRAW_RECT,
    // Fill rectangle
    CMD_FILL_RECT,
    // Clear rectangle
    CMD_CLEAR_RECT,
    // Draw line
    CMD_DRAW_LINE,
    // Draw a polygon
    CMD_DRAW_POLYGON,
    // Fills a polygon
    CMD_FILL_POLYGON,
    // Fills a rounded rectangle
    CMD_FILL_ROUND_RECT,
    // Fills an arc
    CMD_FILL_ARC,
    // Number of standard commands
    CMD_COUNT,
} SurfaceOperation;
