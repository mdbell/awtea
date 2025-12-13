package me.mdbell.awtea.gfx;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import org.teavm.jso.typedarrays.Uint8ClampedArray;

import java.awt.image.BufferedImage;

public interface Surface {

	// See PixelFormat enum in awt_raster_internal.h for the source of truth
	int FORMAT_INT_ARGB = 0;
	int FORMAT_INT_RGB = 1;
	int FORMAT_INT_RGBA = 2; // Not directly supported via BufferedImage types, but commonly used for WebGL
	int FORMAT_INT_ABGR = 3;
	int FORMAT_INT_BGR = 4;

	int MIN_FORMAT = FORMAT_INT_ARGB;
	int MAX_FORMAT = FORMAT_INT_BGR;

	static boolean isValidPixelFormat(int type) {
		return type >= MIN_FORMAT && type <= MAX_FORMAT;
	}

	static int toBufferedImageType(int pixelFormat) {
		switch (pixelFormat) {
			case FORMAT_INT_ARGB:
				return BufferedImage.TYPE_INT_ARGB;
			case FORMAT_INT_RGB:
				return BufferedImage.TYPE_INT_RGB;
			case FORMAT_INT_BGR:
				return BufferedImage.TYPE_INT_BGR;
			case FORMAT_INT_RGBA:
				return BufferedImage.TYPE_INT_ARGB; // Closest match
			default:
				LoggerFactory.getLogger(Surface.class).error("Surface.toBufferedImageType: Unsupported pixel format: {}", pixelFormat);
				return BufferedImage.TYPE_CUSTOM;
		}
	}

	static int fromBufferedImageType(int bufferedImageType) {
		switch (bufferedImageType) {
			case BufferedImage.TYPE_INT_ARGB:
			case BufferedImage.TYPE_INT_ARGB_PRE:
				return FORMAT_INT_ARGB;
			case BufferedImage.TYPE_INT_RGB:
				return FORMAT_INT_RGB;
			case BufferedImage.TYPE_INT_BGR:
				return FORMAT_INT_BGR;
			default:
				LoggerFactory.getLogger(Surface.class).error("Surface.fromBufferedImageType: Unsupported BufferedImage type: {}", bufferedImageType);
				return -1;
		}
	}

	/**
	 * Creates a new rasterizer for drawing to this surface.
	 *
	 * @return A new TRasterizer instance.
	 */
	Rasterizer createRasterizer();

	/**
	 * Resizes this surface to the specified width and height.
	 * Resizing does not preserve the existing pixel data, and
	 * the contents of the surface after resizing are undefined.
	 *
	 * @param width  The new width in pixels.
	 * @param height The new height in pixels.
	 */
	void resize(int width, int height);

	/**
	 * Gets the width of this surface in pixels.
	 *
	 * @return The width of the surface.
	 */
	int getWidth();

	/**
	 * Gets the height of this surface in pixels.
	 *
	 * @return The height of the surface.
	 */
	int getHeight();

	/**
	 * Gets a Uint8ClampedArray view of the pixel data for this surface.
	 * Note: this should be a direct view of the underlying pixel data, so modifying
	 * the contents of the array will modify the surface's pixels.
	 *
	 * @return A Uint8ClampedArray containing the pixel data.
	 */
	Uint8ClampedArray getPixelData();

	int getFormat();

	/**
	 * Indicates whether the surface has been modified since the last check.
	 * This can be used to determine if the surface needs to be re-uploaded
	 * to a texture or otherwise updated in rendering.
	 *
	 * @return true if the surface is dirty (modified), false otherwise.
	 */
	default boolean isDirty() {
		return true;
	}

	/**
	 * Frees any resources associated with this surface.
	 */
	void destroy();
}
