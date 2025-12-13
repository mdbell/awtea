package me.mdbell.awtea.gfx.software;

import me.mdbell.awtea.gfx.Rasterizer;
import me.mdbell.awtea.gfx.Surface;
import org.teavm.jso.typedarrays.*;

import java.awt.image.*;

public class SoftwareSurface implements Surface {

	private final WritableRaster raster;
	private final ColorModel cm; // not really needed here yet, maybe in software rasterizer?
	private final int format;
	private boolean dirty = true;

	private final Uint8ClampedArray pixelData;
	private final int[] intPixels;

	public SoftwareSurface(WritableRaster raster, ColorModel cm, int format) {
		this.raster = raster;
		this.cm = cm;
		this.format = format;

		this.pixelData = getPixelDataFromBuffer(raster.getDataBuffer());

		this.intPixels = this.pixelData == null ? null : new Int32Array(this.pixelData.getBuffer(), this.pixelData.getByteOffset(),
			this.pixelData.getByteLength() / 4).toJavaArray();
	}

	@Override
	public int getFormat() {
		return format;
	}

	void markDirty() {
		this.dirty = true;
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	private Uint8ClampedArray getPixelDataFromBuffer(DataBuffer dataBuffer) {
		switch (dataBuffer.getDataType()) {
			case DataBuffer.TYPE_BYTE:
				return getPixelDataFromByteBuffer((DataBufferByte) dataBuffer);
			case DataBuffer.TYPE_USHORT:
				return getPixelDataFromUShortBuffer((DataBufferUShort) dataBuffer);
			case DataBuffer.TYPE_SHORT:
				return getPixelDataFromShortBuffer((DataBufferShort) dataBuffer);
			case DataBuffer.TYPE_INT:
				return getPixelDataFromIntBuffer((DataBufferInt) dataBuffer);
			case DataBuffer.TYPE_FLOAT:
				return getPixelDataFromFloatBuffer((DataBufferFloat) dataBuffer);
			case DataBuffer.TYPE_DOUBLE:
				return getPixelDataFromDoubleBuffer((DataBufferDouble) dataBuffer);
			case DataBuffer.TYPE_UNDEFINED:
			default:
				System.err.println("SoftwareSurface: Unsupported DataBuffer type: " + dataBuffer.getDataType());
				return null;
		}
	}

	private Uint8ClampedArray getPixelDataFromByteBuffer(DataBufferByte buffer) {
		byte[] buff = buffer.getData();
		Int8Array jsArray = Int8Array.fromJavaArray(buff);
		return new Uint8ClampedArray(jsArray.getBuffer(), buffer.getOffset(),
			buffer.getSize());
	}

	private Uint8ClampedArray getPixelDataFromIntBuffer(DataBufferInt buffer) {
		int[] buff = buffer.getData();
		Int32Array jsArray = Int32Array.fromJavaArray(buff);
		return new Uint8ClampedArray(jsArray.getBuffer(), buffer.getOffset() * 4,
			buffer.getSize() * 4);
	}

	private Uint8ClampedArray getPixelDataFromUShortBuffer(DataBufferUShort buffer) {
		short[] buff = buffer.getData();
		Int16Array jsArray = Int16Array.fromJavaArray(buff);
		return new Uint8ClampedArray(jsArray.getBuffer(), buffer.getOffset() * 2,
			buffer.getSize() * 2);
	}

	private Uint8ClampedArray getPixelDataFromShortBuffer(DataBufferShort buffer) {
		short[] buff = buffer.getData();
		Int16Array jsArray = Int16Array.fromJavaArray(buff);
		return new Uint8ClampedArray(jsArray.getBuffer(), buffer.getOffset() * 2,
			buffer.getSize() * 2);
	}

	private Uint8ClampedArray getPixelDataFromFloatBuffer(DataBufferFloat buffer) {
		float[] buff = buffer.getData();
		Float32Array jsArray = Float32Array.fromJavaArray(buff);
		return new Uint8ClampedArray(jsArray.getBuffer(), buffer.getOffset() * 4,
			buffer.getSize() * 4);
	}

	private Uint8ClampedArray getPixelDataFromDoubleBuffer(DataBufferDouble buffer) {
		double[] buff = buffer.getData();
		Float64Array jsArray = Float64Array.fromJavaArray(buff);
		return new Uint8ClampedArray(jsArray.getBuffer(), buffer.getOffset() * 8,
			buffer.getSize() * 8);
	}

	@Override
	public Rasterizer createRasterizer() {
		return new SoftwareRasterizer(this);
	}

	@Override
	public void resize(int width, int height) {
		// unsupported
	}

	@Override
	public int getWidth() {
		return raster.getWidth();
	}

	@Override
	public int getHeight() {
		return raster.getHeight();
	}

	@Override
	public Uint8ClampedArray getPixelData() {
		// Clear dirty flag when pixel data is accessed
		// This allows consumers to track if surface has been modified since last read
		dirty = false;
		return pixelData;
	}

	public int[] getPixelDataAsInt32Array() {
		// Clear dirty flag when pixel data is accessed
		// This allows consumers to track if surface has been modified since last read
		dirty = false;
		return intPixels;
	}

	@Override
	public void destroy() {

	}
}
