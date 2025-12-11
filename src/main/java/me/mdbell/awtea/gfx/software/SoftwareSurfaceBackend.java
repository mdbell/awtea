package me.mdbell.awtea.gfx.software;

import me.mdbell.awtea.classlib.java.awt.image.TColorModel;
import me.mdbell.awtea.classlib.java.awt.image.TWritableRaster;
import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.gfx.SurfaceBackend;

public class SoftwareSurfaceBackend implements SurfaceBackend {

    public SoftwareSurfaceBackend() {

    }

    @Override
    public Surface createCompatibleSurface(int width, int height, int bufferedImageType) {
        return null;
    }

    @Override
    public Surface createCompatibleSurface(TColorModel cm, TWritableRaster raster, boolean isRasterPremultiplied) {
        return null;
    }
}
