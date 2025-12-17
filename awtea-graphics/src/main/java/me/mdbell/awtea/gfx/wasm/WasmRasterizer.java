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

    private static final double AUTO_FLUSH_THRESHOLD = 0.8; // Flush at 80% capacity

    private final WasmSurface surface;
    private final int contextId;
    private transient final SurfaceCommandBuffer commandBuffer;
    private boolean disposed = false;

    // Track references to surfaces used in blit commands
    private final List<Integer> blitReferences = new ArrayList<>();
    private final List<WasmSurface> tempSurfaces = new ArrayList<>();

    public WasmRasterizer(WasmSurface surface, int contextId) {
        this.surface = surface;
        this.contextId = contextId;
        this.commandBuffer = surface.createBufferForContext(contextId);
    }

    private WasmRasterizer(WasmRasterizer other) {
        this.surface = other.surface;
        // Clone the context to get independent rendering state
        this.contextId = this.surface.getExports().cloneContext(other.contextId);
        if (this.contextId < 0) {
            throw new IllegalStateException("Failed to clone context");
        }
        // each rasterizer gets its own command buffer.
        this.commandBuffer = this.surface.createBufferForContext(this.contextId);
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
            int result = surface.getExports().destroyContext(contextId);
            if (result < 0) {
                log.error("WasmRasterizer: Failed to destroy context {}", contextId);
            }
            // Command buffer is owned by context and freed automatically
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

        // free up temp surfaces
        for (WasmSurface surface : tempSurfaces) {
            surface.backend.getSurfacePool().release(surface);
        }
        tempSurfaces.clear();
    }

    private void blitSurface(Surface srcSurface, int destX, int destY) {
        // pixel data already uploaded to WASM memory by the Surface implementation
        if (srcSurface instanceof WasmSurface) {
            WasmSurface wasmSurface = (WasmSurface) srcSurface;
            int srcSurfaceId = wasmSurface.getId();

            // Create a reference to keep the source surface alive during deferred rendering
            int refResult = surface.getExports().createReference(srcSurfaceId);
            if (refResult < 0) {
                log.error("WasmRasterizer: Failed to create reference for surface {}, skipping blit", srcSurfaceId);
                return;
            }

            // Track this reference so we can release it after flush
            blitReferences.add(srcSurfaceId);

            commandBuffer.emitBlitImage(
                    srcSurfaceId,
                    destX, destY);
        } else {
            // For non-WasmSurface, we need to upload to a temporary WASM surface
            WasmSurfaceBackend backend = surface.backend;

            WasmSurface wasmSurface = backend.getSurfacePool().acquire(srcSurface.getWidth(),
                    srcSurface.getHeight(),
                    srcSurface.getFormat());

            if (wasmSurface == null) {
                log.error("WasmRasterizer: blitSurface failed to create or retrieve surface cache entry");
                return;
            }

            // Upload pixel data to the temporary WASM surface
            boolean uploadSuccess = wasmSurface.uploadFromSurface(srcSurface);
            if (!uploadSuccess) {
                log.error("WasmRasterizer: Failed to upload pixel data to temporary WASM surface, skipping blit");
                backend.getSurfacePool().release(wasmSurface);
                return;
            }

            // The cache entry now holds a surface ID that we can blit from
            commandBuffer.emitBlitImage(
                    wasmSurface.getId(),
                    destX, destY);

            tempSurfaces.add(wasmSurface);
        }
    }

    private void checkAndAutoFlush() {
        if (commandBuffer.getUtilization() > AUTO_FLUSH_THRESHOLD) {
            log.debug("WasmRasterizer:  Auto-flushing at {:.1f}% capacity",
                    commandBuffer.getUtilization() * 100);
            flushAndReleaseReferences();
        }
    }

    @Override
    public void rasterizeCommands(List<SurfaceCommand> cmds) {
        if (disposed) {
            // Silently return if disposed - commands may be queued from before disposal
            return;
        }

        for (SurfaceCommand cmd : cmds) {

            checkAndAutoFlush();

            switch (cmd.type) {
                case DRAW_RECT:
                    commandBuffer.emitDrawRect(cmd.args[0], cmd.args[1], cmd.args[2], cmd.args[3]);
                    break;
                case FILL_RECT:
                    commandBuffer.emitFillRect(cmd.args[0], cmd.args[1], cmd.args[2], cmd.args[3]);
                    break;
                case BLIT_IMAGE:
                    if (!(cmd.obj instanceof SurfaceContainer)) {
                        log.error("WasmRasterizer: BLIT_IMAGE command missing SurfaceContainer object");
                    } else {
                        Surface surface1 = ((SurfaceContainer) cmd.obj).getSurface();
                        // TODO: missing width/height args?
                        blitSurface(surface1, cmd.args[0], cmd.args[1]);
                    }
                    break;
                case SET_COLOR:
                    Color c = (Color) cmd.obj;
                    int argb = c.getAlpha() << 24 |
                            c.getRed() << 16 |
                            c.getGreen() << 8 |
                            c.getBlue();
                    commandBuffer.emitSetColor(argb, cmd.argCount > 0 ? cmd.args[0] : 0);
                    break;
                case SET_CLIP_RECT:
                    Shape shape = (Shape) cmd.obj;
                    if (shape == null) {
                        // Clear clip - use sentinel value (negative width/height) to indicate no
                        // clipping
                        commandBuffer.emitSetClipRect(0, 0, -1, -1);
                    } else {
                        Rectangle bounds = shape.getBounds();
                        commandBuffer.emitSetClipRect(bounds.x, bounds.y, bounds.width, bounds.height);
                    }
                    break;
                case CLEAR_RECT:
                    commandBuffer.emitClearRect(cmd.args[0], cmd.args[1], cmd.args[2], cmd.args[3]);
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
                    commandBuffer.emitDrawLine(cmd.args[0], cmd.args[1], cmd.args[2], cmd.args[3]);
                    break;
                case DRAW_POLYGON: {
                    // TPolygon from awtea-classlib - we can't import it here, but we know it has xpoints/ypoints fields
                    Object polygonObj = cmd.obj;
                    try {
                        int[] xpts = (int[]) polygonObj.getClass().getField("xpoints").get(polygonObj);
                        int[] ypts = (int[]) polygonObj.getClass().getField("ypoints").get(polygonObj);
                        commandBuffer.emitDrawPolygon(xpts, ypts);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to extract polygon points", e);
                    }
                    break;
                }
                case FILL_POLYGON: {
                    // TPolygon from awtea-classlib - we can't import it here, but we know it has xpoints/ypoints fields
                    Object polygonObj = cmd.obj;
                    try {
                        int[] xpts = (int[]) polygonObj.getClass().getField("xpoints").get(polygonObj);
                        int[] ypts = (int[]) polygonObj.getClass().getField("ypoints").get(polygonObj);
                        commandBuffer.emitFillPolygon(xpts, ypts);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to extract polygon points", e);
                    }
                    break;
                }
                case SET_COMPOSITE:
                    if (!(cmd.obj instanceof java.awt.Composite)) {
                        log.error("WasmRasterizer: SET_COMPOSITE command missing Composite object");
                    } else {
                        java.awt.Composite composite = (java.awt.Composite) cmd.obj;
                        if (composite instanceof java.awt.AlphaComposite) {
                            java.awt.AlphaComposite alphaComp = (java.awt.AlphaComposite) composite;
                            // Map AlphaComposite rule to our CompositeMode constants
                            int mode = alphaComp.getRule();
                            float alpha = alphaComp.getAlpha();
                            commandBuffer.emitSetComposite(mode, alpha);
                        } else {
                            log.warn("WasmRasterizer: Unsupported composite type: {}", composite.getClass().getName());
                        }
                    }
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
