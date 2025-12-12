package me.mdbell.awtea.gfx;

public interface SurfaceBackend {

	/**
	 * Create a surface compatible with this backend
	 *
	 * @param width             The width of the surface
	 * @param height            The height of the surface
	 * @param bufferedImageType The BufferedImage type
	 * @return The created surface, or null if the type is not supported
	 */
	Surface createCompatibleSurface(int width, int height, int bufferedImageType);

	/**
	 * Create a surface compatible with this backend
	 *
	 * @param cm                    TColorModel of the surface
	 * @param raster                TWritableRaster of the surface
	 * @param isRasterPremultiplied Whether the raster is premultiplied
	 * @param bufferedImageType     The BufferedImage type (helper, can be derived from cm/raster)
	 * @return The created surface, or null if the type is not supported
	 */
	Surface createCompatibleSurface(Object cm,
									Object raster,
									boolean isRasterPremultiplied, int bufferedImageType);
}
