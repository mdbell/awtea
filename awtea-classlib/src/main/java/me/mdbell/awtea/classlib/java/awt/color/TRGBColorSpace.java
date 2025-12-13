package me.mdbell.awtea.classlib.java.awt.color;

/**
 * Minimal sRGB color space: components are R,G,B in [0.0, 1.0].
 * We treat sRGB <-> sRGB as identity.
 */
public class TRGBColorSpace extends TColorSpace {

	public TRGBColorSpace() {
		super(TYPE_RGB, 3);
	}

	@Override
	public float[] toRGB(float[] colorvalue) {
		// Already in sRGB
		if (colorvalue.length == 3) {
			return colorvalue;
		}
		float[] out = new float[3];
		int len = Math.min(3, colorvalue.length);
		for (int i = 0; i < len; i++) {
			out[i] = colorvalue[i];
		}
		return out;
	}

	@Override
	public float[] fromRGB(float[] rgbvalue) {
		// Identity: sRGB -> sRGB
		if (rgbvalue.length == 3) {
			return rgbvalue;
		}
		float[] out = new float[3];
		int len = Math.min(3, rgbvalue.length);
		for (int i = 0; i < len; i++) {
			out[i] = rgbvalue[i];
		}
		return out;
	}

	@Override
	public float[] toCIEXYZ(float[] colorvalue) {
		// Very rough matrix approximation (BT.709 style).
		// Good enough for code paths that just want "some" XYZ.
		float r = colorvalue[0];
		float g = colorvalue[1];
		float b = colorvalue[2];

		float x = 0.4124f * r + 0.3576f * g + 0.1805f * b;
		float y = 0.2126f * r + 0.7152f * g + 0.0722f * b;
		float z = 0.0193f * r + 0.1192f * g + 0.9505f * b;

		return new float[] { x, y, z };
	}

	@Override
	public float[] fromCIEXYZ(float[] xyzvalue) {
		float x = xyzvalue[0];
		float y = xyzvalue[1];
		float z = xyzvalue[2];

		float r =  3.2406f * x - 1.5372f * y - 0.4986f * z;
		float g = -0.9689f * x + 1.8758f * y + 0.0415f * z;
		float b =  0.0557f * x - 0.2040f * y + 1.0570f * z;

		return new float[] { r, g, b };
	}
}
