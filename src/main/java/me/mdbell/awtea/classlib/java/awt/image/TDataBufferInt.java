package me.mdbell.awtea.classlib.java.awt.image;

import lombok.Getter;
import org.teavm.jso.canvas.ImageData;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int32Array;

import java.awt.image.DataBufferInt;

/**
 * @see DataBufferInt
 */
@Getter
public class TDataBufferInt extends TDataBuffer {

	protected int[][] bankData;
	private Int32Array jsArray;

	TDataBufferInt(ImageData imgData) {
		super(TYPE_INT, imgData.getWidth() * imgData.getHeight());
		this.jsArray = new Int32Array(imgData.getData().getBuffer());
		this.bankData = new int[][]{jsArray.toJavaArray()};
	}

	TDataBufferInt(ArrayBuffer buffer, int offset, int size) {
		super(TYPE_INT, size);
		int numInts = TDataBuffer.getDataTypeSize(TYPE_INT) / 8;
		this.jsArray = new Int32Array(buffer, offset, numInts);
		this.bankData = new int[][]{jsArray.toJavaArray()};
	}

	public TDataBufferInt(int size) {
		super(TYPE_INT, size);
		this.bankData = new int[1][size];
	}

	public TDataBufferInt(int size, int numBanks) {
		super(TYPE_INT, size, numBanks);
		this.bankData = new int[numBanks][size];
	}

	public TDataBufferInt(int[] data, int size) {
		super(TYPE_INT, size);
		this.bankData = new int[][]{data};
	}

	public TDataBufferInt(int[] data, int size, int offset) {
		super(TYPE_INT, size, 1, offset);
		this.bankData = new int[][]{data};
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
