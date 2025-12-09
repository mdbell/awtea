package me.mdbell.awtea.classlib.java.awt.awtea.peer;

import me.mdbell.awtea.classlib.java.awt.TCanvasGraphics;
import me.mdbell.awtea.classlib.java.awt.TFrame;
import me.mdbell.awtea.classlib.java.awt.TWebGLGraphics;
import me.mdbell.awtea.classlib.java.awt.awtea.TEventManager;
import me.mdbell.awtea.ui.FloatingWindow;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLElement;

public final class TFrameFloatingPeer extends FloatingWindow {

	private final HTMLCanvasElement canvasElement;
	private final TEventManager eventManager;

	private TCanvasGraphics graphics;

	public TFrameFloatingPeer(TFrame component) {
		super("frame-peer-window");

		canvasElement = createElement("canvas");
		canvasElement.setAttribute("tabindex", "0"); // make canvas focusable
		canvasElement.getStyle().setProperty("outline", "none"); // remove focus outline

		eventManager = new TEventManager(canvasElement, component);

		eventManager.disableContextMenu()
			.withFocus()
			.withKeyboard()
			.withMouse()
			.withMouseWheel();

		setSize(0, 0); // auto-size to content
	}

	@Override
	public void setSize(int widthPx, int heightPx) {
		canvasElement.setWidth(widthPx);
		canvasElement.setHeight(heightPx);
	}

	@Override
	protected String computeSignature() {
		return "";
	}

	@Override
	protected HTMLElement buildBodyContent() {
		return canvasElement;
	}

	public TCanvasGraphics getGraphics() {
		if (graphics == null) {
			//graphics = new TCanvas2DGraphics(canvasElement);
			graphics = new TWebGLGraphics(canvasElement);
		}
		return graphics;
	}

	public HTMLCanvasElement getCanvas() {
		return canvasElement;
	}
}
