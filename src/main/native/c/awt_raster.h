#include <stdint.h>

#define MAX_IMAGES 1024
//TODO: figure out a good number here
#define NUM_SURFACES 20

#define COLOR_FG 0
#define COLOR_BG 1

#define COLOR_MIN COLOR_FG
#define COLOR_MAX COLOR_BG

#define DEFAULT_FG_COLOR 0xFF000000 // opaque black
#define DEFAULT_BG_COLOR 0xFFFFFFFF // opaque white

#define ASSERT_SURFACE_VALID_RET(surf, ret) \
    do { \
        if (!(surf) || !(surf)->ptr) { \
            return (ret); \
        } \
    } while (0)

#define ASSERT_SURFACE_VALID(surf) \
    ASSERT_SURFACE_VALID_RET(surf, -1)

#define CHECK_SURFACE_ID_RET(sid, err) \
    do { \
        if ((sid) < 0 || (sid) >= NUM_SURFACES) { \
            return (err); \
        } \
    } while (0)

#define CHECK_SURFACE_ID(sid) \
    CHECK_SURFACE_ID_RET(sid, -1)

#define CHECK_SURFACE_ID_VOID(sid) \
    do { \
        if ((sid) < 0 || (sid) >= NUM_SURFACES) { \
            return; \
        } \
    } while (0)

// This should mirror the Operation enum in TSurfaceCommand.java
typedef enum {
    CMD_NO_OP = 0,

    // State setting commands
    CMD_SET_COLOR,
    CMD_SET_TRANSFORM,
    CMD_SET_CLIP_RECT,

    // Drawing commands
    CMD_BLIT_IMAGE,
    CMD_DRAW_RECT,
    CMD_FILL_RECT,
    CMD_CLEAR_RECT,

    CMD_COUNT // last value is reserved for counting

} SurfaceOperation;

typedef enum {
    PIXEL_FORMAT_ARGB = 0,
    PIXEL_FORMAT_RGB,
    PIXEL_FORMAT_RGBA,
    PIXEL_FORMAT_COUNT // last value is reserved for counting
} PixelFormat;

typedef struct {
    uint8_t operation; // SurfaceOperation
    uint8_t reserved[3]; // Padding for alignment
    uint32_t x; // X coordinate for the command
    uint32_t y; // Y coordinate for the command
    uint32_t width; // Width parameter
    uint32_t height; // Height parameter
    union {
        struct { uint32_t rgba, which; } set_color;
        struct { uint32_t image_id; } blit;
        //TODO: figure out how we're going to do transforms
        // struct { uint32_t m00, m01, m10, m11; } transform; 
        uint32_t args[2]; // Fallback for generic access
    };
} SurfaceCommand;

typedef struct {
    uint32_t ptr; // Pointer to pixel data
    uint8_t format; // Pixel format (PixelFormat)
    uint8_t reserved[3]; // Padding for alignment
    uint32_t width;
    uint32_t height;
    uint32_t stride;
} ImageData;

typedef struct {
    int x;
    int y;
    int width;
    int height;
} ClipRect;

// per-surface context
typedef struct {
    uint32_t ptr; // Pointer to pixel data
    PixelFormat pixel_format;
    uint32_t width;
    uint32_t height;
    uint32_t stride; // in bytes (usually width * 4)
    uint32_t rgba[COLOR_MAX + 1];
    float transform[6];
    ClipRect clip;
} Surface;

// pixel format LUT for masks & shifts

typedef struct {
    uint32_t mask_r;
    uint32_t mask_g;
    uint32_t mask_b;
    uint32_t mask_a;
    uint8_t  shift_r;
    uint8_t  shift_g;
    uint8_t  shift_b;
    uint8_t  shift_a;
} PixelFormatInfo;

// used for function table for set_pixel
typedef void (*SetPixelFunc)(Surface*, int, int, PixelFormat, uint32_t);

// function defs

static void set_pixel_generic(Surface* surface, int x, int y, PixelFormat srcFormat, uint32_t pixel);

static void set_pixel_same_format(Surface* surface, int x, int y, PixelFormat srcFormat, uint32_t pixel);

static void set_pixel_rgb_src(Surface* surface, int x, int y, PixelFormat srcFormat, uint32_t pixel);

static inline int clamp_int(int v, int lo, int hi);

static inline int clip_x(int x, const Surface* surf);

static inline int clip_y(int y, const Surface* surf);