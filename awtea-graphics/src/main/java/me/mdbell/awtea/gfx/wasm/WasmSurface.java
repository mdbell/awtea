package me.mdbell.awtea.gfx.wasm;

import lombok.Getter;
import me.mdbell.awtea.gfx.Rasterizer;
import me.mdbell.awtea.gfx.Surface;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Uint8ClampedArray;

public final class WasmSurface implements Surface {
	private final WasmAwtRasterizerExports exports;
	private int surfaceId;
	private final ArrayBuffer memoryBuffer;
	// private final int layer = 0; // presently unused
	private final int pixelFormat;

	// Note: commands are 8 * 4 bytes each (see TSurfaceCommand), so 1024 commands =
	// 32KB
	// 512 = 16KB
	private static final int MAX_COMMANDS = 512;

	@Getter
	private int pixelsPtr = 0;
	private int width = 0;
	private int height = 0;
	@Getter
	private int stride = 0;

	private Uint8ClampedArray pixelsView = null;

	WasmSurfaceBackend backend;

	// Track if this surface should be returned to the pool on destroy
	private boolean poolable = true;

	WasmSurface(WasmSurfaceBackend backend, int surfaceId,
			int width, int height, int pixelFormat) {
		this.backend = backend;
		this.exports = backend.exports;
		this.surfaceId = surfaceId;
		this.pixelFormat = pixelFormat;
		this.memoryBuffer = exports.getMemory().getBuffer();

		// layer is presently unused, so set to 0 - future versions may use it
		resize(width, height);
	}

	/**
	 * Create a command buffer associated with a specific context.
	 * This uses the context's internal buffer (16KB).
	 */
	public SurfaceCommandBuffer createBufferForContext(int contextId) {
		return new SurfaceCommandBuffer(contextId, this.exports);
	}

	@Override
	public Rasterizer createRasterizer() {
		// Create a new context for this rasterizer
		int contextId = exports.createContext(surfaceId);
		if (contextId < 0) {
			throw new IllegalStateException("Failed to create context for surface " + surfaceId);
		}
		return new WasmRasterizer(this, contextId);
	}

	@Override
	public void resize(int width, int height) {
		if (surfaceId == -1) {
			throw new IllegalStateException("Surface has been destroyed");
		}

		int rc = exports.resetSurface(surfaceId, 0, width, height, pixelFormat);
		if (rc != 0) {
			throw new IllegalStateException("resetSurface failed: " + rc);
		}

		// we want to prevent as many calls into wasm as possible, so cache these values
		// instead of retrieving them each time
		// TODO: profile, see if this is worthwhile

		this.width = exports.getSurfaceWidth(this.surfaceId);
		this.height = exports.getSurfaceHeight(this.surfaceId);
		this.pixelsPtr = exports.getSurfacePixelsPtr(this.surfaceId);
		this.stride = exports.getSurfaceStride(this.surfaceId);

		pixelsView = new Uint8ClampedArray(this.memoryBuffer, this.pixelsPtr, this.stride * height);
	}

	@Override
	public int getWidth() {
		if (surfaceId == -1) {
			return 0;
		}
		return width;
	}

	@Override
	public int getHeight() {
		if (surfaceId == -1) {
			return 0;
		}
		return height;
	}

	@Override
	public Uint8ClampedArray getPixelData() {
		if (surfaceId == -1) {
			throw new IllegalStateException("Surface has been destroyed");
		}
		return pixelsView;
	}

	@Override
	public int getFormat() {
		return pixelFormat;
	}

	@Override
	public void destroy() {
		if (surfaceId != -1) {
			if (poolable && backend != null) {
				// Return to pool for reuse
				backend.releaseSurface(this);
			} else {
				// Direct destroy
				destroyInternal();
			}
		}
	}

	/**
	 * Destroy this surface without returning it to the pool.
	 * Used internally by the pool when evicting surfaces.
	 */
	void destroyDirect() {
		destroyInternal();
	}

	/**
	 * Internal method to actually destroy the surface.
	 */
	private void destroyInternal() {
		if (surfaceId != -1) {
			exports.resetSurface(this.surfaceId, 0, 0, 0, 0);
			surfaceId = -1;
		}
	}

	/**
	 * Set whether this surface should be pooled when destroyed.
	 * 
	 * @param poolable true to return to pool on destroy, false to destroy
	 *                 immediately
	 */
	public void setPoolable(boolean poolable) {
		this.poolable = poolable;
	}

	public int getId() {
		return surfaceId;
	}

	public WasmAwtRasterizerExports getExports() {
		return exports;
	}

	public boolean uploadFromSurface(Surface srcSurface) {
		if (srcSurface.getWidth() != this.getWidth() ||
				srcSurface.getHeight() != this.getHeight() ||
				srcSurface.getFormat() != this.getFormat()) {
			return false;
		}

		Uint8ClampedArray srcPixels = srcSurface.getPixelData();
		Uint8ClampedArray destPixels = this.getPixelData();

		destPixels.set(srcPixels);

		return true;
	}
}
