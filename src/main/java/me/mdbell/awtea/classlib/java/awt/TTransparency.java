package me.mdbell.awtea.classlib.java.awt;

public interface TTransparency {

	public static final int OPAQUE = 1;
	public static final int BITMASK = 2;
	public static final int TRANSLUCENT = 3;

	public int getTransparency();
}
