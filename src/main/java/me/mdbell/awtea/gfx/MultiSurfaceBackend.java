package me.mdbell.awtea.gfx;

import me.mdbell.awtea.gfx.software.SoftwareSurfaceBackend;
import me.mdbell.awtea.gfx.wasm.WasmSurfaceBackend;

public class MultiSurfaceBackend implements SurfaceBackend {

    private final SurfaceBackend[] backends;

    private static MultiSurfaceBackend instance = null;

    private MultiSurfaceBackend() {
        this.backends = new SurfaceBackend[]{
                new WasmSurfaceBackend(),
                new SoftwareSurfaceBackend(),
        };
    }

    public MultiSurfaceBackend(SurfaceBackend[] backends) {
        this.backends = backends;
    }

    public static MultiSurfaceBackend getDefault() {
        if (instance == null) {
            instance = new MultiSurfaceBackend();
        }
        return instance;
    }

    public static void setDefault(MultiSurfaceBackend backend) {
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
    public Surface createCompatibleSurface(Object cm, Object raster, boolean isRasterPremultiplied) {
        for (SurfaceBackend backend : backends) {
            Surface surface = backend.createCompatibleSurface(cm, raster, isRasterPremultiplied);
            if (surface != null) {
                return surface;
            }
        }
        return null;
    }
}
