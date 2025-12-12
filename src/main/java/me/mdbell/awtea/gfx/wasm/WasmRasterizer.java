package me.mdbell.awtea.gfx.wasm;

import me.mdbell.awtea.classlib.java.awt.geom.TAffineTransform;
import me.mdbell.awtea.gfx.Rasterizer;
import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.gfx.SurfaceCommand;
import me.mdbell.awtea.gfx.SurfaceContainer;
import me.mdbell.awtea.instrument.Monitored;

import java.awt.*;
import java.util.List;

@Monitored.AllMethods
public class WasmRasterizer implements Rasterizer {

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
	public Rasterizer create() {
		return new WasmRasterizer(this);
	}

	@Override
	public void reset() {
	}

	private void blitSurface(Surface srcSurface, int destX, int destY) {
		// pixel data already uploaded to WASM memory by the Surface implementation
		if (srcSurface instanceof WasmSurface) {
			WasmSurface wasmSurface = (WasmSurface) srcSurface;
			commandBuffer.emitBlitImage(
				wasmSurface.getId(),
				destX, destY);
		} else {
			WasmSurfaceBackend backend = surface.backend;

			SurfaceLRUCache.SurfaceCacheEntry cacheEntry = backend.surfaceCache.get(srcSurface);
			if (cacheEntry == null) {
				System.err.println("WasmRasterizer: blitSurface failed to lookup surface cache");
				return;
			}

			cacheEntry.sync();

			commandBuffer.emitBlitImage(
				cacheEntry.imageId,
				destX, destY);
		}
	}

	@Override
	public void rasterizeCommands(List<SurfaceCommand> cmds) {
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
						System.err.println("WasmRasterizer: BLIT_IMAGE command missing SurfaceContainer object");
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
