#include "awt_commands.h"
#include "awt_memory.h"
#include "awt_surface.h"
#include "awt_draw.h"
#include "awt_util.h"
#include "awt_log.h"

int get_command_size() {
    return sizeof(SurfaceCommand);
}

int request_command_buffer(int max_commands) {
    size_t bytes = (size_t)max_commands * sizeof(SurfaceCommand);
    void* p = tracked_malloc(bytes);
    if (!p){
        return 0;
    }
    memset(p, 0, bytes);
    return (int)(uintptr_t)p;
}

int render_awt(int context_id, uint32_t cmdPtr, int cmdCount) {
    log_debug("render_awt: context_id=%d, cmdCount=%d", context_id, cmdCount);

    SurfaceContext* ctx = get_context_data(context_id);
    if (!ctx || ctx->surface_id == -1) {
        log_error("render_awt: invalid context %d", context_id);
        return -1; // invalid context
    }

    SurfaceData* data = get_surface_data(ctx->surface_id);
    if (!data || !data->ptr) {
        log_error("render_awt: invalid surface %d for context %d", ctx->surface_id, context_id);
        return -2; // invalid surface
    }

    // Build a RenderSurface from data + context for rendering
    RenderSurface surface = make_render_surface(data, ctx);

    // Use context's buffer if cmdPtr is 0
    SurfaceCommand* cmds;
    if (cmdPtr == 0) {
        cmds = ctx->command_buffer;
        if (!cmds) {
            log_error("render_awt: context %d has no command buffer", context_id);
            return -1;
        }
    } else {
        cmds = (SurfaceCommand*)(uintptr_t)cmdPtr;
    }
    
    for (int i = 0; i < cmdCount; i++) {
        SurfaceCommand* cmd = &cmds[i];
        
        // Debug log every command
        log_debug("Command %d/%d: operation=%d, x=%d, y=%d, w=%d, h=%d, arg1=%d, arg2=%d",
                  i+1, cmdCount, cmd->operation, cmd->x, cmd->y, cmd->width, cmd->height,
                  cmd->args[0], cmd->args[1]);
        
        switch (cmd->operation) {
            case CMD_SET_COLOR:
                 set_color(ctx, cmd->set_color.which, cmd->set_color.argb);
                 // Also update our local render surface copy
                 surface.argb[cmd->set_color.which] = cmd->set_color.argb;
                 log_debug("Set color %d to 0x%08X", cmd->set_color.which, cmd->set_color.argb);
                break;
            case CMD_SET_TRANSFORM:
                ctx->transform.m00 = u32_to_float(cmd->x);
                ctx->transform.m01 = u32_to_float(cmd->y);
                ctx->transform.m02 = u32_to_float(cmd->width);
                ctx->transform.m10 = u32_to_float(cmd->height);
                ctx->transform.m11 = u32_to_float(cmd->args[0]);
                ctx->transform.m12 = u32_to_float(cmd->args[1]);
                // Also update our local render surface copy
                surface.transform = ctx->transform;
                break;
            case CMD_SET_CLIP_RECT:
                ctx->clip.x = cmd->x;
                ctx->clip.y = cmd->y;
                ctx->clip.width = cmd->width;
                ctx->clip.height = cmd->height;
                // Also update our local render surface copy
                surface.clip = ctx->clip;
                log_debug("Set clip rect to [%d, %d, %d, %d]", 
                          cmd->x, cmd->y, cmd->width, cmd->height);
                break;
            case CMD_SET_COMPOSITE:
                ctx->composite_mode = (CompositeMode)cmd->set_composite.mode;
                ctx->composite_alpha = u32_to_float(cmd->set_composite.alpha);
                // Also update our local render surface copy
                surface.composite_mode = ctx->composite_mode;
                surface.composite_alpha = ctx->composite_alpha;
                log_debug("Set composite mode=%d, alpha=%.2f", 
                          ctx->composite_mode, ctx->composite_alpha);
                break;    
            // Drawing commands
            case CMD_BLIT_IMAGE:
                blit_image(&surface, cmd->blit.surface_id, cmd->x, cmd->y);
                break;
            case CMD_DRAW_RECT:
                draw_rect(&surface, cmd->x, cmd->y, cmd->width, cmd->height,
                          surface.argb[COLOR_FG]);
                break;
            case CMD_FILL_RECT:
                log_debug("Fill rect: [%d, %d, %d, %d] with color 0x%08X", 
                          cmd->x, cmd->y, cmd->width, cmd->height, surface.argb[COLOR_FG]);
                draw_filled_rect(&surface, cmd->x, cmd->y, cmd->width, cmd->height,
                                 surface.argb[COLOR_FG]);
                break;
            case CMD_CLEAR_RECT:
                clear_rect(&surface, cmd->x, cmd->y, cmd->width, cmd->height);
                break;
            case CMD_DRAW_LINE:
                draw_line(&surface, cmd->x, cmd->y,
                          cmd->width, cmd->height,
                          surface.argb[COLOR_FG]);
                break;

            // No-op or unknown command

            case CMD_NO_OP:
            default:
                // do nothing
                break;
        }
    }
    return 0;
}