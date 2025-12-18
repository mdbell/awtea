package me.mdbell.awtea.classlib.java.awt;

import java.awt.Color;

import lombok.Setter;
import me.mdbell.awtea.classlib.java.awt.awtea.peer.TSurfacePeer;
import me.mdbell.awtea.classlib.java.awt.event.TPaintEvent;

@Setter
abstract class TSurface extends TContainer {

	protected TSurfacePeer surfacePeer;

	protected TSurface() {
		setBackground(new Color(220, 220, 220));
	}

	/**
	 * Gets the graphics context for the real surface.
	 *
	 * @return The graphics context for the surface.
	 */
	public abstract TGraphics getSurfaceGraphics();

	/**
	 * Acts as a fast-path to do rendering, bypassing any offscreen buffers
	 * (This is used if a component [e.g an Applet that performs it's own buffering]
	 * wants to draw directly to the screen).
	 * <p>
	 * Ideally rendering shouldn't use this method, but rather the normal paint
	 * mechanism.
	 *
	 * @return The graphics context for the surface.
	 */
	@Override
	public TGraphics getGraphics() {
		return getSurfaceGraphics();
	}

	@Override
	protected void dispatchPaintEvent(TPaintEvent event) {
		if (surfacePeer != null) {
			surfacePeer.paintAll();
		}
	}

	@Override
	public void update(TGraphics g) {
		paint(g);
	}

	@Override
	public void setSize(int width, int height) {
		super.setSize(width, height);
		if (surfacePeer != null) {
			surfacePeer.resize(width, height);
		}
	}
}
