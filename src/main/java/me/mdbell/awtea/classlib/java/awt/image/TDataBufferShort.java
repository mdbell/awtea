package me.mdbell.awtea.classlib.java.awt.image;

import lombok.Getter;
import org.teavm.jso.typedarrays.Int16Array;
import org.teavm.jso.typedarrays.Int32Array;

/**
 * @see java.awt.image.DataBufferShort
 */
@Getter
public class TDataBufferShort extends TDataBuffer {
	protected short[][] bankData;
	private Int32Array jsArray;

	public TDataBufferShort(int size) {
		super(TYPE_SHORT, size);
		this.bankData = new short[1][size];
	}

	public TDataBufferShort(int size, int numBanks) {
		super(TYPE_SHORT, size, numBanks);
		this.bankData = new short[numBanks][size];
	}

	public TDataBufferShort(short[] data, int size) {
		this(data, size, 0);
	}

	public TDataBufferShort(short[] data, int size, int offset) {
		super(TYPE_SHORT, size, 1, offset);
		this.bankData = new short[][]{data};
		this.jsArray = new Int32Array(Int16Array.fromJavaArray(data).getBuffer(), offset, size);
	}

	public TDataBufferShort(short[][] dataArray, int size) {
		super(TYPE_SHORT, size, dataArray.length);
		this.bankData = dataArray;
	}

	public TDataBufferShort(short[][] dataArray, int size, int[] offsets) {
		super(TYPE_SHORT, size, dataArray.length, offsets);
		this.bankData = dataArray;
	}

	public short[] getData() {
		return bankData[0];
	}

	public short[] getData(int bank) {
		return bankData[bank];
	}

	@Override
	public int getElem(int i) {
		int idx = offsets[0] + i;
		return bankData[0][idx]; // sign-extended
	}

	@Override
	public int getElem(int bank, int i) {
		int idx = offsets[bank] + i;
		return bankData[bank][idx];
	}

	@Override
	public void setElem(int i, int val) {
		int idx = offsets[0] + i;
		bankData[0][idx] = (short) val;
	}

	@Override
	public void setElem(int bank, int i, int val) {
		int idx = offsets[bank] + i;
		bankData[bank][idx] = (short) val;
	}
}
