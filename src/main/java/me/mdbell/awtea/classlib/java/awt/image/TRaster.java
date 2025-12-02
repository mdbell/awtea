package me.mdbell.awtea.classlib.java.awt.image;

import lombok.Getter;
import org.teavm.classlib.java.awt.TPoint;
import me.mdbell.awtea.classlib.java.awt.TRectangle;

@Getter
public class TRaster {

	/** How to interpret the DataBuffer as pixels/bands. */
	protected TSampleModel sampleModel;

	/** The actual storage (backing primitive arrays via DataBuffer*). */
	protected TDataBuffer buffer;

	/** Upper-left corner of this Raster in "image space". */
	protected int minX;
	protected int minY;

	/** Width and height (in pixels). */
	protected int width;
	protected int height;

	/**
	 * Translation from global (x,y) to the SampleModel's coordinate space.
	 * Usually sampleModelX = x - sampleModelTranslateX, etc.
	 */
	protected int sampleModelTranslateX;
	protected int sampleModelTranslateY;

	protected TRaster(TSampleModel sampleModel,
					 TDataBuffer buffer,
					 TPoint origin) {
		this(sampleModel, buffer,
			origin.x, origin.y,
			sampleModel.getWidth(),
			sampleModel.getHeight());
	}

	protected TRaster(TSampleModel sampleModel,
					 TDataBuffer buffer,
					 int minX, int minY,
					 int width, int height) {

		if (sampleModel == null || buffer == null) {
			throw new NullPointerException("sampleModel and dataBuffer must not be null");
		}

		this.sampleModel = sampleModel;
		this.buffer = buffer;

		this.minX = minX;
		this.minY = minY;
		this.width = width;
		this.height = height;

		// For a "root" raster, the sample model usually starts at (0,0)
		this.sampleModelTranslateX = minX;
		this.sampleModelTranslateY = minY;
	}

	public int getNumBands() {
		return sampleModel.getNumBands();
	}

	public int getDataType() {
		return sampleModel.getDataType();
	}

	public TPoint getBoundsOrigin() {
		return new TPoint(minX, minY);
	}

	protected final int toSampleX(int x) {
		return x - sampleModelTranslateX;
	}

	protected final int toSampleY(int y) {
		return y - sampleModelTranslateY;
	}

	public int getSample(int x, int y, int b) {
		int sx = toSampleX(x);
		int sy = toSampleY(y);
		return sampleModel.getSample(sx, sy, b, buffer);
	}

	public int[] getPixel(int x, int y, int[] iArray) {
		int sx = toSampleX(x);
		int sy = toSampleY(y);
		return sampleModel.getPixel(sx, sy, iArray, buffer);
	}

	public Object getDataElements(int x, int y, Object obj) {
		int sx = toSampleX(x);
		int sy = toSampleY(y);
		return sampleModel.getDataElements(sx, sy, obj, buffer);
	}

	public TRaster createChild(int parentX, int parentY,
							  int width, int height,
							  int childMinX, int childMinY,
							  int[] bandList) {

		if (bandList != null) {
			// For now, you can throw or implement a subset logic later.
			throw new UnsupportedOperationException("Band selection not implemented yet");
		}

		TSampleModel childSM =
			sampleModel.createCompatibleSampleModel(width, height);

		// Child sampleModel is usually aligned with childMinX/childMinY.
		TRaster child = new TRaster(childSM, buffer, childMinX, childMinY, width, height);

		// Align child's sampleModel translation relative to parent
		child.sampleModelTranslateX = this.sampleModelTranslateX + (parentX - this.minX);
		child.sampleModelTranslateY = this.sampleModelTranslateY + (parentY - this.minY);

		return child;
	}

	public TRaster createTranslatedChild(int newMinX, int newMinY) {
		TRaster child = new TRaster(sampleModel, buffer,
			newMinX, newMinY,
			width, height);
		// Adjust translation so same data is used
		child.sampleModelTranslateX =
			this.sampleModelTranslateX + (newMinX - this.minX);
		child.sampleModelTranslateY =
			this.sampleModelTranslateY + (newMinY - this.minY);
		return child;
	}

	public TRectangle getBounds() {
		return new TRectangle(minX, minY, width, height);
	}

	public static TWritableRaster createWritableRaster(TSampleModel sm,
													  TDataBuffer db,
													  TPoint location) {
		if (sm == null) {
			throw new NullPointerException("SampleModel is null");
		}

		if (location == null) {
			location = new TPoint(0, 0);
		}

		// If no DataBuffer provided, ask the SampleModel to make one
		if (db == null) {
			db = sm.createDataBuffer();
		} else {
			// Sanity: SampleModel and DataBuffer must agree on type
			if (db.getDataType() != sm.getDataType()) {
				throw new IllegalArgumentException(
					"DataBuffer type (" + db.getDataType() +
						") does not match SampleModel type (" + sm.getDataType() + ")"
				);
			}
		}

		int w = sm.getWidth();
		int h = sm.getHeight();

		// Your WritableRaster subclass/ctor here
		return new TWritableRaster(sm, db, location.x, location.y, w, h);
	}
}
