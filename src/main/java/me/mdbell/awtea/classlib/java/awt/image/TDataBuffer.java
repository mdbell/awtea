package me.mdbell.awtea.classlib.java.awt.image;

import lombok.AccessLevel;
import lombok.Getter;


/**
 * @see java.awt.image.DataBuffer
 */
@Getter
public abstract class TDataBuffer {

	public static final int TYPE_BYTE = 0;
	public static final int TYPE_USHORT = 1;
	public static final int TYPE_SHORT = 2;
	public static final int TYPE_INT = 3;
	public static final int TYPE_FLOAT = 4;
	public static final int TYPE_DOUBLE = 4;
	public static final int TYPE_UNDEFINED = 32;

	protected int dataType;

	// omit Getter for banks, they used getNumBanks() as their method name
	@Getter(AccessLevel.NONE)
	protected int banks;

	protected int offset;

	protected int size;

	protected int[] offsets;

	private static final int[] dataTypeSize = {8,16,16,32,32,64};

	protected TDataBuffer(int dataType, int size) {
		// omit the initial state stuff for now
		this.dataType = dataType;
		this.banks = 1;
		this.size = size;
		this.offset = 0;
		this.offsets = new int[1];
	}

	protected TDataBuffer(int dataType, int size, int numBanks) {
		this.dataType = dataType;
		this.banks = numBanks;
		this.size = size;
		this.offset = 0;
		this.offsets = new int[banks];
	}

	protected TDataBuffer(int dataType, int size, int numBanks, int offset) {
		this.dataType = dataType;
		this.banks = numBanks;
		this.size = size;
		this.offset = offset;
		this.offsets = new int[numBanks];
		for (int i = 0; i < numBanks; i++) {
			this.offsets[i] = offset;
		}
	}

	protected TDataBuffer(int dataType, int size, int numBanks, int[] offsets) {
		if (numBanks != offsets.length) {
			throw new ArrayIndexOutOfBoundsException("Number of banks" +
				" does not match number of bank offsets");
		}
		this.dataType = dataType;
		this.banks = numBanks;
		this.size = size;
		this.offset = offsets[0];
		this.offsets = offsets.clone();
	}

	public int[] getOffsets(){
		return this.offsets.clone();
	}

	public int getNumBanks() {
		return banks;
	}

	public int getElem(int index) {
		return getElem(0, index);
	}

	public abstract int getElem(int bank, int index);

	public void  setElem(int index, int value) {
		setElem(0,index,value);
	}

	public abstract void setElem(int bank, int index, int value);

	public float getElemFloat(int index) {
		return (float) getElem(index);
	}

	public float getElemFloat(int bank, int index) {
		return (float) getElem(bank, index);
	}

	public void setElemFloat(int index, float value){
		setElem(index, (int) value);
	}

	public void setElemFloat(int bank, int index, float value){
		setElem(bank, index, (int) value);
	}

	public double getElemDouble(int index) {
		return getElem(index);
	}

	public double getElemDouble(int bank, int index) {
		return getElem(bank, index);
	}

	public void setElemDouble(int index, double value){
		setElem(index, (int)value);
	}

	public void setElemDouble(int bank, int index, double value){
		setElem(bank, index, (int) value);
	}

	public static int getDataTypeSize(int type){
		if (type < TYPE_BYTE || type > TYPE_DOUBLE) {
			throw new IllegalArgumentException("Unknown data type: " + type);
		}
		return dataTypeSize[type];
	}

}
