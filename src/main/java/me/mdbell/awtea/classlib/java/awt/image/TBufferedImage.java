package me.mdbell.awtea.classlib.java.awt.image;

import lombok.AccessLevel;
import lombok.Getter;
import me.mdbell.awtea.classlib.java.awt.TImage;
import me.mdbell.awtea.classlib.java.awt.color.TColorSpace;
import me.mdbell.awtea.support.ImageDataConsumer;
import me.mdbell.awtea.support.ImageDataProvider;
import me.mdbell.awtea.util.GlyphRasterizer;
import org.teavm.classlib.java.awt.TPoint;
import org.teavm.jso.canvas.ImageData;
import org.teavm.jso.typedarrays.Uint8ClampedArray;

import java.awt.*;
import java.util.Hashtable;

/**
 * @see java.awt.image.BufferedImage
 */
@Getter
public class TBufferedImage extends TImage implements GlyphRasterizer.RasterTarget, ImageDataProvider, ImageDataConsumer {

	public static final int TYPE_CUSTOM = 0;
	public static final int TYPE_INT_RGB = 1;
	public static final int TYPE_INT_ARGB = 2;
	public static final int TYPE_INT_ARGB_PRE = 3;

	private static final int DCM_RED_MASK = 0x00FF0000;
	private static final int DCM_GREEN_MASK = 0x0000FF00;
	private static final int DCM_BLUE_MASK = 0x000000FF;
	private static final int DCM_ALPHA_MASK = 0xFF000000;


	private final int width;
	private final int height;
	private final int imageType;

	private final TColorModel colorModel;
	private final TWritableRaster raster;
	private final boolean alphaPremultiplied;

	@Getter(AccessLevel.NONE)
	private TSoftwareGraphics gfx;

	@SuppressWarnings("rawtypes")
	private final Hashtable properties;

	@Getter(AccessLevel.NONE)
	private ImageData cachedImageData;
	@Getter(AccessLevel.NONE)
	private boolean dirty = true;

	static {
		new Color(255, 0, 0); // Force Color class to initialize
	}

	public TBufferedImage(int width, int height) {
		this(width, height, TYPE_INT_RGB);
	}

	public TBufferedImage(int width, int height, int imageType) {
		this(width, height, imageType, null);
	}

	@SuppressWarnings("rawtypes")
	public TBufferedImage(int width, int height, int imageType, Hashtable properties) {
		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException("width/height must be > 0");
		}

		this.width = width;
		this.height = height;
		this.imageType = imageType;
		this.properties = properties;

		switch (imageType) {
			case TYPE_INT_ARGB: {
				// A,R,G,B in 0xAARRGGBB layout
				int[] masks = {
					0x00FF0000, // R
					0x0000FF00, // G
					0x000000FF, // B
					0xFF000000  // A
				};

				TSinglePixelPackedSampleModel sm =
					new TSinglePixelPackedSampleModel(
						TDataBuffer.TYPE_INT,
						width,
						height,
						width, // scanlineStride == width for packed
						masks
					);

				TDataBufferInt db = new TDataBufferInt(width * height);

				this.raster = TWritableRaster.createWritableRaster(sm, db, new TPoint(0, 0));

				this.colorModel = new TDirectColorModel(
					32,
					masks[0],
					masks[1],
					masks[2],
					masks[3]
				);

				this.alphaPremultiplied = false;
				break;
			}

			case TYPE_INT_RGB: {
				// No alpha; treat as opaque RGB
				int[] masks = {
					0x00FF0000, // R
					0x0000FF00, // G
					0x000000FF  // B
				};

				TSinglePixelPackedSampleModel sm =
					new TSinglePixelPackedSampleModel(
						TDataBuffer.TYPE_INT,
						width,
						height,
						width,
						masks
					);

				TDataBufferInt db = new TDataBufferInt(width * height);

				this.raster = TWritableRaster.createWritableRaster(sm, db, new TPoint(0, 0));

				// Alpha mask = 0 → no alpha
				this.colorModel = new TDirectColorModel(
					32,
					masks[0],
					masks[1],
					masks[2],
					0           // no alpha mask
				);

				this.alphaPremultiplied = false;
				break;
			}

			default:
				// For now, we only fully support INT_RGB/INT_ARGB.
				// You can add more imageTypes later.
				throw new IllegalArgumentException("Unsupported imageType: " + imageType);
		}
	}

	@SuppressWarnings("rawtypes")
	public TBufferedImage(TColorModel cm,
						  TWritableRaster raster,
						  boolean isRasterPremultiplied,
						  Hashtable properties) {
		if (cm == null || raster == null) {
			throw new NullPointerException("ColorModel and Raster must not be null");
		}

		this.colorModel = cm;
		this.raster = raster;
		this.alphaPremultiplied = isRasterPremultiplied;
		this.properties = properties;

		this.width = raster.getWidth();
		this.height = raster.getHeight();

		this.imageType = inferImageType(cm, raster, alphaPremultiplied);
	}

	private static int inferImageType(TColorModel cm,
									  TWritableRaster raster,
									  boolean isRasterPremultiplied) {

		TColorSpace cs = cm.getColorSpace();
		int csType = cs.getType();

		if (csType != TColorSpace.TYPE_RGB) {
			return TYPE_CUSTOM;
		}

		// We only care about packed int RGB/ARGB for now
		if (!(cm instanceof TDirectColorModel)) {
			return TYPE_CUSTOM;
		}
		if (!(raster.getSampleModel() instanceof TSinglePixelPackedSampleModel)) {
			return TYPE_CUSTOM;
		}
		if (!(raster.getBuffer() instanceof TDataBufferInt)) {
			return TYPE_CUSTOM;
		}

		TDirectColorModel dcm = (TDirectColorModel) cm;
		TSinglePixelPackedSampleModel sm =
			(TSinglePixelPackedSampleModel) raster.getSampleModel();

		// SinglePixelPackedSampleModel should have exactly 1 data element per pixel
		if (sm.getNumDataElements() != 1) {
			return TYPE_CUSTOM;
		}

		int pixelBits = dcm.getPixelSize();
		if (pixelBits != 24 && pixelBits != 32) {
			return TYPE_CUSTOM;
		}

		int rmask = dcm.getRedMask();
		int gmask = dcm.getGreenMask();
		int bmask = dcm.getBlueMask();
		int amask = dcm.getAlphaMask();

		// Standard INT RGB/ARGB masks
		if (rmask == DCM_RED_MASK &&
			gmask == DCM_GREEN_MASK &&
			bmask == DCM_BLUE_MASK) {

			boolean hasAlpha = dcm.hasAlpha();

			if (hasAlpha && amask == DCM_ALPHA_MASK) {
				// ARGB or ARGB_PRE depending on premultiplication
				return isRasterPremultiplied
					? TYPE_INT_ARGB_PRE
					: TYPE_INT_ARGB;
			}

			if (!hasAlpha) {
				// No alpha: INT_RGB
				return TYPE_INT_RGB;
			}
		}

		// Anything else is "custom" for now
		return TYPE_CUSTOM;
	}


	public int getRGB(int x, int y) {
		// Fast path for our int-packed case:
		Object data = raster.getDataElements(x, y, null);
		int pixel = ((int[]) data)[0];
		return colorModel.getRGB(pixel);
	}

	public void setRGB(int x, int y, int argb) {
		dirty = true;
		// Decompose ARGB into components
		int r = (argb >> 16) & 0xFF;
		int g = (argb >> 8) & 0xFF;
		int b = (argb) & 0xFF;
		int a = (argb >> 24) & 0xFF;


		int numComp = colorModel.getNumComponents();
		int[] comps = new int[numComp];

		// Assume ordering R,G,B,(A) for DirectColorModel
		int idx = 0;
		comps[idx++] = r;
		comps[idx++] = g;
		comps[idx++] = b;
		if (colorModel.hasAlpha()) {
			comps[idx] = a;
		}

		int packedPixel = colorModel.getDataElement(comps, 0);
		int[] arr = new int[1];
		arr[0] = packedPixel;

		raster.setDataElements(x, y, arr);
	}

	public int[] getRGB(int startX, int startY,
						int w, int h,
						int[] rgbArray,
						int offset,
						int scansize) {

		if (w <= 0 || h <= 0) {
			return rgbArray;
		}

		if (rgbArray == null) {
			rgbArray = new int[offset + h * scansize];
		}

		int yEnd = startY + h;
		int xEnd = startX + w;

		int idx = offset;
		for (int y = startY; y < yEnd; y++) {
			int lineIdx = idx;
			for (int x = startX; x < xEnd; x++) {
				lineIdx = x - startX + idx;
				rgbArray[lineIdx] = getRGB(x, y);
			}
			idx += scansize;
		}
		return rgbArray;
	}

	public void setRGB(int startX, int startY,
					   int w, int h,
					   int[] rgbArray,
					   int offset,
					   int scansize) {

		dirty = true;

		if (w <= 0 || h <= 0) {
			return;
		}

		int yEnd = startY + h;
		int xEnd = startX + w;

		int idx = offset;
		for (int y = startY; y < yEnd; y++) {
			int lineIdx = idx;
			for (int x = startX; x < xEnd; x++) {
				int argb = rgbArray[lineIdx++];
				setRGB(x, y, argb);
			}
			idx += scansize;
		}
	}

	@Override
	public int getWidth(TImageObserver observer) {
		return width;
	}

	@Override
	public int getHeight(TImageObserver observer) {
		return height;
	}

	@Override
	public TImageProducer getSource() {
		return null;
	}

	@Override
	public TSoftwareGraphics getGraphics() {
		if (this.gfx == null) {
			this.gfx = new TSoftwareGraphics(this);
		}
		return gfx;
	}

	@Override
	public Object getProperty(String name, TImageObserver observer) {
		return properties.get(name);
	}

	@Override
	public boolean isDirty() {
		return cachedImageData == null | dirty;
	}

	@Override
	public void markClean() {
		dirty = false;
	}

	@Override
	public ImageData getImageData() {
		//TODO: dirty check (likely need to propagate from the raster, since it can be modified directly)
		if (cachedImageData == null) {
			cachedImageData = getImageData(0, 0, width, height);
		} else {
			copyToImageData(0, 0, width, height, cachedImageData);
		}
		dirty = false;
		return cachedImageData;
	}

	@Override
	public ImageData getImageData(int x, int y, int w, int h) {
		// Clamp to image bounds, just in case
		if (x < 0) {
			w += x;
			x = 0;
		}
		if (y < 0) {
			h += y;
			y = 0;
		}
		if (x + w > width) {
			w = width - x;
		}
		if (y + h > height) {
			h = height - y;
		}

		if (w <= 0 || h <= 0) {
			// Minimal fallback: empty ImageData
			return new ImageData(1, 1);
		}

		// Prepare destination ImageData
		ImageData imgData = new ImageData(w, h);

		// Copy pixels into ImageData
		copyToImageData(x, y, w, h, imgData);

		return imgData;
	}

	private void copyToImageData(int x, int y, int w, int h, ImageData dest) {
		int dstIndex = 0;

		TSampleModel sm = raster.getSampleModel();
		TDataBuffer db = raster.getBuffer();
		Uint8ClampedArray dst = dest.getData();

		// --------------------------------------------------------------------
		// FAST PATH: packed int pixels (TYPE_INT_RGB / TYPE_INT_ARGB / PRE)
		// --------------------------------------------------------------------
		boolean packedInt =
			db instanceof TDataBufferInt &&
				sm instanceof TSinglePixelPackedSampleModel &&
				(imageType == TYPE_INT_RGB ||
					imageType == TYPE_INT_ARGB ||
					imageType == TYPE_INT_ARGB_PRE);

		if (packedInt) {
			int[] pixels = ((TDataBufferInt) db).getData();
			int scanlineStride = ((TSinglePixelPackedSampleModel) sm).getScanlineStride();

			for (int row = 0; row < h; row++) {
				int srcY = y + row;
				int srcBase = srcY * scanlineStride;

				for (int col = 0; col < w; col++) {
					int srcX = x + col;
					int pixel = pixels[srcBase + srcX];

					int a, r, g, b;

					switch (imageType) {
						case TYPE_INT_RGB:
							// 0x00RRGGBB (no alpha in pixel, treat as opaque)
							a = 0xFF;
							r = (pixel >>> 16) & 0xFF;
							g = (pixel >>> 8) & 0xFF;
							b = (pixel) & 0xFF;
							break;

						case TYPE_INT_ARGB:
							// 0xAARRGGBB, straight alpha
							a = (pixel >>> 24) & 0xFF;
							r = (pixel >>> 16) & 0xFF;
							g = (pixel >>> 8) & 0xFF;
							b = (pixel) & 0xFF;
							break;

						case TYPE_INT_ARGB_PRE:
							// 0xAARRGGBB, premultiplied alpha
							a = (pixel >>> 24) & 0xFF;
							r = (pixel >>> 16) & 0xFF;
							g = (pixel >>> 8) & 0xFF;
							b = (pixel) & 0xFF;

							if (alphaPremultiplied && a != 0 && a != 255) {
								// Un-premultiply to RGBA expected by canvas
								// r' = r / a * 255, etc., with rounding
								r = (r * 255 + (a / 2)) / a;
								g = (g * 255 + (a / 2)) / a;
								b = (b * 255 + (a / 2)) / a;
							}
							break;

						default:
							// Should not happen due to packedInt guard
							a = 0xFF;
							r = g = b = 0;
							break;
					}

					dst.set(dstIndex++, r);
					dst.set(dstIndex++, g);
					dst.set(dstIndex++, b);
					dst.set(dstIndex++, a);
				}
			}
			return;
		}

		// --------------------------------------------------------------------
		// SLOW / GENERIC PATH: respect the ColorModel fully
		// --------------------------------------------------------------------
		// For TYPE_CUSTOM or non-int buffers, use getRGB() which already uses
		// colorModel.getRGB(pixel) under the hood.
		for (int row = 0; row < h; row++) {
			int srcY = y + row;
			for (int col = 0; col < w; col++) {
				int srcX = x + col;

				int argb = getRGB(srcX, srcY); // respects current TColorModel
				int a = (argb >>> 24) & 0xFF;
				int r = (argb >>> 16) & 0xFF;
				int g = (argb >>> 8) & 0xFF;
				int b = (argb) & 0xFF;

				dst.set(dstIndex++, r);
				dst.set(dstIndex++, g);
				dst.set(dstIndex++, b);
				dst.set(dstIndex++, a);
			}
		}
	}

	@Override
	public void putImageData(int x, int y, int w, int h, ImageData data) {
		// Clamp to image bounds, just in case
		if (x < 0) {
			w += x;
			x = 0;
		}
		if (y < 0) {
			h += y;
			y = 0;
		}
		if (x + w > width) {
			w = width - x;
		}
		if (y + h > height) {
			h = height - y;
		}

		if (w <= 0 || h <= 0) {
			return;
		}

		dirty = true;

		Uint8ClampedArray src = data.getData();
		int srcIndex = 0;

		for (int row = 0; row < h; row++) {
			int dstY = y + row;
			for (int col = 0; col < w; col++) {
				int dstX = x + col;

				int r = src.get(srcIndex++);
				int g = src.get(srcIndex++);
				int b = src.get(srcIndex++);
				int a = src.get(srcIndex++);

				int argb = (a << 24) | (r << 16) | (g << 8) | b;
				setRGB(dstX, dstY, argb);
			}
		}
	}
}
