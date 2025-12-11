package me.mdbell.awtea.gfx.software;

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
	public Surface createCompatibleSurface(Object cm, Object raster, boolean isRasterPremultiplied, int bufferedImageType) {
		return new SoftwareSurface((java.awt.image.WritableRaster) raster,
			(java.awt.image.ColorModel) cm,
			bufferedImageType);
	}
}
