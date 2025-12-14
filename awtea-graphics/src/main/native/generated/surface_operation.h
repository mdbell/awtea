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
    // Set compositing mode (unimplemented in WASM)
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
    // Number of standard commands
    CMD_COUNT,
} SurfaceOperation;
