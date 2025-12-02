package me.mdbell.awtea.classlib.java.awt.image;

import lombok.AccessLevel;
import lombok.Getter;
import me.mdbell.awtea.classlib.java.awt.color.TColorSpace;

/**
 * @see java.awt.image.ColorModel
 */
@Getter
public abstract class TColorModel {

	// Transparency constants (mirroring java.awt.Transparency)
	public static final int OPAQUE      = 1;
	public static final int BITMASK     = 2;
	public static final int TRANSLUCENT = 3;

	private static TColorModel rgbDefaultModel;

	/** Total bits per pixel (e.g. 32 for ARGB) */
	protected int pixelSize;

	/** Number of components (including alpha if present). */
	protected int numComponents;

	/** True if model has an alpha component. */
	@Getter(AccessLevel.NONE)
	protected boolean alpha;

	/** True if colors are stored pre-multiplied by alpha. */
	protected boolean isAlphaPremultiplied;

	/** Transparency mode (OPAQUE, BITMASK, TRANSLUCENT). */
	protected int transparency;

	/** Transfer type (DataBuffer.TYPE_*), e.g., TYPE_INT for int pixels. */
	protected int transferType;

	protected TColorSpace colorSpace = TColorSpace.getInstance(TColorSpace.CS_sRGB);

	protected TColorModel(int pixelSize,
						 int numComponents,
						 boolean hasAlpha,
						 boolean isAlphaPremultiplied,
						 int transparency,
						 int transferType) {
		this.pixelSize = pixelSize;
		this.numComponents = numComponents;
		this.alpha = hasAlpha;
		this.isAlphaPremultiplied = isAlphaPremultiplied;
		this.transparency = transparency;
		this.transferType = transferType;
	}

	public int getNumColorComponents() {
		return alpha ? numComponents - 1 : numComponents;
	}

	public boolean hasAlpha() {
		return alpha;
	}

	public boolean isAlphaPremultiplied() {
		return isAlphaPremultiplied;
	}

	// ---- Core abstract color queries ----

	/** Extract red (0–255) from a packed pixel. */
	public abstract int getRed(int pixel);

	/** Extract green (0–255) from a packed pixel. */
	public abstract int getGreen(int pixel);

	/** Extract blue (0–255) from a packed pixel. */
	public abstract int getBlue(int pixel);

	/** Extract alpha (0–255) from a packed pixel. */
	public abstract int getAlpha(int pixel);

	/**
	 * Get ARGB as a single int in the standard Java format:
	 * 0xAARRGGBB
	 */
	public int getRGB(int pixel) {
		int a = getAlpha(pixel) & 0xFF;
		int r = getRed(pixel)   & 0xFF;
		int g = getGreen(pixel) & 0xFF;
		int b = getBlue(pixel)  & 0xFF;
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	// ---- Convenience for bulk component access (optional) ----

	public int[] getComponents(int pixel, int[] components, int offset) {
		int[] arr = components;
		if (arr == null || arr.length < offset + numComponents) {
			arr = new int[offset + numComponents];
		}

		int r = getRed(pixel);
		int g = getGreen(pixel);
		int b = getBlue(pixel);
		int a = alpha ? getAlpha(pixel) : 0xFF;

		int idx = offset;
		// Assume ARGB ordering for now: R, G, B, (A)
		arr[idx++] = r;
		arr[idx++] = g;
		arr[idx++] = b;
		if (alpha) {
			arr[idx] = a;
		}
		return arr;
	}

	/**
	 * Composes a packed pixel from components in argb[] starting at offset.
	 * This is a helper for e.g. setRGB-style APIs.
	 */
	public int getDataElement(int[] components, int offset) {
		int r = components[offset]     & 0xFF;
		int g = components[offset + 1] & 0xFF;
		int b = components[offset + 2] & 0xFF;
		int a = alpha
			? (components[offset + 3] & 0xFF)
			: 0xFF;

		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	public Object getDataElements(int rgb, Object pixel) {
		throw new UnsupportedOperationException
			("This method is not supported by this color model.");
	}

	public static TColorModel getRGBdefault() {
		if (rgbDefaultModel == null) {
			rgbDefaultModel = new TDirectColorModel(32,
				0x00ff0000,       // Red
				0x0000ff00,       // Green
				0x000000ff,       // Blue
				0xff000000        // Alpha
			);
		}
		return rgbDefaultModel;
	}
}
