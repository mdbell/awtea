package me.mdbell.awtea.gfx.wasm;

import me.mdbell.awtea.gfx.Rasterizer;
import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.gfx.SurfaceCommand;
import me.mdbell.awtea.gfx.SurfaceContainer;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class WasmRasterizer implements Rasterizer {

    private static final Logger log = LoggerFactory.getLogger(WasmRasterizer.class);

    private final WasmSurface surface;
    private final int contextId;
    private transient final SurfaceCommandBuffer commandBuffer;
    private boolean disposed = false;
    
    // Track references to surfaces used in blit commands
    private final List<Integer> blitReferences = new ArrayList<>();

    public WasmRasterizer(WasmSurface surface, int contextId) {
        this.surface = surface;
        this.contextId = contextId;
        this.commandBuffer = surface.createBuffer();
        this.commandBuffer.setContextId(contextId);
    }

    private WasmRasterizer(WasmRasterizer other) {
        this.surface = other.surface;
        // Clone the context to get independent rendering state
        this.contextId = this.surface.getExports().cloneContext(other.contextId);
        if (this.contextId < 0) {
            throw new IllegalStateException("Failed to clone context");
        }
        // each rasterizer gets its own command buffer.
        this.commandBuffer = this.surface.createBuffer();
        this.commandBuffer.setContextId(this.contextId);
    }

    @Override
    public Rasterizer create() {
        if (disposed) {
            throw new IllegalStateException("Cannot create from disposed rasterizer");
        }
        return new WasmRasterizer(this);
    }

    @Override
    public void reset() {
    }

    public void dispose() {
        if (!disposed && contextId >= 0) {
            // Flush any pending commands before destroying context
            flushAndReleaseReferences();
            // Destroy the context (decrements surface ref count)
            surface.getExports().destroyContext(contextId);
            disposed = true;
        }
    }

    private void flushAndReleaseReferences() {
        // Flush commands first
        commandBuffer.flush();
        
        // Release all blit references
        for (Integer surfaceId : blitReferences) {
            // Decrement ref count by calling release_reference
            int result = surface.getExports().releaseReference(surfaceId);
            if (result < 0) {
                log.error("WasmRasterizer: Failed to release reference for surface {}", surfaceId);
            }
        }
        blitReferences.clear();
    }

    private void blitSurface(Surface srcSurface, int destX, int destY) {
        // pixel data already uploaded to WASM memory by the Surface implementation
        if (srcSurface instanceof WasmSurface) {
            WasmSurface wasmSurface = (WasmSurface) srcSurface;
            int srcSurfaceId = wasmSurface.getId();
            
            // Create a reference to keep the source surface alive during deferred rendering
            int refResult = surface.getExports().createReference(srcSurfaceId);
            if (refResult < 0) {
                log.error("WasmRasterizer: Failed to create reference for surface {}", srcSurfaceId);
            } else {
                // Track this reference so we can release it after flush
                blitReferences.add(srcSurfaceId);
            }
            
            commandBuffer.emitBlitImage(
                    srcSurfaceId,
                    destX, destY);
        } else {
            WasmSurfaceBackend backend = surface.backend;

            SurfaceLRUCache.SurfaceCacheEntry cacheEntry = backend.surfaceCache.create(srcSurface);
            if (cacheEntry == null) {
                log.error("WasmRasterizer: blitSurface failed to lookup surface cache");
                return;
            }

            cacheEntry.sync();

            commandBuffer.emitBlitImage(
                    cacheEntry.imageId,
                    destX, destY);
            commandBuffer.emitFreeImage(cacheEntry.imageId);
            commandBuffer.flush(); // we have to flush here to ensure the image is ready before drawing
        }
    }

    @Override
    public void rasterizeCommands(List<SurfaceCommand> cmds) {
        if (disposed) {
            throw new IllegalStateException("Cannot rasterize with disposed rasterizer");
        }
        
        for (SurfaceCommand cmd : cmds) {
            switch (cmd.type) {
                case DRAW_RECT:
                    commandBuffer.emitDrawRect(cmd.arg1, cmd.arg2, cmd.arg3, cmd.arg4);
                    break;
                case FILL_RECT:
                    commandBuffer.emitFillRect(cmd.arg1, cmd.arg2, cmd.arg3, cmd.arg4);
                    break;
                case BLIT_IMAGE:
                    if (!(cmd.obj instanceof SurfaceContainer)) {
                        log.error("WasmRasterizer: BLIT_IMAGE command missing SurfaceContainer object");
                    } else {
                        Surface surface1 = ((SurfaceContainer) cmd.obj).getSurface();
                        //TODO: missing width/height args?
                        blitSurface(surface1, cmd.arg1, cmd.arg2);
                    }
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
                    me.mdbell.awtea.gfx.AffineTransform transform = (me.mdbell.awtea.gfx.AffineTransform) cmd.obj;
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
                    log.info("WasmRasterizer: Unhandled command type: {}", cmd.type);
                    break;
            }
        }
        flushAndReleaseReferences();
    }
}
