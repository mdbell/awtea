package org.teavm.classlib.java.awt;

import lombok.*;

import java.awt.geom.Point2D;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode(callSuper = false)
public class TPoint extends Point2D {
	public int x;
	public int y;

	public TPoint(TPoint p) {
		this.x = p.x;
		this.y = p.y;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public TPoint getLocation() {
		return new TPoint(x, y);
	}

	public void setLocation(TPoint p) {
		this.x = p.x;
		this.y = p.y;
	}

	public void setLocation(int x, int y) {
		move(x, y);
	}

	public void move(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public void translate(int dx, int dy) {
		this.x += dx;
		this.y += dy;
	}

	@Override
	public void setLocation(double x, double y) {
		this.x = (int) Math.floor(x + 0.5);
		this.y = (int) Math.floor(y + 0.5);
	}
}
