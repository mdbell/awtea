package me.mdbell.awtea.gfx;

import me.mdbell.awtea.classlib.java.awt.image.TColorModel;
import me.mdbell.awtea.classlib.java.awt.image.TWritableRaster;

public interface SurfaceBackend {

    Surface createCompatibleSurface(int width, int height, int bufferedImageType);

    Surface createCompatibleSurface(TColorModel cm,
                                    TWritableRaster raster,
                                    boolean isRasterPremultiplied);
}
