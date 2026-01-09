package me.mdbell.awtea.gfx;

import lombok.Getter;

/**
 * A composite surface backend that implements the composite pattern with fallback support.
 *
 * <p>This backend tries each configured backend in order until one succeeds.
 * This allows for automatic fallback from WASM to Software rendering if WASM fails to load.
 *
 * <p>Example usage:
 * <pre>{@code
 * SurfaceBackend[] backends = new SurfaceBackend[] {
 *     new WasmSurfaceBackend(),
 *     new SoftwareSurfaceBackend()
 * };
 * SurfaceBackend composite = new CompositeSurfaceBackend(backends);
 *
 * // This will try WASM first, then fall back to Software
 * Surface surface = composite.createCompatibleSurface(800, 600, Surface.FORMAT_INT_ARGB);
 * }</pre>
 *
 * <p>Note: For most use cases, prefer using {@link SurfaceBackendFactory} methods
 * instead of creating instances directly.
 */
@Getter
public class CompositeSurfaceBackend implements SurfaceBackend {

    private final SurfaceBackend[] backends;

    /**
     * Creates a new composite backend with the specified backends.
     * Backends are tried in the order provided.
     *
     * @param backends the backends to try in order
     */
    public CompositeSurfaceBackend(SurfaceBackend[] backends) {
        this.backends = backends;
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
     * Create a surface suitable for rendering text/fonts.
     *
     * <p>Note: This method currently forces software rendering because the WASM backend
     * has issues with font rendering. This is a temporary workaround.
     *
     * @param width  The width of the surface in pixels
     * @param height The height of the surface in pixels
     * @return The created surface, or null if no backend can create the surface
     */
    @Override
    public Surface createFontRenderSurface(int width, int height) {
        // TODO: the wasm backend does not like the font render code, so for now we force software
        // Use the last backend in the array (typically the software fallback)
        if (backends.length > 0) {
            SurfaceBackend lastBackend = backends[backends.length - 1];
            return lastBackend.createFontRenderSurface(width, height);
        }
        return null;

        // Future implementation when WASM font rendering is fixed:
        // for (SurfaceBackend backend : backends) {
        //     Surface surface = backend.createFontRenderSurface(width, height);
        //     if (surface != null) {
        //         return surface;
        //     }
        // }
        // return null;
    }
}
