#pragma once
#include <math.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>

// Include auto-generated enums
#include "generated/surface_operation.h"
#include "generated/pixel_format.h"
#include "awt_command_reader.h"

#define NUM_SURFACES 1024
#define NUM_CONTEXTS 2048

#define START_SURFACE_ID 0
#define END_SURFACE_ID (START_SURFACE_ID + NUM_SURFACES)

#define START_CONTEXT_ID (START_SURFACE_ID + NUM_SURFACES)
#define END_CONTEXT_ID (START_CONTEXT_ID + NUM_CONTEXTS)

#define COLOR_FG 0
#define COLOR_BG 1

#define COLOR_MIN COLOR_FG
#define COLOR_MAX COLOR_BG

#define DEFAULT_FG_COLOR 0xFF000000 // opaque black
#define DEFAULT_BG_COLOR 0xFFFFFFFF // opaque white

// Note: SurfaceOperation enum is now defined in generated/surface_operation.h
// Edit schemas/surface-operation.yaml to modify the enum values

typedef struct {
    float m00, m01, m02; // first row: x' = m00*x + m01*y + m02
    float m10, m11, m12; // second row: y' = m10*x + m11*y + m12
} Transform2D;



// Note: PixelFormat enum is now defined in generated/pixel_format.h
// Edit schemas/pixel-format.yaml to modify the enum values

typedef struct {
    uint32_t mask_r;
    uint32_t mask_g;
    uint32_t mask_b;
    uint32_t mask_a;
    uint8_t  shift_r;
    uint8_t  shift_g;
    uint8_t  shift_b;
    uint8_t  shift_a;
    PixelFormat alphaVariant; // which format to use for alpha channel operations
} PixelFormatInfo;

typedef struct {
    int x;
    int y;
    int width;
    int height;
} ClipRect;


// SurfaceData: pixel buffer and metadata, shared by multiple contexts
typedef struct {
    uint32_t    ptr;     // pointer to pixels
    PixelFormat format;  // same enum as Surface/ImageData
    uint32_t    width;
    uint32_t    height;
    uint32_t    stride;  // in bytes
    // metadata
    uint32_t    layer;     // not used yet, reserved for future use
    uint32_t    ref_count; // number of contexts referencing this surface
} SurfaceData;

// SurfaceContext: per-reference rendering state
typedef struct {
    int         surface_id; // which surface this context references (-1 = unused)
    uint32_t    argb[COLOR_MAX + 1];
    Transform2D transform;
    ClipRect    clip;
    
    // Variable-length command buffer reader for this context
    CommandReader reader;
} SurfaceContext;