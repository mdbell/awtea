package me.mdbell.awtea.classlib.java.awt.image;

import lombok.Getter;

public class TSinglePixelPackedSampleModel extends TSampleModel {

	/** Elements per row in the DataBuffer (for int pixels). */
	@Getter
	protected int scanlineStride;

	/** Bit mask for each band, e.g. {0xFF0000, 0xFF00, 0xFF}. */
	protected int[] bitMasks;

	/** Right-shift count for each mask to get the band sample. */
	protected int[] bitOffsets;


	public TSinglePixelPackedSampleModel(int dataType, int w, int h, int[] bitMasks) {
		this(dataType, w, h, w, bitMasks);
	}

	public TSinglePixelPackedSampleModel(int dataType, int w, int h, int scanlineStride, int[] bitMasks) {
		super(dataType, w, h, bitMasks.length);
		this.scanlineStride = scanlineStride;

		this.bitMasks = new int[bitMasks.length];
		System.arraycopy(bitMasks, 0, this.bitMasks, 0, bitMasks.length);

		this.bitOffsets = new int[bitMasks.length];
		for (int i = 0; i < bitMasks.length; i++) {
			int mask = bitMasks[i];
			int off = 0;
			if (mask != 0) {
				while ((mask & 0x1) == 0) {
					mask >>>= 1;
					off++;
				}
			}
			this.bitOffsets[i] = off;
		}
	}

	public int[] getBitMasks() {
		int[] copy = new int[bitMasks.length];
		System.arraycopy(bitMasks, 0, copy, 0, bitMasks.length);
		return copy;
	}

	public int getBitMask(int band) {
		return bitMasks[band];
	}

	public int[] getBitOffsets() {
		int[] copy = new int[bitOffsets.length];
		System.arraycopy(bitOffsets, 0, copy, 0, bitOffsets.length);
		return copy;
	}

	public int getBitOffset(int band) {
		return bitOffsets[band];
	}

	private int getBufferIndex(int x, int y, TDataBuffer data) {
		// Generally DataBuffer may have bank offsets; for SPPM we assume 1 bank.
		int base = data.getOffset(); // offset for bank 0
		return base + y * scanlineStride + x;
	}

	@Override
	public int getSample(int x, int y, int b, TDataBuffer data) {
		int idx = getBufferIndex(x, y, data);
		int pixel = data.getElem(idx);
		int mask  = bitMasks[b];
		int shift = bitOffsets[b];
		return (pixel & mask) >>> shift;
	}

	@Override
	public void setSample(int x, int y, int b, int s, TDataBuffer data) {
		int idx = getBufferIndex(x, y, data);
		int pixel = data.getElem(idx);
		int mask  = bitMasks[b];
		int shift = bitOffsets[b];

		// Clear band bits, then OR in the new sample
		pixel = (pixel & ~mask) | ((s << shift) & mask);
		data.setElem(idx, pixel);
	}

	@Override
	public int[] getPixel(int x, int y, int[] iArray, TDataBuffer data) {
		int[] out = (iArray != null && iArray.length >= numBands)
			? iArray
			: new int[numBands];

		int idx = getBufferIndex(x, y, data);
		int pixel = data.getElem(idx);

		for (int b = 0; b < numBands; b++) {
			int mask  = bitMasks[b];
			int shift = bitOffsets[b];
			out[b] = (pixel & mask) >>> shift;
		}

		return out;
	}

	@Override
	public void setPixel(int x, int y, int[] iArray, TDataBuffer data) {
		int idx = getBufferIndex(x, y, data);
		int pixel = data.getElem(idx);

		for (int b = 0; b < numBands; b++) {
			int mask  = bitMasks[b];
			int shift = bitOffsets[b];
			int s = iArray[b];

			pixel = (pixel & ~mask) | ((s << shift) & mask);
		}

		data.setElem(idx, pixel);
	}

	@Override
	public TSampleModel createCompatibleSampleModel(int w, int h) {
		// Same layout, just new size
		return new TSinglePixelPackedSampleModel(dataType, w, h, w, bitMasks);
	}

	@Override
	public TDataBuffer createDataBuffer() {
		int size = scanlineStride * height;
		switch (dataType) {
			case TDataBuffer.TYPE_INT:
				return new TDataBufferInt(size);
			case TDataBuffer.TYPE_USHORT:
				return new TDataBufferUShort(size);
			case TDataBuffer.TYPE_SHORT:
				return new TDataBufferShort(size);
			case TDataBuffer.TYPE_BYTE:
				return new TDataBufferByte(size);
			default:
				throw new IllegalArgumentException("Unsupported dataType for SPPM: " + dataType);
		}
	}

	@Override
	public int getNumDataElements() {
		// Single packed pixel per int → one element
		return 1;
	}

	@Override
	public Object getDataElements(int x, int y, Object obj, TDataBuffer data) {
		// For SPPM with int pixels, the "data element" is the packed int itself.
		int idx = getBufferIndex(x, y, data);
		int pixel = data.getElem(idx);

		int[] arr;
		if (obj instanceof int[] && ((int[]) obj).length > 0) {
			arr = (int[]) obj;
		} else {
			arr = new int[1];
		}
		arr[0] = pixel;
		return arr;
	}

	@Override
	public void setDataElements(int x, int y, Object obj, TDataBuffer data) {
		int[] arr = (int[]) obj;
		int idx = getBufferIndex(x, y, data);
		data.setElem(idx, arr[0]);
	}
}
