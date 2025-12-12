package me.mdbell.awtea.gfx.webgl;

import me.mdbell.awtea.gfx.Rasterizer;
import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.instrument.Monitored;
import org.teavm.jso.typedarrays.ArrayBufferView;
import org.teavm.jso.typedarrays.Uint8ClampedArray;
import org.teavm.jso.webgl.WebGL2RenderingContext;
import org.teavm.jso.webgl.WebGLFramebuffer;
import org.teavm.jso.webgl.WebGLRenderingContext;
import org.teavm.jso.webgl.WebGLTexture;

class WebGLSurface implements Surface {

	private final WebGLSurfaceBackend backend;
	private int width;
	private int height;
	WebGLTexture texture;
	WebGLFramebuffer framebuffer;
	private boolean forScreen;

	private boolean dirty = true;

	WebGLSurface(WebGLSurfaceBackend backend, int width, int height, boolean forScreen) {
		this.backend = backend;
		this.texture = backend.gl.createTexture();

		this.framebuffer = backend.gl.createFramebuffer();
		this.forScreen = forScreen;

		resize(width, height);
	}

	@Override
	public Rasterizer createRasterizer() {
		return new WebGLRasterizer(backend, this, forScreen);
	}

	@Override
	public void resize(int width, int height) {
		if (width == this.width && height == this.height) {
			return;
		}

		if (texture == null) {
			throw new IllegalStateException("Surface has been destroyed");
		}

		WebGL2RenderingContext gl = backend.gl;
		gl.bindTexture(WebGL2RenderingContext.TEXTURE_2D, texture);
		gl.texImage2D(WebGLRenderingContext.TEXTURE_2D, 0, WebGLRenderingContext.RGBA, width,
			height, 0, WebGLRenderingContext.RGBA,
			WebGLRenderingContext.UNSIGNED_BYTE, (ArrayBufferView) null);

		//TODO: reconsider texture parameters
		gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_MIN_FILTER, WebGLRenderingContext.NEAREST);
		gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_MAG_FILTER, WebGLRenderingContext.NEAREST);
		gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_WRAP_S, WebGLRenderingContext.CLAMP_TO_EDGE);
		gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_WRAP_T, WebGLRenderingContext.CLAMP_TO_EDGE);

		this.width = width;
		this.height = height;

		markDirty();
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	private Uint8ClampedArray pixelDataCache = null;

	private void updatePixelDataCache() {
		WebGL2RenderingContext gl = backend.gl;
		gl.bindFramebuffer(WebGL2RenderingContext.FRAMEBUFFER, framebuffer);
		gl.framebufferTexture2D(WebGL2RenderingContext.FRAMEBUFFER, WebGL2RenderingContext.COLOR_ATTACHMENT0,
			WebGL2RenderingContext.TEXTURE_2D, texture, 0);

		if (pixelDataCache == null || pixelDataCache.getLength() != width * height * 4) {
			pixelDataCache = new Uint8ClampedArray(width * height * 4);
		}

		gl.readPixels(0, 0, width, height, WebGL2RenderingContext.RGBA,
			WebGL2RenderingContext.UNSIGNED_BYTE, pixelDataCache);

		dirty = false;
	}

	@Monitored
	@Override
	public Uint8ClampedArray getPixelData() {
		if (dirty || pixelDataCache == null) {
			updatePixelDataCache();
		}
		return pixelDataCache;
	}

	void markDirty() {
		dirty = true;
	}

	@Override
	public int getFormat() {
		// WebGL surfaces are always RGBA
		return Surface.FORMAT_INT_RGBA;
	}

	@Override
	public void destroy() {
		if (texture != null) {
			backend.gl.deleteTexture(texture);
			texture = null;
		}
		if (framebuffer != null) {
			backend.gl.deleteFramebuffer(framebuffer);
			framebuffer = null;
		}
	}
}
