package me.mdbell.awtea.classlib.java.awt.color;

/**
 * Minimal grayscale color space: single component in [0.0, 1.0].
 */
public class TGrayColorSpace extends TColorSpace {

	public TGrayColorSpace() {
		super(TYPE_GRAY, 1);
	}

	@Override
	public float[] toRGB(float[] colorvalue) {
		float g = colorvalue[0];
		return new float[] { g, g, g };
	}

	@Override
	public float[] fromRGB(float[] rgbvalue) {
		// Simple luminance approximation
		float r = rgbvalue[0];
		float g = rgbvalue[1];
		float b = rgbvalue[2];
		float gray = 0.2126f * r + 0.7152f * g + 0.0722f * b;
		return new float[] { gray };
	}

	@Override
	public float[] toCIEXYZ(float[] colorvalue) {
		float g = colorvalue[0];
		// Map gray -> Y, then XYZ ~ (Y,Y,Y) for simplicity
		return new float[] { g, g, g };
	}

	@Override
	public float[] fromCIEXYZ(float[] xyzvalue) {
		float y = xyzvalue[1]; // luminance
		return new float[] { y };
	}
}
