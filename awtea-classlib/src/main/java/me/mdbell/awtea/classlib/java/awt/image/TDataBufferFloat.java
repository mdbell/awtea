package me.mdbell.awtea.classlib.java.awt.image;

import lombok.Getter;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Int32Array;

/**
 * @see java.awt.image.DataBufferFloat
 */
@Getter
public class TDataBufferFloat extends TDataBuffer {
	protected float[][] bankData;
	private Int32Array jsArray;

	public TDataBufferFloat(int size) {
		super(TYPE_FLOAT, size);
		this.bankData = new float[1][size];
	}

	public TDataBufferFloat(int size, int numBanks) {
		super(TYPE_FLOAT, size, numBanks);
		this.bankData = new float[numBanks][size];
	}

	public TDataBufferFloat(float[] data, int size) {
		this(data, size, 0);
	}

	public TDataBufferFloat(float[] data, int size, int offset) {
		super(TYPE_FLOAT, size, 1, offset);
		this.bankData = new float[][]{data};
		this.jsArray = new Int32Array(Float32Array.fromJavaArray(data).getBuffer(), offset, size);
	}

	public TDataBufferFloat(float[][] dataArray, int size) {
		super(TYPE_FLOAT, size, dataArray.length);
		this.bankData = dataArray;
	}

	public TDataBufferFloat(float[][] dataArray, int size, int[] offsets) {
		super(TYPE_FLOAT, size, dataArray.length, offsets);
		this.bankData = dataArray;
	}

	public float[] getData() {
		return bankData[0];
	}

	public float[] getData(int bank) {
		return bankData[bank];
	}

	@Override
	public int getElem(int i) {
		int idx = offsets[0] + i;
		return (int) bankData[0][idx];
	}

	@Override
	public int getElem(int bank, int i) {
		int idx = offsets[bank] + i;
		return (int) bankData[bank][idx];
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

	@Override
	public float getElemFloat(int i) {
		int idx = offsets[0] + i;
		return bankData[0][idx];
	}

	@Override
	public float getElemFloat(int bank, int i) {
		int idx = offsets[bank] + i;
		return bankData[bank][idx];
	}

	@Override
	public void setElemFloat(int i, float val) {
		int idx = offsets[0] + i;
		bankData[0][idx] = val;
	}

	@Override
	public void setElemFloat(int bank, int i, float val) {
		int idx = offsets[bank] + i;
		bankData[bank][idx] = val;
	}
}
