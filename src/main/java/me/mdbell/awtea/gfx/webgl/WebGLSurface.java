package me.mdbell.awtea.gfx.webgl;

import me.mdbell.awtea.gfx.Rasterizer;
import me.mdbell.awtea.gfx.Surface;
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
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public Uint8ClampedArray getPixelData() {
		return null;
	}

	@Override
	public int getFormat() {
		// WebGL surfaces are always RGBA - mapped to INT_ARGB in our format system
		return Surface.FORMAT_INT_ARGB;
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
