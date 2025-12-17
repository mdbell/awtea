package me.mdbell.awtea.gfx.wasm;

import lombok.Getter;
import me.mdbell.awtea.gfx.Rasterizer;
import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Uint8ClampedArray;

/**
 * WASM-backed surface implementation.
 * 
 * <h2>Thread Safety</h2>
 * This class is NOT inherently thread-safe. If you need to access a surface from multiple threads,
 * use {@code createReference()} to create independent references per thread. Each reference should
 * be destroyed by the thread that created it.
 * 
 * <h2>Resource Management</h2>
 * <ul>
 *   <li>Always call {@link #destroy()} when done to free WASM memory</li>
 *   <li>Destroy is idempotent - safe to call multiple times</li>
 *   <li>Surfaces not explicitly destroyed will trigger leak warnings in debug mode</li>
 * </ul>
 */
public final class WasmSurface implements Surface {
	private static final Logger log = LoggerFactory.getLogger(WasmSurface.class);
	
	// Track if this surface has been explicitly destroyed to avoid leak warnings
	private boolean explicitlyDestroyed = false;
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
			// Failed to create context - provide helpful error message
			WasmDiagnostics diag = backend.getDiagnostics();
			log.error("Failed to create context for surface {}: no free context IDs available. " +
					"Active: {} / {}, Utilization: {:.1f}%",
					surfaceId, diag.getActiveContextCount(), diag.getMaxContexts(),
					diag.getContextUtilization() * 100);
			throw new IllegalStateException(String.format(
					"Failed to create context for surface %d: no free context IDs available " +
					"(%d / %d contexts active). Consider disposing unused rasterizers or increasing MAX_CONTEXTS.",
					surfaceId, diag.getActiveContextCount(), diag.getMaxContexts()));
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
		// Idempotent: safe to call multiple times
		if (surfaceId == -1) {
			return;
		}
		
		explicitlyDestroyed = true;
		
		if (poolable && backend != null) {
			// Return to pool for reuse
			backend.releaseSurface(this);
		} else {
			// Direct destroy
			destroyInternal();
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
	 * Idempotent: safe to call multiple times.
	 */
	private void destroyInternal() {
		if (surfaceId == -1) {
			return;
		}
		
		explicitlyDestroyed = true;
		int id = surfaceId;
		surfaceId = -1;
		
		int result = exports.resetSurface(id, 0, 0, 0, 0);
		if (result != 0) {
			log.warn("destroyInternal: resetSurface returned error code {} for surface {}", result, id);
		}
	}
	
	/**
	 * Finalizer to detect resource leaks.
	 * Warns if surface was not explicitly destroyed.
	 */
	@Override
	protected void finalize() throws Throwable {
		try {
			if (surfaceId != -1 && !explicitlyDestroyed) {
				log.warn("WasmSurface {} was finalized without explicit destroy() - possible resource leak. " +
						"Always call destroy() when done with a surface. Size: {}x{}, Format: {}",
						surfaceId, width, height, pixelFormat);
				// Clean up even though we're in finalizer
				destroyInternal();
			}
		} finally {
			super.finalize();
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
