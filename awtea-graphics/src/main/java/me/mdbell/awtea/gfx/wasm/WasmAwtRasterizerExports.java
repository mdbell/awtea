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

	@JSMethod("get_command_size")
	int getSurfaceCommandSize();

	@JSMethod("request_command_buffer")
	int requestCommandBuffer(int commandCount);

	// int reset_surface(int surface_id, int layer, int width, int height, int pixel_format);
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

	@JSMethod("get_max_context_commands")
	int getMaxContextCommands();

	@JSMethod("get_context_command_buffer_ptr")
	int getContextCommandBufferPtr(int contextId);

	@JSMethod("free_pixels")
	void freePixels(int ptr);

	// int render_awt(int context_id, uint32_t cmdPtr, int cmdCount);
	@JSMethod("render_awt")
	int renderAwt(int contextId, int commandsPtr, int commandCount);
}
