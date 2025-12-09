package me.mdbell.awtea.classlib.java.awt.image;

import lombok.Getter;
import org.teavm.jso.typedarrays.Float64Array;
import org.teavm.jso.typedarrays.Int32Array;

/**
 * @see java.awt.image.DataBufferDouble
 */
@Getter
public class TDataBufferDouble extends TDataBuffer {
	protected double[][] bankData;
	private Int32Array jsArray;

	public TDataBufferDouble(int size) {
		super(TYPE_DOUBLE, size);
		this.bankData = new double[1][size];
	}

	public TDataBufferDouble(int size, int numBanks) {
		super(TYPE_DOUBLE, size, numBanks);
		this.bankData = new double[numBanks][size];
	}

	public TDataBufferDouble(double[] data, int size) {
		this(data, size, 0);
	}

	public TDataBufferDouble(double[] data, int size, int offset) {
		super(TYPE_DOUBLE, size, 1, offset);
		this.bankData = new double[][]{data};
		this.jsArray = new Int32Array(Float64Array.fromJavaArray(data).getBuffer(), offset, size);
	}

	public TDataBufferDouble(double[][] dataArray, int size) {
		super(TYPE_DOUBLE, size, dataArray.length);
		this.bankData = dataArray;
	}

	public TDataBufferDouble(double[][] dataArray, int size, int[] offsets) {
		super(TYPE_DOUBLE, size, dataArray.length, offsets);
		this.bankData = dataArray;
	}

	public double[] getData() {
		return bankData[0];
	}

	public double[] getData(int bank) {
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
		return (float) bankData[0][idx];
	}

	@Override
	public float getElemFloat(int bank, int i) {
		int idx = offsets[bank] + i;
		return (float) bankData[bank][idx];
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

	@Override
	public double getElemDouble(int i) {
		int idx = offsets[0] + i;
		return bankData[0][idx];
	}

	@Override
	public double getElemDouble(int bank, int i) {
		int idx = offsets[bank] + i;
		return bankData[bank][idx];
	}

	@Override
	public void setElemDouble(int i, double val) {
		int idx = offsets[0] + i;
		bankData[0][idx] = val;
	}

	@Override
	public void setElemDouble(int bank, int i, double val) {
		int idx = offsets[bank] + i;
		bankData[bank][idx] = val;
	}
}
