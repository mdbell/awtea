package me.mdbell.awtea.classlib.java.awt.color;

/**
 * Minimal ColorSpace equivalent for TeaVM.
 * Modeled loosely after java.awt.color.ColorSpace.
 */
public abstract class TColorSpace {

	// Standard color spaces (subset)
	public static final int CS_sRGB  = 1000;
	public static final int CS_GRAY  = 1003;

	// Color space types (subset)
	public static final int TYPE_RGB     = 5;
	public static final int TYPE_GRAY    = 6;
	public static final int TYPE_UNKNOWN = 0;

	private final int type;
	private final int numComponents;

	protected TColorSpace(int type, int numComponents) {
		this.type = type;
		this.numComponents = numComponents;
	}

	/** Like ColorSpace.getType(). */
	public int getType() {
		return type;
	}

	/** Number of components in this space (3 for RGB, 1 for gray, etc). */
	public int getNumComponents() {
		return numComponents;
	}

	/**
	 * Convert a color value from this color space to sRGB.
	 * Components are in the range [0.0, 1.0].
	 */
	public abstract float[] toRGB(float[] colorvalue);

	/**
	 * Convert a color value from sRGB into this color space.
	 * Components are in the range [0.0, 1.0].
	 */
	public abstract float[] fromRGB(float[] rgbvalue);

	/**
	 * Convert from this space to CIEXYZ.
	 * You can stub this out if you don't use XYZ anywhere.
	 */
	public abstract float[] toCIEXYZ(float[] colorvalue);

	/**
	 * Convert from CIEXYZ to this space.
	 */
	public abstract float[] fromCIEXYZ(float[] xyzvalue);

	// --- Singleton instances for standard spaces ---

	private static final TColorSpace SRGB  = new TRGBColorSpace();
	private static final TColorSpace GRAY  = new TGrayColorSpace();

	public static TColorSpace getInstance(int colorspace) {
		switch (colorspace) {
			case CS_sRGB:
				return SRGB;
			case CS_GRAY:
				return GRAY;
			default:
				// Fallback: treat everything else as sRGB for now
				return SRGB;
		}
	}
}
