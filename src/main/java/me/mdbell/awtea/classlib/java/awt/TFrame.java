package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.awtea.TEventManager;
import me.mdbell.awtea.classlib.java.awt.event.TWindowListener;
import me.mdbell.awtea.classlib.java.awt.image.TBufferedImage;
import me.mdbell.awtea.peer.FrameFloatingPeer;

import java.awt.*;
import java.awt.event.ComponentListener;

public class TFrame extends TContainer {

	private final FrameFloatingPeer peer;

	private final TCanvas2DGraphics graphics;

	private final TEventManager eventManager;

	private TBufferedImage offscreenBuffer;

	// Constructor
	public TFrame() {
		peer = new FrameFloatingPeer();
		graphics = new TCanvas2DGraphics(peer.getCanvasContext());

		eventManager = new TEventManager(peer.getCanvas(), this);

		eventManager.disableContextMenu()
			.withFocus()
			.withKeyboard()
			.withMouse()
			.withMouseWheel();

		offscreenBuffer = new TBufferedImage(1, 1);
	}

	public TGraphics getGraphics() {
		return graphics;
	}

	public void update(TGraphics g) {
		paint(g);
	}

	@Override
	public void paint(TGraphics g) {
		TGraphics offscreenGfx = offscreenBuffer.getGraphics();
		super.paint(offscreenGfx);
		g.drawImage(offscreenBuffer, 0, 0, null);
	}

	// Set the title of the frame
	public void setTitle(String title) {
		peer.setTitle(title);
	}

	// Set whether the frame is resizable
	public void setResizable(boolean resizable) {
		peer.setResizeable(resizable);
	}

	// Add a window listener
	public void addWindowListener(TWindowListener l) {
		// Implement event listener logic
	}

	// Set the frame visible
	public void setVisible(boolean b) {
		peer.setVisible(b);
	}

	public void toFront() {
		peer.bringToFront();
	}

	// Get Insets, using the TInsets class
	public TInsets getInsets() {
		return new TInsets(0, 0, 0, 0);
	}

	public void setSize(int width, int height) {
		if (width == getWidth() && height == getHeight()) {
			return;
		}
		super.setSize(width, height);
		offscreenBuffer = new TBufferedImage(width, height);
		peer.setSize(width, height);
		repaint();
	}

	public void setPreferredSize(Dimension dim) {
		// No-op mock
	}

	public void setMinimumSize(Dimension dim) {
		// No-op mock
	}

	public void addComponentListener(ComponentListener l) {

	}
}
