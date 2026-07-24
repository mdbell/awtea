package me.mdbell.awtea.classlib.java.awt.image;

import lombok.Getter;
import org.teavm.jso.canvas.ImageData;
import org.teavm.jso.typedarrays.Int32Array;
import org.teavm.jso.typedarrays.Uint8ClampedArray;

import java.awt.image.DataBufferInt;
import me.mdbell.awtea.util.TypedArrays;

/**
 * @see DataBufferInt
 */
@Getter
public class TDataBufferInt extends TDataBuffer {

	protected int[][] bankData;
	private Int32Array jsArray;

	TDataBufferInt(ImageData imgData) {
		this(imgData.getData(), imgData.getWidth(), imgData.getHeight());
	}

	TDataBufferInt(Uint8ClampedArray pixelData, int width, int height) {
		super(TYPE_INT, width * height);
		this.jsArray = new Int32Array(pixelData.getBuffer(), pixelData.getByteOffset(), pixelData.getLength() / 4);
		// ALIAS-DEPENDENT (JS backend): bankData shares memory with jsArray/pixelData,
		// so raster writes show up in the surface. On wasm-gc this is a detached copy —
		// needs the PixelBuffer restructure (docs/wasm-port-plan.md Lane 2).
		this.bankData = new int[][]{TypedArrays.toJavaArray(this.jsArray)};
	}

	public TDataBufferInt(int size) {
		super(TYPE_INT, size);
		this.bankData = new int[1][size];
	}

	@Override
	public Int32Array getJsArray() {
		return jsArray;
	}

	public TDataBufferInt(int size, int numBanks) {
		super(TYPE_INT, size, numBanks);
		this.bankData = new int[numBanks][size];
	}

	public TDataBufferInt(int[] data, int size) {
		this(data, size, 0);
	}

	public TDataBufferInt(int[] data, int size, int offset) {
		super(TYPE_INT, size, 1, offset);
		this.bankData = new int[][]{data};
		this.jsArray = new Int32Array(TypedArrays.from(data).getBuffer(), offset, size);
	}

	public TDataBufferInt(int[][] dataArray, int size) {
		super(TYPE_INT, size, dataArray.length);
		this.bankData = dataArray;
	}

	public TDataBufferInt(int[][] dataArray, int size, int[] offsets) {
		super(TYPE_INT, size, dataArray.length, offsets);
		this.bankData = dataArray;
	}

	public Int32Array getJSArray() {
		return jsArray;
	}

	public int[] getData() {
		return bankData[0];
	}

	public int[] getData(int bank) {
		return bankData[bank];
	}

	@Override
	public int getElem(int i) {
		int idx = offsets[0] + i;
		return bankData[0][idx];
	}

	@Override
	public int getElem(int bank, int i) {
		int idx = offsets[bank] + i;
		return bankData[bank][idx];
	}

	@Override
	public void setElem(int i, int val) {
		int idx = offsets[0] + i;
		bankData[0][idx] = val;
	}

	@Override
	public void setElem(int bank, int i, int val) {
		int idx = offsets[bank] + i;
		bankData[bank][idx] = val;
	}
}
