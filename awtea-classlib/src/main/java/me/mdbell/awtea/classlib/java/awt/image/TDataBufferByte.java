package me.mdbell.awtea.classlib.java.awt.image;

import lombok.Getter;
import org.teavm.jso.typedarrays.Int32Array;
import org.teavm.jso.typedarrays.Int8Array;

/**
 * @see java.awt.image.DataBufferByte
 */
@Getter
public class TDataBufferByte extends TDataBuffer {
	protected byte[][] bankData;
	private Int32Array jsArray;

	public TDataBufferByte(int size) {
		super(TYPE_BYTE, size);
		this.bankData = new byte[1][size];
	}

	public TDataBufferByte(int size, int numBanks) {
		super(TYPE_BYTE, size, numBanks);
		this.bankData = new byte[numBanks][size];
	}

	public TDataBufferByte(byte[] data, int size) {
		this(data, size, 0);
	}

	public TDataBufferByte(byte[] data, int size, int offset) {
		super(TYPE_BYTE, size, 1, offset);
		this.bankData = new byte[][]{data};
		this.jsArray = new Int32Array(Int8Array.fromJavaArray(data).getBuffer(), offset, size);
	}

	public TDataBufferByte(byte[][] dataArray, int size) {
		super(TYPE_BYTE, size, dataArray.length);
		this.bankData = dataArray;
	}

	public TDataBufferByte(byte[][] dataArray, int size, int[] offsets) {
		super(TYPE_BYTE, size, dataArray.length, offsets);
		this.bankData = dataArray;
	}

	public byte[] getData() {
		return bankData[0];
	}

	public byte[] getData(int bank) {
		return bankData[bank];
	}

	@Override
	public int getElem(int i) {
		int idx = offsets[0] + i;
		return bankData[0][idx] & 0xFF;
	}

	@Override
	public int getElem(int bank, int i) {
		int idx = offsets[bank] + i;
		return bankData[bank][idx] & 0xFF;
	}

	@Override
	public void setElem(int i, int val) {
		int idx = offsets[0] + i;
		bankData[0][idx] = (byte) val;
	}

	@Override
	public void setElem(int bank, int i, int val) {
		int idx = offsets[bank] + i;
		bankData[bank][idx] = (byte) val;
	}
}
