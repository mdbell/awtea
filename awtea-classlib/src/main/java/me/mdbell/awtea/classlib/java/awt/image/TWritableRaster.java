package me.mdbell.awtea.classlib.java.awt.image;

import org.teavm.classlib.java.awt.TPoint;

/**
 * @see java.awt.image.WritableRaster
 */
public class TWritableRaster extends TRaster {

	protected TWritableRaster(TSampleModel sampleModel,
							  TDataBuffer dataBuffer,
							  TPoint origin) {
		super(sampleModel, dataBuffer, origin);
	}

	protected TWritableRaster(TSampleModel sampleModel,
							  TDataBuffer dataBuffer,
							  int minX, int minY,
							  int width, int height) {
		super(sampleModel, dataBuffer, minX, minY, width, height);
	}

	public static TWritableRaster createWritableRaster(TSampleModel sm,
													   TDataBuffer db,
													   TPoint location) {
		if (location == null) {
			location = new TPoint(0, 0);
		}
		return new TWritableRaster(sm, db, location);
	}

	// ---- Write operations ----

	public void setSample(int x, int y, int b, int s) {
		int sx = toSampleX(x);
		int sy = toSampleY(y);
		sampleModel.setSample(sx, sy, b, s, buffer);
	}

	public void setPixel(int x, int y, int[] iArray) {
		int sx = toSampleX(x);
		int sy = toSampleY(y);
		sampleModel.setPixel(sx, sy, iArray, buffer);
	}

	public void setDataElements(int x, int y, Object obj) {
		int sx = toSampleX(x);
		int sy = toSampleY(y);
		sampleModel.setDataElements(sx, sy, obj, buffer);
	}

	public TDataBuffer getDataBuffer() {
		return buffer;
	}

	// TODO: Add setSamples/setPixels

	// ---- Writable child rasters ----

	public TWritableRaster createWritableChild(int parentX, int parentY,
											   int width, int height,
											   int childMinX, int childMinY,
											   int[] bandList) {

		if (bandList != null) {
			throw new UnsupportedOperationException("Band selection not implemented yet");
		}

		TSampleModel childSM =
			sampleModel.createCompatibleSampleModel(width, height);

		TWritableRaster child = new TWritableRaster(childSM, buffer,
			childMinX, childMinY,
			width, height);

		child.sampleModelTranslateX =
			this.sampleModelTranslateX + (parentX - this.minX);
		child.sampleModelTranslateY =
			this.sampleModelTranslateY + (parentY - this.minY);

		return child;
	}

	public TWritableRaster createWritableTranslatedChild(int newMinX, int newMinY) {
		TWritableRaster child = new TWritableRaster(sampleModel, buffer,
			newMinX, newMinY,
			width, height);

		child.sampleModelTranslateX =
			this.sampleModelTranslateX + (newMinX - this.minX);
		child.sampleModelTranslateY =
			this.sampleModelTranslateY + (newMinY - this.minY);

		return child;
	}

}
