#include "awt_commands.h"
#include "awt_memory.h"
#include "awt_surface.h"
#include "awt_draw.h"
#include "awt_util.h"
#include "awt_log.h"
#include "awt_stack.h"

// Forward declarations of command handlers
typedef int (*CommandHandler)(SurfaceContext* ctx, RenderSurface* surface, CommandReader* reader, uint8_t flags, uint16_t length);

static int handle_no_op(SurfaceContext* ctx, RenderSurface* surface, CommandReader* reader, uint8_t flags, uint16_t length);
static int handle_set_color(SurfaceContext* ctx, RenderSurface* surface, CommandReader* reader, uint8_t flags, uint16_t length);
static int handle_set_transform(SurfaceContext* ctx, RenderSurface* surface, CommandReader* reader, uint8_t flags, uint16_t length);
static int handle_set_clip_rect(SurfaceContext* ctx, RenderSurface* surface, CommandReader* reader, uint8_t flags, uint16_t length);
static int handle_set_composite(SurfaceContext* ctx, RenderSurface* surface, CommandReader* reader, uint8_t flags, uint16_t length);
static int handle_blit_image(SurfaceContext* ctx, RenderSurface* surface, CommandReader* reader, uint8_t flags, uint16_t length);
static int handle_draw_rect(SurfaceContext* ctx, RenderSurface* surface, CommandReader* reader, uint8_t flags, uint16_t length);
static int handle_fill_rect(SurfaceContext* ctx, RenderSurface* surface, CommandReader* reader, uint8_t flags, uint16_t length);
static int handle_clear_rect(SurfaceContext* ctx, RenderSurface* surface, CommandReader* reader, uint8_t flags, uint16_t length);
static int handle_draw_line(SurfaceContext* ctx, RenderSurface* surface, CommandReader* reader, uint8_t flags, uint16_t length);

// Command handler function table (indexed by SurfaceOperation enum)
static const CommandHandler command_handlers[] = {
    [CMD_NO_OP] = handle_no_op,
    [CMD_SET_COLOR] = handle_set_color,
    [CMD_SET_TRANSFORM] = handle_set_transform,
    [CMD_SET_CLIP_RECT] = handle_set_clip_rect,
    [CMD_SET_COMPOSITE] = handle_set_composite,
    [CMD_BLIT_IMAGE] = handle_blit_image,
    [CMD_DRAW_RECT] = handle_draw_rect,
    [CMD_FILL_RECT] = handle_fill_rect,
    [CMD_CLEAR_RECT] = handle_clear_rect,
    [CMD_DRAW_LINE] = handle_draw_line,
};

// Number of command handlers
#define NUM_COMMAND_HANDLERS (sizeof(command_handlers) / sizeof(command_handlers[0]))

int render_awt(int context_id, uint32_t cmdPtr, int bytesUsed) {
    STACK_ENTER_EXT(NULL, -1, context_id, 0, 0, 0);
    
    log_debug("render_awt: context_id=%d, bytesUsed=%d", context_id, bytesUsed);

    SurfaceContext* ctx = get_context_data(context_id);
    if (!ctx || ctx->surface_id == -1) {
        log_error("render_awt: invalid context %d", context_id);
        STACK_EXIT_ERR(-1);
        return -1;
    }

    SurfaceData* data = get_surface_data(ctx->surface_id);
    if (!data || !data->ptr) {
        log_error("render_awt: invalid surface %d for context %d", ctx->surface_id, context_id);
        STACK_EXIT_ERR(-2);
        return -2;
    }

    // Build a RenderSurface from data + context for rendering
    RenderSurface surface = make_render_surface(data, ctx);

    // Use context's buffer if cmdPtr is 0
    CommandReader* reader = &ctx->reader;
    if (cmdPtr != 0) {
        // Using external buffer (backward compatibility, but not recommended)
        log_warn("render_awt: using external buffer at 0x%08X (not recommended)", cmdPtr);
        // We can't safely use an external buffer with our reader, so return error
        log_error("render_awt: external buffers not supported with variable-length commands");
        STACK_EXIT_ERR(-3);
        return -3;
    }

    // Reset reader to beginning with the specified byte limit
    reset_reader(reader, bytesUsed);

    // Parse and execute commands
    int commands_processed = 0;
    while (!reader_at_end(reader)) {
        // Read command header: [opcode:1][flags:1][length:2]
        if (!reader_has_bytes(reader, 4)) {
            log_error("render_awt: incomplete command header at pos %zu", reader_position(reader));
            STACK_EXIT_ERR(-4);
            return -4;
        }

        uint8_t opcode = read_u8(reader);
        uint8_t flags = read_u8(reader);
        uint16_t length = read_u16(reader); // Length in words (4-byte units)

        log_debug("Command %d: opcode=%d, flags=0x%02X, length=%d words", 
                  commands_processed + 1, opcode, flags, length);

        stack_push_extended("render_awt", __LINE__, NULL, ctx->surface_id, context_id, 
                           opcode, (uint16_t)commands_processed + 1, (uint16_t)data->ref_count);

        // Validate opcode
        if (opcode >= NUM_COMMAND_HANDLERS || command_handlers[opcode] == NULL) {
            log_warn("Command %d: unknown opcode %d, skipping %d bytes",
                     commands_processed + 1, opcode, length * 4);
            // Skip the command data
            if (!reader_skip(reader, length * 4)) {
                log_error("render_awt: failed to skip unknown command at pos %zu", reader_position(reader));
                return -6;
            }
            commands_processed++;
            continue;
        }

        // Dispatch to handler
        CommandHandler handler = command_handlers[opcode];
        int result = handler(ctx, &surface, reader, flags, length);
        if (result != 0) {
            log_error("Command %d (opcode=%d) failed with error %d", 
                      commands_processed + 1, opcode, result);
            return -7;
        }
        stack_pop();
        commands_processed++;
    }

    log_debug("render_awt: processed %d commands successfully", commands_processed);
    STACK_EXIT();
    return 0;
}

// Command handler implementations

static int handle_no_op(SurfaceContext* ctx, RenderSurface* surface, CommandReader* reader, uint8_t flags, uint16_t length) {
    // Skip any data (should be 0 length anyway)
    reader_skip(reader, length * 4);
    return 0;
}

static int handle_set_color(SurfaceContext* ctx, RenderSurface* surface, CommandReader* reader, uint8_t flags, uint16_t length) {
    // Expected: [argb: uint32][which: uint32]
    if (length != 2) {
        log_error("handle_set_color: expected length 2, got %d", length);
        reader_skip(reader, length * 4);
        return -1;
    }

    uint32_t argb = read_u32(reader);
    uint32_t which = read_u32(reader);

    if (which > COLOR_MAX) {
        log_error("handle_set_color: invalid color index %d (max %d)", which, COLOR_MAX);
        return -1;
    }

    set_color(ctx, which, argb);
    surface->argb[which] = argb;
    log_debug("Set color %d to 0x%08X", which, argb);
    return 0;
}

static int handle_set_transform(SurfaceContext* ctx, RenderSurface* surface, CommandReader* reader, uint8_t flags, uint16_t length) {
    // Expected: [m00: float][m01: float][m02: float][m10: float][m11: float][m12: float]
    if (length != 6) {
        log_error("handle_set_transform: expected length 6, got %d", length);
        reader_skip(reader, length * 4);
        return -1;
    }

    ctx->transform.m00 = read_float(reader);
    ctx->transform.m01 = read_float(reader);
    ctx->transform.m02 = read_float(reader);
    ctx->transform.m10 = read_float(reader);
    ctx->transform.m11 = read_float(reader);
    ctx->transform.m12 = read_float(reader);

    surface->transform = ctx->transform;
    log_debug("Set transform: [[%.2f, %.2f, %.2f], [%.2f, %.2f, %.2f]]",
              ctx->transform.m00, ctx->transform.m01, ctx->transform.m02,
              ctx->transform.m10, ctx->transform.m11, ctx->transform.m12);
    return 0;
}

static int handle_set_clip_rect(SurfaceContext* ctx, RenderSurface* surface, CommandReader* reader, uint8_t flags, uint16_t length) {
    // Expected: [x: int32][y: int32][width: int32][height: int32]
    if (length != 4) {
        log_error("handle_set_clip_rect: expected length 4, got %d", length);
        reader_skip(reader, length * 4);
        return -1;
    }

    ctx->clip.x = (int)read_u32(reader);
    ctx->clip.y = (int)read_u32(reader);
    ctx->clip.width = (int)read_u32(reader);
    ctx->clip.height = (int)read_u32(reader);

    log_debug("Set clip rect to [%d, %d, %d, %d]",
              ctx->clip.x, ctx->clip.y, ctx->clip.width, ctx->clip.height);
    return 0;
}

static int handle_set_composite(SurfaceContext* ctx, RenderSurface* surface, CommandReader* reader, uint8_t flags, uint16_t length) {
    if(length != 2) {
        log_error("handle_set_composite: expected length 2, got %d", length);
        reader_skip(reader, length * 4);
        return -1;
    }
    ctx->composite_mode = (CompositeMode) read_u32(reader);
    ctx->composite_alpha = read_float(reader);
    surface->composite_mode = ctx->composite_mode;
    surface->composite_alpha = ctx->composite_alpha;
    log_debug("Set composite mode=%d, alpha=%.2f", 
                ctx->composite_mode, ctx->composite_alpha);
    return 0;
}

static int handle_blit_image(SurfaceContext* ctx, RenderSurface* surface, CommandReader* reader, uint8_t flags, uint16_t length) {
    // Expected: [surface_id: int32][x: int32][y: int32]
    if (length != 3) {
        log_error("handle_blit_image: expected length 3, got %d", length);
        reader_skip(reader, length * 4);
        return -1;
    }

    int surface_id = (int)read_u32(reader);
    int x = (int)read_u32(reader);
    int y = (int)read_u32(reader);

    blit_image(surface, surface_id, x, y);
    log_debug("Blit image: surface_id=%d at (%d, %d)", surface_id, x, y);
    return 0;
}

static int handle_draw_rect(SurfaceContext* ctx, RenderSurface* surface, CommandReader* reader, uint8_t flags, uint16_t length) {
    // Expected: [x: int32][y: int32][width: int32][height: int32]
    if (length != 4) {
        log_error("handle_draw_rect: expected length 4, got %d", length);
        reader_skip(reader, length * 4);
        return -1;
    }

    int x = (int)read_u32(reader);
    int y = (int)read_u32(reader);
    int width = (int)read_u32(reader);
    int height = (int)read_u32(reader);

    draw_rect(surface, x, y, width, height, surface->argb[COLOR_FG]);
    log_debug("Draw rect: [%d, %d, %d, %d]", x, y, width, height);
    return 0;
}

static int handle_fill_rect(SurfaceContext* ctx, RenderSurface* surface, CommandReader* reader, uint8_t flags, uint16_t length) {
    // Expected: [x: int32][y: int32][width: int32][height: int32]
    if (length != 4) {
        log_error("handle_fill_rect: expected length 4, got %d", length);
        reader_skip(reader, length * 4);
        return -1;
    }

    int x = (int)read_u32(reader);
    int y = (int)read_u32(reader);
    int width = (int)read_u32(reader);
    int height = (int)read_u32(reader);

    draw_filled_rect(surface, x, y, width, height, surface->argb[COLOR_FG]);
    log_debug("Fill rect: [%d, %d, %d, %d] with color 0x%08X",
              x, y, width, height, surface->argb[COLOR_FG]);
    return 0;
}

static int handle_clear_rect(SurfaceContext* ctx, RenderSurface* surface, CommandReader* reader, uint8_t flags, uint16_t length) {
    // Expected: [x: int32][y: int32][width: int32][height: int32]
    if (length != 4) {
        log_error("handle_clear_rect: expected length 4, got %d", length);
        reader_skip(reader, length * 4);
        return -1;
    }

    int x = (int)read_u32(reader);
    int y = (int)read_u32(reader);
    int width = (int)read_u32(reader);
    int height = (int)read_u32(reader);

    clear_rect(surface, x, y, width, height);
    log_debug("Clear rect: [%d, %d, %d, %d]", x, y, width, height);
    return 0;
}

static int handle_draw_line(SurfaceContext* ctx, RenderSurface* surface, CommandReader* reader, uint8_t flags, uint16_t length) {
    // Expected: [x1: int32][y1: int32][x2: int32][y2: int32]
    if (length != 4) {
        log_error("handle_draw_line: expected length 4, got %d", length);
        reader_skip(reader, length * 4);
        return -1;
    }

    int x1 = (int)read_u32(reader);
    int y1 = (int)read_u32(reader);
    int x2 = (int)read_u32(reader);
    int y2 = (int)read_u32(reader);

    draw_line(surface, x1, y1, x2, y2, surface->argb[COLOR_FG]);
    log_debug("Draw line: (%d, %d) to (%d, %d)", x1, y1, x2, y2);
    return 0;
}