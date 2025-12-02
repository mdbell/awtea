package me.mdbell.awtea.polyfill.java.awt;

import lombok.*;
import me.mdbell.awtea.transform.AwtPolyfillTransformer;

import java.awt.geom.Point2D;

/**
 * Not actually deprecated, but marked so to remind
 * us to use the base awt.Point.
 * NOTE: TeaVM has their own TPoint implementation, but it's incomplete.
 * This class is dynamically injected into their implementation, replacing all fields +
 * methods - however calling external classes is _highly_ discouraged since
 * this class is 'special' - it should not reference itself, instead
 * it should reference "Point" getLocation - additionally no other could should
 * @see AwtPolyfillTransformer for where it's injected
 * @see java.awt.Point for the baseclass
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@Deprecated
@EqualsAndHashCode(callSuper = false)
public class TPoint extends Point2D {
	public int x;
	public int y;

	public TPoint(TPoint p){
		this.x = p.x;
		this.y = p.y;
	}

	public double getX(){
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
		this.x = (int) Math.floor(x+0.5);
		this.y = (int) Math.floor(y+0.5);
	}
}
