package me.mdbell.awtea.gfx;

import me.mdbell.awtea.gfx.software.SoftwareSurfaceBackend;
import me.mdbell.awtea.gfx.wasm.WasmSurfaceBackend;
import me.mdbell.awtea.gfx.webgl.WebGLSurfaceBackend;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.dom.html.HTMLCanvasElement;

public class DefaultSurfaceBackend implements SurfaceBackend {

    private static final Logger log = LoggerFactory.getLogger(DefaultSurfaceBackend.class);

    private final SurfaceBackend[] backends;

    private static DefaultSurfaceBackend instance = null;

    private SurfaceBackend defaultBackend;

    /**
     * System property to force a specific rendering backend.
     * Valid values: "wasm", "webassembly", "software", "java"
     * Example: -Dme.mdbell.awtea.gfx.backend=software
     */
    private static final String BACKEND_PROPERTY = "me.mdbell.awtea.gfx.backend";

    private DefaultSurfaceBackend() {
        this.defaultBackend = new SoftwareSurfaceBackend();
        this.backends = createBackends(defaultBackend);
    }

    public DefaultSurfaceBackend(SurfaceBackend[] backends) {
        this.backends = backends;
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

    /**
     * Create a surface suitable for rendering text/fonts.
     * Tries each backend in priority order (WASM > Software), using the first one that succeeds.
     * This ensures that if WASM fails to load or errors occur, we fall back to software rendering.
     *
     * @param width  The width of the surface in pixels
     * @param height The height of the surface in pixels
     * @return The created surface, or null if no backend can create the surface
     */
    @Override
    public Surface createFontRenderSurface(int width, int height) {
        //TODO: the wasm backend does not like the font render code, so for now we force software
        return defaultBackend.createFontRenderSurface(width, height);
//		for (SurfaceBackend backend : backends) {
//			Surface surface = backend.createFontRenderSurface(width, height);
//			if (surface != null) {
//				return surface;
//			}
//		}
    }
}
