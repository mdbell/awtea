package me.mdbell.awtea.gfx;

import me.mdbell.awtea.gfx.software.SoftwareSurfaceBackend;
import me.mdbell.awtea.gfx.wasm.WasmSurfaceBackend;
import me.mdbell.awtea.gfx.webgl.WebGLSurfaceBackend;
import org.teavm.jso.dom.html.HTMLCanvasElement;

import java.awt.image.BufferedImage;

public class DefaultSurfaceBackend implements SurfaceBackend {

	private final SurfaceBackend[] backends;

	private static DefaultSurfaceBackend instance = null;

	private DefaultSurfaceBackend() {
		this.backends = new SurfaceBackend[]{
			new WasmSurfaceBackend(),
			new SoftwareSurfaceBackend(),
		};
	}

	public DefaultSurfaceBackend(SurfaceBackend[] backends) {
		this.backends = backends;
	}

	public static DefaultSurfaceBackend getDefault() {
		if (instance == null) {
			instance = new DefaultSurfaceBackend();
		}
		return instance;
	}

	public static void setDefault(DefaultSurfaceBackend backend) {
		instance = backend;
	}

	@Override
	public Surface createCompatibleSurface(int width, int height, int bufferedImageType) {
		for (SurfaceBackend backend : backends) {
			Surface surface = backend.createCompatibleSurface(width, height, bufferedImageType);
			if (surface != null) {
				return surface;
			}
		}
		return null;
	}

	@Override
	public Surface createCompatibleSurface(Object cm, Object raster, boolean isRasterPremultiplied, int bufferedImageType) {
		for (SurfaceBackend backend : backends) {
			Surface surface = backend.createCompatibleSurface(cm, raster, isRasterPremultiplied, bufferedImageType);
			if (surface != null) {
				return surface;
			}
		}
		return null;
	}

	/**
	 * Should return a surface where the rasterizer can render directly to the screen.
	 * <p>
	 * getPixelData() should return null, and the rasterizer should not attempt to read from it.
	 *
	 * @param width  The width of the surface
	 * @param height The height of the surface
	 * @param canvas The HTMLCanvasElement to render to
	 * @return The created surface
	 */
	public Surface createScreenSurface(int width, int height, HTMLCanvasElement canvas) {
		WebGLSurfaceBackend webGLBackend = new WebGLSurfaceBackend(canvas);
		return webGLBackend.createCompatibleSurface(width, height, BufferedImage.TYPE_INT_ARGB);
	}
}
