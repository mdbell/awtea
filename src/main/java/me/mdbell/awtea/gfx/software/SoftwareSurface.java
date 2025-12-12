package me.mdbell.awtea.gfx.software;

import me.mdbell.awtea.gfx.Rasterizer;
import me.mdbell.awtea.gfx.Surface;
import org.teavm.jso.typedarrays.*;

import java.awt.image.*;

public class SoftwareSurface implements Surface {

	private WritableRaster raster;
	private ColorModel cm; // not really needed here yet, maybe in software rasterizer?
	private int format;

	private Uint8ClampedArray pixelData;

	public SoftwareSurface(WritableRaster raster, ColorModel cm, int format) {
		this.raster = raster;
		this.cm = cm;
		this.format = format;

		this.pixelData = getPixelDataFromBuffer(raster.getDataBuffer());

	}

	@Override
	public int getFormat() {
		return format;
	}

	private Uint8ClampedArray getPixelDataFromBuffer(DataBuffer dataBuffer) {
		switch (dataBuffer.getDataType()) {
			case DataBuffer.TYPE_BYTE:
				return getPixeDataFromBuffer((DataBufferByte) dataBuffer);
			case DataBuffer.TYPE_USHORT:
				return getPixelDataFromBuffer((DataBufferUShort) dataBuffer);
			case DataBuffer.TYPE_SHORT:
				return getPixelDataFromBuffer((DataBufferShort) dataBuffer);
			case DataBuffer.TYPE_INT:
				return getPixelDataFromBuffer((DataBufferInt) dataBuffer);
			case DataBuffer.TYPE_FLOAT:
				return getPixelDataFromBuffer((DataBufferFloat) dataBuffer);
			case DataBuffer.TYPE_DOUBLE:
				return getPixelDataFromBuffer((DataBufferDouble) dataBuffer);
			case DataBuffer.TYPE_UNDEFINED:
			default:
				System.err.println("SoftwareSurface: Unsupported DataBuffer type: " + dataBuffer.getDataType());
				return null;
		}
	}

	private Uint8ClampedArray getPixeDataFromBuffer(DataBufferByte buffer) {
		byte[] buff = buffer.getData();
		Int8Array jsArray = Int8Array.fromJavaArray(buff);
		return new Uint8ClampedArray(jsArray.getBuffer(), buffer.getOffset(),
			buffer.getSize());
	}

	private Uint8ClampedArray getPixelDataFromBuffer(DataBufferInt buffer) {
		int[] buff = buffer.getData();
		Int32Array jsArray = Int32Array.fromJavaArray(buff);
		return new Uint8ClampedArray(jsArray.getBuffer(), buffer.getOffset() * 4,
			buffer.getSize() * 4);
	}

	private Uint8ClampedArray getPixelDataFromBuffer(DataBufferUShort buffer) {
		short[] buff = buffer.getData();
		Int16Array jsArray = Int16Array.fromJavaArray(buff);
		return new Uint8ClampedArray(jsArray.getBuffer(), buffer.getOffset() * 2,
			buffer.getSize() * 2);
	}

	private Uint8ClampedArray getPixelDataFromBuffer(DataBufferShort buffer) {
		short[] buff = buffer.getData();
		Int16Array jsArray = Int16Array.fromJavaArray(buff);
		return new Uint8ClampedArray(jsArray.getBuffer(), buffer.getOffset() * 2,
			buffer.getSize() * 2);
	}

	private Uint8ClampedArray getPixelDataFromBuffer(DataBufferFloat buffer) {
		float[] buff = buffer.getData();
		Float32Array jsArray = Float32Array.fromJavaArray(buff);
		return new Uint8ClampedArray(jsArray.getBuffer(), buffer.getOffset() * 4,
			buffer.getSize() * 4);
	}

	private Uint8ClampedArray getPixelDataFromBuffer(DataBufferDouble buffer) {
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
		return pixelData;
	}

	@Override
	public void destroy() {

	}
}
