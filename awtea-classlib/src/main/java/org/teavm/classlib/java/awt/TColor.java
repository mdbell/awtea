package org.teavm.classlib.java.awt;

import lombok.Getter;
import me.mdbell.awtea.classlib.java.awt.*;
import me.mdbell.awtea.classlib.java.awt.color.TColorSpace;
import me.mdbell.awtea.classlib.java.awt.geom.TAffineTransform;
import me.mdbell.awtea.classlib.java.awt.geom.TRectangle2D;
import me.mdbell.awtea.classlib.java.awt.image.TColorModel;


public class TColor implements TPaint {

	// shades
	public static final TColor BLACK = new TColor(0, 0, 0);
	public static final TColor black = BLACK;
	public static final TColor WHITE = new TColor(255, 255, 255);
	public static final TColor white = WHITE;

	// gray shades
	public static final TColor GRAY = new TColor(128, 128, 128);
	public static final TColor gray = GRAY;
	public static final TColor LIGHT_GRAY = new TColor(192, 192, 192);
	public static final TColor lightGray = LIGHT_GRAY;
	public static final TColor DARK_GRAY = new TColor(64, 64, 64);
	public static final TColor darkGray = DARK_GRAY;

	// primary colors

	public static final TColor RED = new TColor(255, 0, 0);
	public static final TColor red = RED;
	public static final TColor GREEN = new TColor(0, 255, 0);
	public static final TColor green = GREEN;
	public static final TColor BLUE = new TColor(0, 0, 255);
	public static final TColor blue = BLUE;
	public static final TColor YELLOW = new TColor(255, 255, 0);
	public static final TColor yellow = YELLOW;
	public static final TColor CYAN = new TColor(0, 255, 255);
	public static final TColor cyan = CYAN;
	public static final TColor MAGENTA = new TColor(255, 0, 255);
	public static final TColor magenta = MAGENTA;
	public static final TColor ORANGE = new TColor(255, 200, 0);
	public static final TColor orange = ORANGE;
	public static final TColor PINK = new TColor(255, 175, 175);
	public static final TColor pink = PINK;

	private final int value;

	@Getter
	private TColorSpace colorSpace = TColorSpace.getInstance(TColorSpace.CS_sRGB);

	public TColor(int r, int g, int b) {
		this(r, g, b, 255);
	}

	public TColor(float r, float g, float b) {
		this(r, g, b, 1.0f);
	}

	public TColor(int r, int g, int b, int a) {
		value = ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
	}

	public TColor(float r, float g, float b, float a) {
		this((int) (r * 255 + 0.5), (int) (g * 255 + 0.5), (int) (b * 255 + 0.5), (int) (a * 255 + 0.5));
	}

	public TColor(int rgb) {
		this(rgb, false);
	}

	public TColor(int rgba, boolean hasalpha) {
		if (hasalpha) {
			value = rgba;
		} else {
			value = 0xFF000000 | (rgba & 0xFFFFFF);
		}
	}

	public TColor(TColorSpace cspace, float[] components, float alpha) {
		this.colorSpace = cspace;
		int r = (int) (components[0] * 255 + 0.5);
		int g = (int) (components[1] * 255 + 0.5);
		int b = (int) (components[2] * 255 + 0.5);
		int a = (int) (alpha * 255 + 0.5);
		value = ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
	}

	public float[] getColorComponents(float[] compArray) {
		if (compArray == null || compArray.length < 3) {
			compArray = new float[3];
		}
		compArray[0] = getRed() / 255f;
		compArray[1] = getGreen() / 255f;
		compArray[2] = getBlue() / 255f;
		return compArray;
	}

	public float[] getColorComponents(TColorSpace cspace, float[] compArray) {
		if (this.colorSpace == null) {
			this.colorSpace = TColorSpace.getInstance(TColorSpace.CS_sRGB);
		}
		float[] f = new float[]{
			((float) getRed()) / 255f,
			((float) getGreen()) / 255f,
			((float) getBlue()) / 255f
		};
		float[] tmp = this.colorSpace.toCIEXYZ(f);
		float[] tmpout = cspace.fromCIEXYZ(tmp);

		if (compArray == null) {
			return tmpout;
		}

		System.arraycopy(tmpout, 0, compArray, 0, tmpout.length);
		return compArray;
	}

	public float[] getComponents(float[] compArray) {
		if (compArray == null || compArray.length < 4) {
			compArray = new float[4];
		}
		compArray[0] = getRed() / 255f;
		compArray[1] = getGreen() / 255f;
		compArray[2] = getBlue() / 255f;
		compArray[3] = getAlpha() / 255f;
		return compArray;
	}

	public float[] getComponents(TColorSpace cspace, float[] compArray) {
		if (this.colorSpace == null) {
			this.colorSpace = TColorSpace.getInstance(TColorSpace.CS_sRGB);
		}
		float[] f = new float[]{
			((float) getRed()) / 255f,
			((float) getGreen()) / 255f,
			((float) getBlue()) / 255f
		};

		float[] tmp = this.colorSpace.toCIEXYZ(f);
		float[] tmpout = cspace.fromCIEXYZ(tmp);

		if (compArray == null) {
			compArray = new float[tmpout.length + 1];
		}
		System.arraycopy(tmpout, 0, compArray, 0, tmpout.length);
		compArray[tmpout.length] = ((float) getAlpha()) / 255f;

		return compArray;
	}

	public float[] getRGBComponents(float[] compArray) {
		if (compArray == null || compArray.length < 4) {
			compArray = new float[4];
		}
		compArray[0] = getRed() / 255f;
		compArray[1] = getGreen() / 255f;
		compArray[2] = getBlue() / 255f;
		compArray[3] = getAlpha() / 255f;
		return compArray;
	}

	public float[] getRGBColorComponents(float[] compArray) {
		if (compArray == null || compArray.length < 3) {
			compArray = new float[3];
		}
		compArray[0] = getRed() / 255f;
		compArray[1] = getGreen() / 255f;
		compArray[2] = getBlue() / 255f;
		return compArray;
	}

	public int getRGB() {
		return value;
	}

	public int getRed() {
		return getRGB() >> 16 & 0xFF;
	}

	public int getGreen() {
		return getRGB() >> 8 & 0xFF;
	}

	public int getBlue() {
		return getRGB() & 0xFF;
	}

	public int getAlpha() {
		return getRGB() >> 24 & 0xFF;
	}

	public TColor brighter() {
		int r = getRed();
		int g = getGreen();
		int b = getBlue();
		int i = (int) (1.0 / (1.0 - 0.7));
		if (r == 0 && g == 0 && b == 0) {
			return new TColor(i, i, i);
		}
		if (r > 0 && r < i) r = i;
		if (g > 0 && g < i) g = i;
		if (b > 0 && b < i) b = i;

		return new TColor(Math.min((int) (r / 0.7), 255),
			Math.min((int) (g / 0.7), 255),
			Math.min((int) (b / 0.7), 255));
	}

	public TColor darker() {
		return new TColor(Math.max((int) (getRed() * 0.7), 0),
			Math.max((int) (getGreen() * 0.7), 0),
			Math.max((int) (getBlue() * 0.7), 0));
	}

	@Override
	public TPaintContext createContext(TColorModel cm, TRectangle deviceBounds, TRectangle2D userBounds, TAffineTransform xform, TRenderingHints hints) {
		return null;
	}

	@Override
	public int getTransparency() {
		int alpha = getAlpha();
		if (alpha == 255) {
			return TTransparency.OPAQUE;
		} else if (alpha == 0) {
			return TTransparency.BITMASK;
		} else {
			return TTransparency.TRANSLUCENT;
		}
	}

	@Override
	public int hashCode() {
		return value;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof TColor)) {
			return false;
		}
		TColor c = (TColor) obj;
		return c.getRGB() == getRGB();
	}

	@Override
	public String toString() {
		return "TColor[r=" + getRed() + ",g=" + getGreen() + ",b=" + getBlue() + ",a=" + getAlpha() + "]";
	}

	public static TColor decode(String str) {
		int value = Integer.decode(str);
		return new TColor(value >> 16 & 0xFF, value >> 8 & 0xFF, value & 0xFF);
	}

	public static TColor getColor(String str) {
		return getColor(str, null);
	}

	public static TColor getColor(String str, TColor defaultColor) {
		Integer value = Integer.getInteger(str);
		if (value == null) {
			return defaultColor;
		}
		return new TColor(value >> 16 & 0xFF, value >> 8 & 0xFF, value & 0xFF);
	}

	public static TColor getColor(String str, int defaultValue) {
		Integer value = Integer.getInteger(str);
		if (value == null) {
			value = defaultValue;
		}
		return new TColor(value >> 16 & 0xFF, value >> 8 & 0xFF, value & 0xFF);
	}

	public static TColor getHSBColor(float h, float s, float b) {
		int rgb = HSBtoRGB(h, s, b);
		return new TColor(rgb);
	}

	public static float[] RGBtoHSB(int r, int g, int b, float[] hsbvals) {
		float hue, saturation, brightness;
		if (hsbvals == null) {
			hsbvals = new float[3];
		}
		int cmax = Math.max(r, g);
		if (b > cmax) cmax = b;
		int cmin = Math.min(r, g);
		if (b < cmin) cmin = b;

		brightness = ((float) cmax) / 255.0f;
		if (cmax != 0) {
			saturation = ((float) (cmax - cmin)) / ((float) cmax);
		} else {
			saturation = 0;
		}
		if (saturation == 0) {
			hue = 0;
		} else {
			float redc = ((float) (cmax - r)) / ((float) (cmax - cmin));
			float greenc = ((float) (cmax - g)) / ((float) (cmax - cmin));
			float bluec = ((float) (cmax - b)) / ((float) (cmax - cmin));
			if (r == cmax) {
				hue = bluec - greenc;
			} else if (g == cmax) {
				hue = 2.0f + redc - bluec;
			} else {
				hue = 4.0f + greenc - redc;
			}
			hue /= 6.0f;
			if (hue < 0) {
				hue += 1.0f;
			}
		}
		hsbvals[0] = hue;
		hsbvals[1] = saturation;
		hsbvals[2] = brightness;
		return hsbvals;
	}

	public static int HSBtoRGB(float hue, float saturation, float brightness) {
		int r = 0, g = 0, b = 0;
		if (saturation == 0) {
			r = g = b = (int) (brightness * 255.0f + 0.5f);
		} else {
			float h = (hue - (float) Math.floor(hue)) * 6.0f;
			float f = h - (float) java.lang.Math.floor(h);
			float p = brightness * (1.0f - saturation);
			float q = brightness * (1.0f - saturation * f);
			float t = brightness * (1.0f - (saturation * (1.0f - f)));
			switch ((int) h) {
				case 0:
					r = (int) (brightness * 255.0f + 0.5f);
					g = (int) (t * 255.0f + 0.5f);
					b = (int) (p * 255.0f + 0.5f);
					break;
				case 1:
					r = (int) (q * 255.0f + 0.5f);
					g = (int) (brightness * 255.0f + 0.5f);
					b = (int) (p * 255.0f + 0.5f);
					break;
				case 2:
					r = (int) (p * 255.0f + 0.5f);
					g = (int) (brightness * 255.0f + 0.5f);
					b = (int) (t * 255.0f + 0.5f);
					break;
				case 3:
					r = (int) (p * 255.0f + 0.5f);
					g = (int) (q * 255.0f + 0.5f);
					b = (int) (brightness * 255.0f + 0.5f);
					break;
				case 4:
					r = (int) (t * 255.0f + 0.5f);
					g = (int) (p * 255.0f + 0.5f);
					b = (int) (brightness * 255.0f + 0.5f);
					break;
				case 5:
					r = (int) (brightness * 255.0f + 0.5f);
					g = (int) (p * 255.0f + 0.5f);
					b = (int) (q * 255.0f + 0.5f);
					break;
			}
		}
		return (0xFF << 24) | (r << 16) | (g << 8) | b;
	}
}
