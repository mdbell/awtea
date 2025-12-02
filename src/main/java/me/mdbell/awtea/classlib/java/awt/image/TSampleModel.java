package me.mdbell.awtea.classlib.java.awt.image;

import lombok.Getter;

@Getter
public abstract class TSampleModel {

	protected int dataType;
	protected int width;
	protected int height;
	// number of bands (channels) per pixel, 3 for RGB, 4 for RGBA
	protected int numBands;

	protected TSampleModel(int dataType, int width, int height, int numBands) {
		this.dataType = dataType;
		this.width = width;
		this.height = height;
		this.numBands = numBands;
	}

	/**
	 * Returns one sample (band) at (x,y) from the given DataBuffer.
	 * b is the band index: 0=R, 1=G, 2=B, 3=A for ARGB-type models.
	 */
	public abstract int getSample(int x, int y, int b, TDataBuffer data);

	/**
	 * Sets one sample (band) at (x,y) in the given DataBuffer.
	 */
	public abstract void setSample(int x, int y, int b, int s, TDataBuffer data);

	/**
	 * Gets all band samples at (x,y) into iArray[0..numBands-1].
	 */
	public abstract int[] getPixel(int x, int y, int[] iArray, TDataBuffer data);

	/**
	 * Sets all band samples at (x,y) from iArray[0..numBands-1].
	 */
	public abstract void setPixel(int x, int y, int[] iArray, TDataBuffer data);

	/**
	 * Creates a new SampleModel of the same layout, but for a new width/height.
	 * Used when creating child/compatible rasters.
	 */
	public abstract TSampleModel createCompatibleSampleModel(int w, int h);

	/**
	 * Create a new DataBuffer suitable to hold samples for width*height pixels
	 * using this sample model's layout and dataType.
	 */
	public abstract TDataBuffer createDataBuffer();

	/**
	 * Returns the number of data elements required to hold one pixel.
	 * For SinglePixelPackedSampleModel with int pixels, this is 1.
	 * For ComponentSampleModel (separate per-band arrays), it can be >1.
	 */
	public abstract int getNumDataElements();

	/**
	 * Get all data elements for the pixel at (x,y) as “raw” values into obj.
	 * Often just same as getPixel, but typed differently (Object for flexibility).
	 */
	public abstract Object getDataElements(int x, int y, Object obj, TDataBuffer data);

	/**
	 * Set the pixel at (x,y) from raw data elements in obj.
	 */
	public abstract void setDataElements(int x, int y, Object obj, TDataBuffer data);
}
