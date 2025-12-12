package me.mdbell.awtea.classlib.java.awt.awtea.peer;

import me.mdbell.awtea.classlib.java.awt.TFrame;
import me.mdbell.awtea.classlib.java.awt.TSurfaceRasterizerGraphics;
import me.mdbell.awtea.classlib.java.awt.awtea.TEventManager;
import me.mdbell.awtea.gfx.DefaultSurfaceBackend;
import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.ui.FloatingWindow;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLElement;

public final class TFrameFloatingPeer extends FloatingWindow {

	private final HTMLCanvasElement canvasElement;
	private final TEventManager eventManager;

	private TSurfaceRasterizerGraphics graphics;

	private Surface surface;

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

		canvasElement.setWidth(10);
		canvasElement.setHeight(10);

		surface = DefaultSurfaceBackend.getDefault().createScreenSurface(
			canvasElement.getWidth(),
			canvasElement.getHeight(),
			canvasElement
		);
	}

	@Override
	public void setSize(int widthPx, int heightPx) {
		canvasElement.setWidth(widthPx);
		canvasElement.setHeight(heightPx);

		if (surface != null) {
			surface.resize(widthPx, heightPx);
		}
	}

	@Override
	protected String computeSignature() {
		return "";
	}

	@Override
	protected HTMLElement buildBodyContent() {
		return canvasElement;
	}

	public TSurfaceRasterizerGraphics getGraphics() {
		if (graphics == null) {
			//graphics = new TCanvas2DGraphics(canvasElement);
			//graphics = new TCanvasGraphics(canvasElement, true);
			graphics = new TSurfaceRasterizerGraphics((
				surface.createRasterizer()));
		}
		return graphics;
	}
}
