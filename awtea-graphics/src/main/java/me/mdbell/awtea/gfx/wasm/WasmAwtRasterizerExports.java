package me.mdbell.awtea.gfx.wasm;

import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 * Low-level view of the AWT rasterizer WASM exports.
 * This should mirror the C exports in awt_surface.c
 */
interface WasmAwtRasterizerExports extends JSObject {

	// exported "memory"
	@JSProperty("memory")
	WasmMemory getMemory();

	// Initialization
	@JSMethod("init_surface_system")
	void initSurfaceSystem();

	@JSMethod("find_free_surface")
	int findFreeSurfaceId();

	// int reset_surface(int surface_id, int layer, int width, int height, int
	// pixel_format);
	@JSMethod("reset_surface")
	int resetSurface(int surfaceId, int layer, int width, int height, int pixelFormat);

	// uint32_t get_surface_pixels_ptr(int surface_id);
	@JSMethod("get_surface_pixels_ptr")
	int getSurfacePixelsPtr(int surfaceId);

	@JSMethod("get_surface_width")
	int getSurfaceWidth(int surfaceId);

	@JSMethod("get_surface_height")
	int getSurfaceHeight(int surfaceId);

	@JSMethod("get_surface_stride")
	int getSurfaceStride(int surfaceId);

	// Context management
	@JSMethod("find_free_context")
	int findFreeContextId();

	@JSMethod("create_context")
	int createContext(int surfaceId);

	@JSMethod("clone_context")
	int cloneContext(int contextId);

	@JSMethod("destroy_context")
	int destroyContext(int contextId);

	@JSMethod("create_reference")
	int createReference(int surfaceId);

	@JSMethod("release_reference")
	int releaseReference(int surfaceId);

	@JSMethod("get_context_surface_id")
	int getContextSurfaceId(int contextId);

	@JSMethod("get_context_buffer_size_words")
	int getContextBufferSizeWords();

	@JSMethod("get_context_buffer_ptr")
	int getContextBufferPtr(int contextId);

	@JSMethod("free_pixels")
	void freePixels(int ptr);

	// int render_awt(int context_id, uint32_t cmdPtr, int bytesUsed);
	// Note: bytesUsed parameter is now in bytes (not command count)
	@JSMethod("render_awt")
	int renderAwt(int contextId, int commandsPtr, int bytesUsed);

	// Stack tracking exports
	@JSMethod("get_stack_buffer_ptr")
	int getStackBufferPtr();

	@JSMethod("get_stack_depth")
	int getStackDepth();

	@JSMethod("get_max_stack_depth")
	int getMaxStackDepth();
}
