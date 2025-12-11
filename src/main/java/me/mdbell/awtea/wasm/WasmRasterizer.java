package me.mdbell.awtea.wasm;

import me.mdbell.awtea.classlib.java.awt.awtea.gfx.TRasterizer;
import me.mdbell.awtea.classlib.java.awt.awtea.gfx.TSurfaceCommand;
import me.mdbell.awtea.classlib.java.awt.geom.TAffineTransform;

import java.awt.*;
import java.util.List;

public class WasmRasterizer implements TRasterizer {

    private final WasmSurface surface;
    private transient final SurfaceCommandBuffer commandBuffer;

    public WasmRasterizer(WasmSurface surface) {
        this.surface = surface;
        this.commandBuffer = surface.createBuffer();
    }

    private WasmRasterizer(WasmRasterizer other) {
        this.surface = other.surface;
        // each rasterizer gets its own command buffer.
        this.commandBuffer = this.surface.createBuffer();
    }

    @Override
    public TRasterizer create() {
        return new WasmRasterizer(this);
    }

    @Override
    public void reset() {
    }

    @Override
    public void onResize(int width, int height) {
        surface.resize(width, height);
    }

    @Override
    public void rasterizeCommands(List<TSurfaceCommand> cmds) {
        for (TSurfaceCommand cmd : cmds) {
            switch (cmd.type) {
                case DRAW_RECT:
                    commandBuffer.emitDrawRect(cmd.arg1, cmd.arg2, cmd.arg3, cmd.arg4);
                    break;
                case FILL_RECT:
                    commandBuffer.emitFillRect(cmd.arg1, cmd.arg2, cmd.arg3, cmd.arg4);
                    break;
                case BLIT_IMAGE:
                    //TODO: implement image blitting (Java side)
//                    commandBuffer.emitBlitImage(
//                            ((WasmImage)cmd.obj).getImageId(),
//                            cmd.arg1, cmd.arg2, cmd.arg3, cmd.arg4);
                    break;
                case SET_COLOR:
                    Color c = (Color) cmd.obj;
                    int argb = c.getAlpha() << 24 |
                            c.getRed() << 16 |
                            c.getGreen() << 8 |
                            c.getBlue();
                    commandBuffer.emitSetColor(argb, cmd.arg1);
                    break;
                case SET_CLIP_RECT:
                    commandBuffer.emitSetClipRect(cmd.arg1, cmd.arg2, cmd.arg3, cmd.arg4);
                    break;
                case CLEAR_RECT:
                    commandBuffer.emitClearRect(cmd.arg1, cmd.arg2, cmd.arg3, cmd.arg4);
                    break;
                case SET_TRANSFORM:
                    TAffineTransform transform = (TAffineTransform) cmd.obj;
                    commandBuffer.emitSetTransform(
                            (float) transform.getScaleX(),
                            (float) transform.getShearY(),
                            (float) transform.getShearX(),
                            (float) transform.getScaleY(),
                            (float) transform.getTranslateX(),
                            (float) transform.getTranslateY());
                    break;
                case DRAW_LINE:
                    commandBuffer.emitDrawLine(cmd.arg1, cmd.arg2, cmd.arg3, cmd.arg4);
                    break;
                case NO_OP:
                    break;
                default:
                    System.out.println("WasmRasterizer: Unhandled command type: " + cmd.type);
                    break;
            }
        }
        commandBuffer.flush();
    }
}
