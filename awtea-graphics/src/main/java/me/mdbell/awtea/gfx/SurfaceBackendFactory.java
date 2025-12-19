package me.mdbell.awtea.gfx;

import me.mdbell.awtea.gfx.software.SoftwareSurfaceBackend;
import me.mdbell.awtea.gfx.wasm.WasmSurfaceBackend;
import me.mdbell.awtea.gfx.webgl.WebGLSurfaceBackend;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.dom.html.HTMLCanvasElement;

/**
 * Factory class for creating and managing SurfaceBackend instances.
 * 
 * <p>This factory provides static methods for creating backend instances with
 * automatic fallback support and system property configuration.
 * 
 * <p>The factory supports the following system property for configuration:
 * <ul>
 *   <li>{@code me.mdbell.awtea.gfx.backend} - Force a specific rendering backend
 *       (valid values: "wasm", "webassembly", "software", "java")</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Get the default backend with automatic WASM > Software fallback
 * SurfaceBackend backend = SurfaceBackendFactory.getDefault();
 * 
 * // Get a specific backend type
 * SurfaceBackend wasmBackend = SurfaceBackendFactory.getWasmBackend();
 * SurfaceBackend softwareBackend = SurfaceBackendFactory.getSoftwareBackend();
 * 
 * // Get a backend suitable for specific use cases
 * SurfaceBackend textBackend = SurfaceBackendFactory.getTextBackend();
 * SurfaceBackend screenBackend = SurfaceBackendFactory.getScreenBackend(canvasElement);
 * }</pre>
 */
public class SurfaceBackendFactory {
	
	private static final Logger log = LoggerFactory.getLogger(SurfaceBackendFactory.class);
	
	/**
	 * System property to force a specific rendering backend.
	 * Valid values: "wasm", "webassembly", "software", "java"
	 * Example: -Dme.mdbell.awtea.gfx.backend=software
	 */
	private static final String BACKEND_PROPERTY = "me.mdbell.awtea.gfx.backend";
	
	private static SurfaceBackend defaultInstance = null;
	private static SurfaceBackend wasmInstance = null;
	private static SurfaceBackend softwareInstance = null;
	
	private SurfaceBackendFactory() {
		// Utility class - prevent instantiation
	}
	
	/**
	 * Get the default backend with automatic WASM > Software fallback.
	 * The default backend is a singleton that respects the system property configuration.
	 * 
	 * @return the default surface backend instance
	 */
	public static SurfaceBackend getDefault() {
		if (defaultInstance == null) {
			SoftwareSurfaceBackend defaultBackend = new SoftwareSurfaceBackend();
			SurfaceBackend[] backends = createBackends(defaultBackend);
			defaultInstance = new CompositeSurfaceBackend(backends);
		}
		return defaultInstance;
	}
	
	/**
	 * Set the default backend instance.
	 * This allows programmatic override of the default backend.
	 * 
	 * @param backend the backend to use as default
	 */
	public static void setDefault(SurfaceBackend backend) {
		defaultInstance = backend;
	}
	
	/**
	 * Get a software-only renderer backend.
	 * This backend uses pure Java implementation without hardware acceleration.
	 * Returns a singleton instance for efficiency.
	 * 
	 * @return the software surface backend instance
	 */
	public static SurfaceBackend getSoftwareBackend() {
		if (softwareInstance == null) {
			softwareInstance = new SoftwareSurfaceBackend();
		}
		return softwareInstance;
	}
	
	/**
	 * Get a WASM renderer backend.
	 * This backend uses native C code compiled to WebAssembly for high-performance rendering.
	 * Returns a singleton instance for efficiency.
	 * 
	 * @return the WASM surface backend instance
	 * @throws RuntimeException if the WASM module fails to load
	 */
	public static SurfaceBackend getWasmBackend() {
		if (wasmInstance == null) {
			wasmInstance = new WasmSurfaceBackend();
		}
		return wasmInstance;
	}
	
	/**
	 * Get a WebGL renderer backend for a specific canvas element.
	 * This backend uses hardware-accelerated WebGL rendering.
	 * 
	 * @param canvas the HTML canvas element to render to
	 * @return a new WebGL surface backend instance
	 */
	public static SurfaceBackend getWebGLBackend(HTMLCanvasElement canvas) {
		return new WebGLSurfaceBackend(canvas);
	}
	
	/**
	 * Get a backend suitable for text/font rendering.
	 * Currently returns the software backend as WASM has issues with font rendering.
	 * 
	 * @return a backend suitable for text rendering
	 */
	public static SurfaceBackend getTextBackend() {
		return getSoftwareBackend();
	}
	
	/**
	 * Get a backend suitable for screen rendering to a canvas element.
	 * This creates a WebGL backend for hardware-accelerated screen rendering.
	 * 
	 * @param canvas the HTML canvas element to render to
	 * @return a WebGL backend configured for screen rendering
	 */
	public static SurfaceBackend getScreenBackend(HTMLCanvasElement canvas) {
		return getWebGLBackend(canvas);
	}
	
	/**
	 * Create a surface where the rasterizer can render directly to the screen.
	 * 
	 * <p>getPixelData() should return null, and the rasterizer should not attempt to read from it.
	 * This is a convenience method that creates a WebGL backend and surface for screen rendering.
	 * 
	 * @param width  The width of the surface
	 * @param height The height of the surface
	 * @param canvas The HTMLCanvasElement to render to
	 * @return The created surface
	 */
	public static Surface createScreenSurface(int width, int height, HTMLCanvasElement canvas) {
		WebGLSurfaceBackend webGLBackend = new WebGLSurfaceBackend(canvas);
		return webGLBackend.createCompatibleSurface(width, height, Surface.FORMAT_INT_RGBA);
	}
	
	/**
	 * Create a composite backend with custom fallback order.
	 * The backends are tried in the order provided, using the first one that succeeds.
	 * 
	 * @param backends the backends to try in order
	 * @return a composite backend with the specified fallback order
	 */
	public static SurfaceBackend createComposite(SurfaceBackend... backends) {
		return new CompositeSurfaceBackend(backends);
	}
	
	/**
	 * Get the WASM backend instance if it has been initialized.
	 * This is useful for accessing WASM diagnostics when the WASM backend is in use.
	 * 
	 * @return the WasmSurfaceBackend instance if initialized, or null otherwise
	 */
	public static WasmSurfaceBackend getWasmBackendFromDefault() {
		if (wasmInstance instanceof WasmSurfaceBackend) {
			return (WasmSurfaceBackend) wasmInstance;
		}
		return null;
	}
	
	/**
	 * Creates the backend array based on system property configuration.
	 * If me.mdbell.awtea.gfx.backend is set, only that backend is used.
	 * Otherwise, uses default priority: WASM > Software.
	 */
	private static SurfaceBackend[] createBackends(SurfaceBackend defaultBackend) {
		String forcedBackend = System.getProperty(BACKEND_PROPERTY);
		
		if (forcedBackend != null && !forcedBackend.isEmpty()) {
			forcedBackend = forcedBackend.toLowerCase().trim();
			
			switch (forcedBackend) {
				case "wasm":
				case "webassembly":
					log.info("Forcing WASM backend via system property");
					return new SurfaceBackend[]{new WasmSurfaceBackend()};
				case "software":
				case "java":
					log.info("Forcing Software backend via system property");
					return new SurfaceBackend[]{new SoftwareSurfaceBackend()};
				default:
					log.error("Unknown backend '{}", forcedBackend + "' specified in " +
							BACKEND_PROPERTY + ". Using default priority.");
					break;
			}
		}
		
		// Default priority: WASM > Software
		// Try to create each backend individually and only include successful ones
		java.util.ArrayList<SurfaceBackend> backendList = new java.util.ArrayList<>();
		
		// Try WASM backend first
		try {
			WasmSurfaceBackend wasmBackend = new WasmSurfaceBackend();
			backendList.add(wasmBackend);
		} catch (Exception e) {
			// WASM backend failed to load (e.g., wasm file not found on server)
			// Continue without it - will use software fallback
			log.warn("Failed to load WASM backend, will use software renderer: {}", e.getMessage());
		}
		
		backendList.add(defaultBackend);
		
		return backendList.toArray(new SurfaceBackend[0]);
	}
}
