package me.mdbell.awtea.classlib.java.awt.awtea.peer;

import me.mdbell.awtea.classlib.java.awt.TFrame;
import me.mdbell.awtea.classlib.java.awt.TGraphics;
import me.mdbell.awtea.classlib.java.awt.THeavyCanvas;
import me.mdbell.awtea.ui.FloatingFrame;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;

public final class TFrameFloatingPeer extends FloatingFrame {

	private final THeavyCanvas heavyCanvas;

	public TFrameFloatingPeer(TFrame component) {
		super("frame-peer-window");

		// Create and configure heavyweight canvas
		heavyCanvas = new THeavyCanvas(Window.current().getDocument(), component);
		heavyCanvas.configureStandardEvents();

		setSize(0, 0); // auto-size to content
	}

	@Override
	public void setSize(int widthPx, int heightPx) {
		heavyCanvas.resize(widthPx, heightPx);
	}

	@Override
	protected String computeSignature() {
		return "";
	}

	@Override
	protected HTMLElement buildBodyContent() {
		return heavyCanvas.getCanvasElement();
	}

	public TGraphics getGraphics() {
		return heavyCanvas.getGraphics();
	}
}
