package me.mdbell.awtea.gfx;

import me.mdbell.awtea.gfx.software.SoftwareSurfaceBackend;
import me.mdbell.awtea.gfx.wasm.WasmSurfaceBackend;
import me.mdbell.awtea.gfx.webgl.WebGLSurfaceBackend;
import org.teavm.jso.dom.html.HTMLCanvasElement;

public class DefaultSurfaceBackend implements SurfaceBackend {

	private final SurfaceBackend[] backends;

	private static DefaultSurfaceBackend instance = null;

	/**
	 * System property to force a specific rendering backend.
	 * Valid values: "wasm", "software"
	 * Example: -Dme.mdbell.awtea.gfx.backend=software
	 */
	private static final String BACKEND_PROPERTY = "me.mdbell.awtea.gfx.backend";

	private DefaultSurfaceBackend() {
		this.backends = createBackends();
	}

	public DefaultSurfaceBackend(SurfaceBackend[] backends) {
		this.backends = backends;
	}

	/**
	 * Creates the backend array based on system property configuration.
	 * If me.mdbell.awtea.gfx.backend is set, only that backend is used.
	 * Otherwise, uses default priority: WASM > Software.
	 */
	private static SurfaceBackend[] createBackends() {
		String forcedBackend = System.getProperty(BACKEND_PROPERTY);
		
		if (forcedBackend != null && !forcedBackend.isEmpty()) {
			forcedBackend = forcedBackend.toLowerCase().trim();
			
			switch (forcedBackend) {
				case "wasm":
				case "webassembly":
					System.out.println("Forcing WASM backend via system property");
					return new SurfaceBackend[]{new WasmSurfaceBackend()};
				case "software":
				case "java":
					System.out.println("Forcing Software backend via system property");
					return new SurfaceBackend[]{new SoftwareSurfaceBackend()};
				default:
					System.err.println("Unknown backend '" + forcedBackend + "' specified in " + 
						BACKEND_PROPERTY + ". Using default priority.");
					break;
			}
		}
		
		// Default priority: WASM > Software
		return new SurfaceBackend[]{
			new WasmSurfaceBackend(),
			new SoftwareSurfaceBackend(),
		};
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
		return webGLBackend.createCompatibleSurface(width, height, Surface.FORMAT_INT_RGBA);
	}
}
