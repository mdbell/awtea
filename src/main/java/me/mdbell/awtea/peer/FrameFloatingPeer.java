package me.mdbell.awtea.peer;

import me.mdbell.awtea.ui.FloatingWindow;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLElement;

public final class FrameFloatingPeer extends FloatingWindow {

	private final HTMLCanvasElement canvasElement;

	public FrameFloatingPeer() {
		super("frame-peer-window");
		canvasElement = createElement("canvas");

		canvasElement.setAttribute("tabindex", "0"); // make canvas focusable
		canvasElement.getStyle().setProperty("outline", "none"); // remove focus outline

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

	public CanvasRenderingContext2D getCanvasContext() {
		return (CanvasRenderingContext2D) canvasElement.getContext("2d");
	}

	public HTMLCanvasElement getCanvas() {
		return canvasElement;
	}
}
