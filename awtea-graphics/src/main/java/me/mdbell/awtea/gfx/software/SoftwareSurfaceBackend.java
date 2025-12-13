package me.mdbell.awtea.gfx.software;

import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.gfx.SurfaceBackend;

import java.awt.image.*;

public class SoftwareSurfaceBackend implements SurfaceBackend {

	public SoftwareSurfaceBackend() {

	}

	@Override
	public Surface createCompatibleSurface(int width, int height, int bufferedImageType) {
		int format = Surface.fromBufferedImageType(bufferedImageType);
		if (format == -1) {
			return null;
		}

		// Create a new WritableRaster and ColorModel for the given dimensions
		WritableRaster raster;
		ColorModel cm;

		switch (bufferedImageType) {
			case BufferedImage.TYPE_INT_ARGB: {
				int[] masks = {0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000}; // R, G, B, A masks for 0xAARRGGBB
				SinglePixelPackedSampleModel sm = new SinglePixelPackedSampleModel(
					DataBuffer.TYPE_INT, width, height, masks);
				DataBufferInt db = new DataBufferInt(width * height);
				raster = Raster.createWritableRaster(sm, db, null);
				cm = new DirectColorModel(32, masks[0], masks[1], masks[2], masks[3]);
				break;
			}
			case BufferedImage.TYPE_INT_RGB: {
				int[] masks = {0x00FF0000, 0x0000FF00, 0x000000FF}; // R, G, B masks for 0x00RRGGBB
				SinglePixelPackedSampleModel sm = new SinglePixelPackedSampleModel(
					DataBuffer.TYPE_INT, width, height, masks);
				DataBufferInt db = new DataBufferInt(width * height);
				raster = Raster.createWritableRaster(sm, db, null);
				cm = new DirectColorModel(24, masks[0], masks[1], masks[2]);
				break;
			}
			case BufferedImage.TYPE_INT_BGR: {
				// R, G, B masks for 0x00BBGGRR format (B in bits 16-23, G in bits 8-15, R in bits 0-7)
				int[] masks = {0x000000FF, 0x0000FF00, 0x00FF0000};
				SinglePixelPackedSampleModel sm = new SinglePixelPackedSampleModel(
					DataBuffer.TYPE_INT, width, height, masks);
				DataBufferInt db = new DataBufferInt(width * height);
				raster = Raster.createWritableRaster(sm, db, null);
				cm = new DirectColorModel(24, masks[0], masks[1], masks[2]);
				break;
			}
			default:
				return null;
		}

		return new SoftwareSurface(raster, cm, format);
	}

	@Override
	public Surface createCompatibleSurface(Object cm, Object raster, boolean isRasterPremultiplied, int bufferedImageType) {
		int format = Surface.fromBufferedImageType(bufferedImageType);
		if (format == -1) {
			return null;
		}
		return new SoftwareSurface((java.awt.image.WritableRaster) raster,
			(java.awt.image.ColorModel) cm,
			format);
	}

	@Override
	public Surface createFontRenderSurface(int width, int height) {
		// Use TYPE_INT_ARGB for text rendering to support alpha transparency
		return createCompatibleSurface(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
	}
}
