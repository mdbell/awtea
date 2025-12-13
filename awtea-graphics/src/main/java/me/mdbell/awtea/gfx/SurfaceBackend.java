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

	/**
	 * Create a surface suitable for rendering text/fonts.
	 * This method allows backends to optimize surface creation for font rendering.
	 * If a backend cannot create a font render surface, it should return null,
	 * and the caller should fall back to the next available backend.
	 *
	 * @param width  The width of the surface in pixels
	 * @param height The height of the surface in pixels
	 * @return The created surface, or null if this backend cannot provide a font render surface
	 */
	default Surface createFontRenderSurface(int width, int height) {
		// Default implementation delegates to createCompatibleSurface with ARGB format
		// for text rendering with alpha transparency
		return createCompatibleSurface(width, height, Surface.FORMAT_INT_ARGB);
	}
}
