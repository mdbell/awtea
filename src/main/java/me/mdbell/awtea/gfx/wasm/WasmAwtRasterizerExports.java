package me.mdbell.awtea.gfx.wasm;

import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 * Low-level view of the AWT rasterizer WASM exports.
 * This should mirror the C exports in awt_image.c and awt_surface.c
 */
interface WasmAwtRasterizerExports extends JSObject {

	// exported "memory"
	@JSProperty("memory")
	WasmMemory getMemory();

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

	// int register_image(int format, int width, int height, int stride)
	@JSMethod("register_image")
	int registerImage(int format, int width, int height, int stride);

	@JSMethod("get_image_pixels_ptr")
	int getImagePixelsPtr(int imageId);

	@JSMethod("free_pixels")
	void freePixels(int ptr);

	// int render_awt(int surface_id, uint32_t cmdPtr, int cmdCount);
	@JSMethod("render_awt")
	int renderAwt(int surfaceId, int commandsPtr, int commandCount);
}
