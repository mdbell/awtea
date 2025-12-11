#include "awt_commands.h"
#include "awt_surface.h"
#include "awt_draw.h"
#include "awt_util.h"

int get_command_size() {
    return sizeof(SurfaceCommand);
}

int render_awt(int surface_id, uint32_t cmdPtr, int cmdCount) {

    Surface* surface = get_surface_data(surface_id);

    if( !surface || !surface->ptr ) {
        return -1;
    }

    SurfaceCommand* cmds = (SurfaceCommand*)(uintptr_t)cmdPtr;
    for (int i = 0; i < cmdCount; i++) {
        SurfaceCommand* cmd = &cmds[i];
        switch (cmd->operation) {
            case CMD_SET_COLOR:
                 set_color(surface, cmd->set_color.which, cmd->set_color.argb);
                break;
            case CMD_SET_TRANSFORM:
                surface->transform.m00 = u32_to_float(cmd->x);
                surface->transform.m01 = u32_to_float(cmd->y);
                surface->transform.m02 = u32_to_float(cmd->width);
                surface->transform.m10 = u32_to_float(cmd->height);
                surface->transform.m11 = u32_to_float(cmd->args[0]);
                surface->transform.m12 = u32_to_float(cmd->args[1]);
                break;
            case CMD_SET_CLIP_RECT:
                surface->clip.x = cmd->x;
                surface->clip.y = cmd->y;
                surface->clip.width = cmd->width;
                surface->clip.height = cmd->height;
            break;    

            // Drawing commands
            case CMD_BLIT_IMAGE:
                blit_image(surface, cmd->blit.image_id, cmd->x, cmd->y);
            break;
            case CMD_DRAW_RECT:
                draw_rect(surface, cmd->x, cmd->y, cmd->width, cmd->height,
                          surface->argb[COLOR_FG]);
                break;
            case CMD_FILL_RECT:
                draw_filled_rect(surface, cmd->x, cmd->y, cmd->width, cmd->height,
                                 surface->argb[COLOR_FG]);
                break;
            case CMD_CLEAR_RECT:
                clear_rect(surface, cmd->x, cmd->y, cmd->width, cmd->height);
                break;
            case CMD_DRAW_LINE:
                draw_line(surface, cmd->x, cmd->y,
                          cmd->width, cmd->height,
                          surface->argb[COLOR_FG]);
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