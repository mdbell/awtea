package me.mdbell.awtea.gfx.software;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import me.mdbell.awtea.gfx.Rasterizer;
import me.mdbell.awtea.gfx.Surface;
import org.teavm.classlib.PlatformDetector;
import org.teavm.jso.typedarrays.*;

import java.awt.image.*;
import me.mdbell.awtea.util.TypedArrays;

public class SoftwareSurface implements Surface {

	private static final Logger log = LoggerFactory.getLogger(SoftwareSurface.class);

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

		if (PlatformDetector.isWebAssemblyGC()) {
			// wasm-gc: JS typed arrays cannot alias Java arrays, so the raster's
			// own Java array is the canonical pixel store and getPixelData()
			// materializes a JS-side copy on demand (a per-present cost, akin
			// to putImageData).
			DataBuffer db = raster.getDataBuffer();
			if (db instanceof DataBufferInt) {
				this.intPixels = ((DataBufferInt) db).getData();
			} else {
				log.error("SoftwareSurface: only TYPE_INT rasters are supported under wasm-gc, got type: {}",
					db.getDataType());
				this.intPixels = null;
			}
			this.pixelData = null;
		} else {
			// JS backend: pixelData and intPixels are zero-copy views of the
			// raster's storage — one memory, three names. Writes through any
			// of them are visible everywhere.
			this.pixelData = getPixelDataFromBuffer(raster.getDataBuffer());
			this.intPixels = this.pixelData == null ? null : TypedArrays.toJavaArray(new Int32Array(this.pixelData.getBuffer(), this.pixelData.getByteOffset(),
				this.pixelData.getByteLength() / 4));
		}
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
				log.error("SoftwareSurface: Unsupported DataBuffer type: {}", dataBuffer.getDataType());
				return null;
		}
	}

	private Uint8ClampedArray getPixelDataFromByteBuffer(DataBufferByte buffer) {
		byte[] buff = buffer.getData();
		Int8Array jsArray = TypedArrays.from(buff);
		return new Uint8ClampedArray(jsArray.getBuffer(), buffer.getOffset(),
			buffer.getSize());
	}

	private Uint8ClampedArray getPixelDataFromIntBuffer(DataBufferInt buffer) {
		int[] buff = buffer.getData();
		Int32Array jsArray = TypedArrays.from(buff);
		return new Uint8ClampedArray(jsArray.getBuffer(), buffer.getOffset() * 4,
			buffer.getSize() * 4);
	}

	private Uint8ClampedArray getPixelDataFromUShortBuffer(DataBufferUShort buffer) {
		short[] buff = buffer.getData();
		Int16Array jsArray = TypedArrays.from(buff);
		return new Uint8ClampedArray(jsArray.getBuffer(), buffer.getOffset() * 2,
			buffer.getSize() * 2);
	}

	private Uint8ClampedArray getPixelDataFromShortBuffer(DataBufferShort buffer) {
		short[] buff = buffer.getData();
		Int16Array jsArray = TypedArrays.from(buff);
		return new Uint8ClampedArray(jsArray.getBuffer(), buffer.getOffset() * 2,
			buffer.getSize() * 2);
	}

	private Uint8ClampedArray getPixelDataFromFloatBuffer(DataBufferFloat buffer) {
		float[] buff = buffer.getData();
		Float32Array jsArray = TypedArrays.from(buff);
		return new Uint8ClampedArray(jsArray.getBuffer(), buffer.getOffset() * 4,
			buffer.getSize() * 4);
	}

	private Uint8ClampedArray getPixelDataFromDoubleBuffer(DataBufferDouble buffer) {
		double[] buff = buffer.getData();
		Float64Array jsArray = TypedArrays.from(buff);
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
		if (PlatformDetector.isWebAssemblyGC()) {
			// Fresh JS-side snapshot of the canonical Java array. Callers get
			// current pixels but NOT a live view — hold the int[] from
			// getPixelDataAsInt32Array() instead if a live view is needed.
			if (intPixels == null) {
				return null;
			}
			Int32Array copy = Int32Array.copyFromJavaArray(intPixels);
			return new Uint8ClampedArray(copy.getBuffer(), 0, intPixels.length * 4);
		}
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
