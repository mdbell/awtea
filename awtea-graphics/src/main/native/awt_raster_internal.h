#pragma once
#include <math.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>

// Include auto-generated enums
#include "generated/surface_operation.h"
#include "generated/pixel_format.h"

#define MAX_IMAGES 1024
#define NUM_SURFACES 1024

#define START_IMAGE_ID 0
#define END_IMAGE_ID MAX_IMAGES

#define START_SURFACE_ID MAX_IMAGES
#define END_SURFACE_ID (START_SURFACE_ID + NUM_SURFACES)

#define COLOR_FG 0
#define COLOR_BG 1

#define COLOR_MIN COLOR_FG
#define COLOR_MAX COLOR_BG

#define DEFAULT_FG_COLOR 0xFF000000 // opaque black
#define DEFAULT_BG_COLOR 0xFFFFFFFF // opaque white

// Note: SurfaceOperation enum is now defined in generated/surface_operation.h
// Edit schemas/surface-operation.yaml to modify the enum values

typedef struct {
    uint8_t operation; // SurfaceOperation
    uint8_t reserved[3]; // Padding for alignment
    uint32_t x; // X coordinate for the command
    uint32_t y; // Y coordinate for the command
    uint32_t width; // Width parameter
    uint32_t height; // Height parameter
    union {
        struct { uint32_t argb, which; } set_color;
        struct { uint32_t image_id; } blit;
        //TODO: figure out how we're going to do transforms
        // struct { uint32_t m00, m01, m10, m11; } transform; 
        uint32_t args[2]; // Fallback for generic access
    };
} SurfaceCommand;

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


// ===================================================================================
// | Note: Conceptually Surface and ImageView are very similar                       |
// |       They differ in that Surface has rendering state (clip, transform, colors) |
// |       whereas ImageView is intended to represent external image data            |
// |       However, they share the same initial memory layout for pixel data         |
// |       Thus, we can cast between Surface* and ImageView* in some places          |
// ===================================================================================

typedef struct {
    // exact same structure as ImageView
    // it _must_ be at the start of this struct, and match ImageView layout
    // (we cast between Surface* and ImageView* in some places)
    uint32_t    ptr;     // pointer to pixels
    PixelFormat format;  // same enum as Surface/ImageData
    uint32_t    width;
    uint32_t    height;
    uint32_t    stride;  // in bytes
    // render state
    uint32_t    layer; // not used yet, reserved for future use
    uint32_t    argb[COLOR_MAX + 1];
    Transform2D transform;
    ClipRect    clip;
} Surface;

typedef struct {
    uint32_t    ptr;     // pointer to pixels
    PixelFormat format;  // same enum as Surface/ImageData
    uint32_t    width;
    uint32_t    height;
    uint32_t    stride;  // in bytes
} ImageView;