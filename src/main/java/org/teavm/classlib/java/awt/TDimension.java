package org.teavm.classlib.java.awt;

import lombok.*;
import me.mdbell.awtea.classlib.java.awt.geom.TDimension2D;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
public class TDimension extends TDimension2D {

	public int width;
	public int height;

	public TDimension(TDimension d) {
		this.width = d.width;
		this.height = d.height;
	}

	public double getWidth() {
		return width;
	}

	public double getHeight() {
		return height;
	}

	@Override
	public void setSize(double width, double height) {
		this.width = (int) Math.ceil(width);
		this.height = (int) Math.ceil(height);
	}

	public void setSize(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public void setSize(TDimension d) {
		this.width = d.width;
		this.height = d.height;
	}

	public TDimension getSize() {
		return new TDimension(this);
	}
}
