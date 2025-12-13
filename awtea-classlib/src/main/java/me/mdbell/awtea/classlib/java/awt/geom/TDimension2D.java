package me.mdbell.awtea.classlib.java.awt.geom;

import lombok.SneakyThrows;

public abstract class TDimension2D implements Cloneable{

	protected TDimension2D() {

	}

	public abstract double getWidth();
	public abstract double getHeight();

	public abstract void setSize(double width, double height);

	public void setSize(TDimension2D d) {
		setSize(d.getWidth(), d.getHeight());
	}

	@SneakyThrows
	public Object clone() {
		return super.clone();
	}
}
