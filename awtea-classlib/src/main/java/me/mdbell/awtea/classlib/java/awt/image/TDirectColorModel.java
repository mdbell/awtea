package me.mdbell.awtea.classlib.java.awt.image;

import lombok.Getter;
import me.mdbell.awtea.classlib.java.awt.color.TColorSpace;

/**
 * @see java.awt.image.DirectColorModel
 */
public class TDirectColorModel extends TColorModel {

	@Getter
	protected int redMask;
	@Getter
	protected int greenMask;
	@Getter
	protected int blueMask;
	@Getter
	protected int alphaMask;

	protected int redShift;
	protected int greenShift;
	protected int blueShift;
	protected int alphaShift;

	public TDirectColorModel(
		int bits,
		int rMask,
		int gMask,
		int bMask){
		this(bits, rMask, gMask, bMask, 0);
	}

	/**
	 * Common 32-bit ARGB constructor.
	 *
	 * bits: total pixel bits (usually 32)
	 * rMask, gMask, bMask, aMask: bit masks for each component.
	 */
	public TDirectColorModel(int bits,
							int rMask,
							int gMask,
							int bMask,
							int aMask) {
		this(TColorSpace.getInstance(TColorSpace.CS_sRGB), bits, rMask, gMask, bMask, aMask, false, TDataBuffer.TYPE_INT);
	}

	/**
	 * Extended constructor with ColorSpace parameter.
	 * Constructs a DirectColorModel from the given masks specifying
	 * which bits in an int pixel representation contain the red, green and blue color samples,
	 * the alpha samples, and the color space.
	 *
	 * @param space the ColorSpace to use
	 * @param bits the number of bits in the pixel values
	 * @param rMask the mask indicating which bits represent the red color component
	 * @param gMask the mask indicating which bits represent the green color component
	 * @param bMask the mask indicating which bits represent the blue color component
	 * @param aMask the mask indicating which bits represent the alpha component
	 * @param isAlphaPremultiplied true if color samples are premultiplied by the alpha sample
	 * @param transferType the type of array used to represent pixel values
	 */
	public TDirectColorModel(TColorSpace space,
							 int bits,
							 int rMask,
							 int gMask,
							 int bMask,
							 int aMask,
							 boolean isAlphaPremultiplied,
							 int transferType) {
		super(
			bits,
			aMask != 0 ? 4 : 3,          // numComponents
			aMask != 0,                  // hasAlpha
			isAlphaPremultiplied,        // isAlphaPremultiplied
			aMask == 0
				? OPAQUE
				: TRANSLUCENT,           // transparency
			transferType                 // transferType
		);

		this.redMask   = rMask;
		this.greenMask = gMask;
		this.blueMask  = bMask;
		this.alphaMask = aMask;

		this.redShift   = computeShift(rMask);
		this.greenShift = computeShift(gMask);
		this.blueShift  = computeShift(bMask);
		this.alphaShift = computeShift(aMask);

		this.colorSpace = space != null ? space : TColorSpace.getInstance(TColorSpace.CS_sRGB);
	}

	private static int computeShift(int mask) {
		if (mask == 0) {
			return 0;
		}
		int shift = 0;
		while ((mask & 0x1) == 0) {
			mask >>>= 1;
			shift++;
		}
		return shift;
	}

	public TSampleModel createCompatibleSampleModel(int width, int height){
		int[] masks;
		if(hasAlpha()) {
			masks = new int[]{ redMask, greenMask, blueMask, alphaMask};
		}else {
			masks = new int [] {redMask, greenMask, blueMask};
		}
		return new TSinglePixelPackedSampleModel(getTransferType(), width, height, masks);
	}

	@Override
	public int getRed(int pixel) {
		int v = (pixel & redMask) >>> redShift;
		// assume 8-bit components; if not, we would scale to 0-255 here
		return v & 0xFF;
	}

	@Override
	public int getGreen(int pixel) {
		int v = (pixel & greenMask) >>> greenShift;
		return v & 0xFF;
	}

	@Override
	public int getBlue(int pixel) {
		int v = (pixel & blueMask) >>> blueShift;
		return v & 0xFF;
	}

	@Override
	public int getAlpha(int pixel) {
		if (!alpha || alphaMask == 0) {
			return 0xFF;
		}
		int v = (pixel & alphaMask) >>> alphaShift;
		return v & 0xFF;
	}

	@Override
	public int getRGB(int pixel) {
		int result = !alpha ? 0xFF000000 : (((pixel & alphaMask) >>> alphaShift) << 24);
		result |= ((pixel & redMask) >>> redShift) << 16;
		result |= ((pixel & greenMask) >>> greenShift) << 8;
		result |= (pixel & blueMask) >>> blueShift;
		return result;
	}

	// Optionally override component-based helpers if you want:
	@Override
	public int[] getComponents(int pixel, int[] components, int offset) {
		int[] arr = components;
		if (arr == null || arr.length < offset + numComponents) {
			arr = new int[offset + numComponents];
		}

		int idx = offset;
		arr[idx++] = getRed(pixel);
		arr[idx++] = getGreen(pixel);
		arr[idx++] = getBlue(pixel);
		if (alpha) {
			arr[idx] = getAlpha(pixel);
		}
		return arr;
	}

	@Override
	public int getDataElement(int[] components, int offset) {
		// Assemble packed pixel from components, honoring masks/shifts.
		int r = components[offset]     & 0xFF;
		int g = components[offset + 1] & 0xFF;
		int b = components[offset + 2] & 0xFF;
		int a = alpha
			? (components[offset + 3] & 0xFF)
			: 0xFF;

		int pixel = 0;

		if (redMask != 0) {
			pixel |= (r << redShift) & redMask;
		}
		if (greenMask != 0) {
			pixel |= (g << greenShift) & greenMask;
		}
		if (blueMask != 0) {
			pixel |= (b << blueShift) & blueMask;
		}
		if (alpha && alphaMask != 0) {
			pixel |= (a << alphaShift) & alphaMask;
		}

		return pixel;
	}

	public Object getDataElements(int rgb, Object pixel) {
		throw new UnsupportedOperationException("This method has not been "+
			"implemented for transferType " + transferType);

	}
}
