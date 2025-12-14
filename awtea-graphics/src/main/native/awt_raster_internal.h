#pragma once
#include <math.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>

#define MAX_IMAGES 1024
#define NUM_SURFACES 1024
#define NUM_CONTEXTS 2048

#define START_IMAGE_ID 0
#define END_IMAGE_ID MAX_IMAGES

#define START_SURFACE_ID MAX_IMAGES
#define END_SURFACE_ID (START_SURFACE_ID + NUM_SURFACES)

#define START_CONTEXT_ID (START_SURFACE_ID + NUM_SURFACES)
#define END_CONTEXT_ID (START_CONTEXT_ID + NUM_CONTEXTS)

#define COLOR_FG 0
#define COLOR_BG 1

#define COLOR_MIN COLOR_FG
#define COLOR_MAX COLOR_BG

#define DEFAULT_FG_COLOR 0xFF000000 // opaque black
#define DEFAULT_BG_COLOR 0xFFFFFFFF // opaque white

// This should mirror the Operation enum in TSurfaceCommand.java
typedef enum {
    CMD_NO_OP = 0,

    // State setting commands
    CMD_SET_COLOR,
    CMD_SET_TRANSFORM,
    CMD_SET_CLIP_RECT,
    CMD_SET_COMPOSITE, // unimplemented in wasm backend, only in software right now

    // Drawing commands
    CMD_BLIT_IMAGE,
    CMD_DRAW_RECT,
    CMD_FILL_RECT,
    CMD_CLEAR_RECT,
    CMD_DRAW_LINE,

    CMD_COUNT, // last value is reserved for counting

    EXT_FREE_IMAGE = 128, // non-standard render commands start here

} SurfaceOperation;

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



// This file is the source of truth for the representation of the pixel formats,
// it should be updated _first_ before anywhere else (e.g Java side)
typedef enum {
    PIXEL_FORMAT_ARGB = 0,
    PIXEL_FORMAT_RGB,
    PIXEL_FORMAT_RGBA,
    PIXEL_FORMAT_ABGR,
    PIXEL_FORMAT_BGR,
    PIXEL_FORMAT_COUNT // last value is reserved for counting
} PixelFormat;

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
// | Note: Conceptually SurfaceData and ImageView are very similar                   |
// |       They differ in that SurfaceData has reference counting                    |
// |       whereas ImageView is intended to represent external image data            |
// |       However, they share the same initial memory layout for pixel data         |
// |       Thus, we can cast between SurfaceData* and ImageView* in some places      |
// ===================================================================================

typedef struct {
    uint32_t    ptr;     // pointer to pixels
    PixelFormat format;  // same enum as Surface/ImageData
    uint32_t    width;
    uint32_t    height;
    uint32_t    stride;  // in bytes
} ImageView;

// SurfaceData: pixel buffer and metadata, shared by multiple contexts
typedef struct {
    // exact same structure as ImageView
    // it _must_ be at the start of this struct, and match ImageView layout
    // (we cast between SurfaceData* and ImageView* in some places)
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
} SurfaceContext;

// Legacy typedef for backwards compatibility during transition
typedef SurfaceData Surface;

// RenderSurface: temporary combined view of SurfaceData + SurfaceContext for rendering
// This struct has the same layout as the legacy Surface struct, making it easy to
// pass to existing rendering functions that need both pixel data and rendering state.
typedef struct {
    // Pixel data fields (from SurfaceData)
    uint32_t    ptr;
    PixelFormat format;
    uint32_t    width;
    uint32_t    height;
    uint32_t    stride;
    uint32_t    layer;
    // Rendering state fields (from SurfaceContext)
    uint32_t    argb[COLOR_MAX + 1];
    Transform2D transform;
    ClipRect    clip;
} RenderSurface;