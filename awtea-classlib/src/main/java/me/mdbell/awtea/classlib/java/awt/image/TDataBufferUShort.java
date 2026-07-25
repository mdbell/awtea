package me.mdbell.awtea.classlib.java.awt.image;

import lombok.Getter;
import org.teavm.jso.typedarrays.Int16Array;
import org.teavm.jso.typedarrays.Int32Array;
import me.mdbell.awtea.util.TypedArrays;

/**
 * @see java.awt.image.DataBufferUShort
 */
@Getter
public class TDataBufferUShort extends TDataBuffer {
	protected short[][] bankData;
	private Int32Array jsArray;

	public TDataBufferUShort(int size) {
		super(TYPE_USHORT, size);
		this.bankData = new short[1][size];
	}

	public TDataBufferUShort(int size, int numBanks) {
		super(TYPE_USHORT, size, numBanks);
		this.bankData = new short[numBanks][size];
	}

	public TDataBufferUShort(short[] data, int size) {
		this(data, size, 0);
	}

	public TDataBufferUShort(short[] data, int size, int offset) {
		super(TYPE_USHORT, size, 1, offset);
		this.bankData = new short[][]{data};
		this.jsArray = new Int32Array(TypedArrays.from(data).getBuffer(), offset, size);
	}

	public TDataBufferUShort(short[][] dataArray, int size) {
		super(TYPE_USHORT, size, dataArray.length);
		this.bankData = dataArray;
	}

	public TDataBufferUShort(short[][] dataArray, int size, int[] offsets) {
		super(TYPE_USHORT, size, dataArray.length, offsets);
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
		return bankData[0][idx] & 0xFFFF;
	}

	@Override
	public int getElem(int bank, int i) {
		int idx = offsets[bank] + i;
		return bankData[bank][idx] & 0xFFFF;
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
