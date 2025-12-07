package me.mdbell.awtea.classlib.java.applet;

import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.classlib.java.awt.TCanvas2DGraphics;
import me.mdbell.awtea.classlib.java.awt.TCanvasGraphics;
import me.mdbell.awtea.classlib.java.awt.TContainer;
import me.mdbell.awtea.classlib.java.awt.TGraphics;
import me.mdbell.awtea.classlib.java.awt.awtea.TEventManager;
import me.mdbell.awtea.util.JSObjectsExtensions;
import org.teavm.jso.canvas.CanvasRenderingContext2D;

import java.net.URL;

/**
 * @see java.applet.Applet
 */
@ExtensionMethod({JSObjectsExtensions.class})
public class TApplet extends TContainer {

	private TAppletStub stub;
	private TCanvasGraphics graphics;
	private CanvasRenderingContext2D context;

	private TEventManager eventManager;

	public URL getCodeBase() {
		return stub.getCodeBase();
	}

	public URL getDocumentBase() {
		return stub.getDocumentBase();
	}

	public TAppletContext getAppletContext() {
		return stub.getAppletContext();
	}

	public final void setStub(TAppletStub stub) {
		this.stub = stub;
		this.context = stub.getCanvas().getContext2d(false);
		this.graphics = null;
		if (eventManager != null) {
			eventManager.detach();
		}

		eventManager = new TEventManager(stub.getCanvas(), this);

		eventManager.withFocus()
			.withKeyboard()
			.withMouse()
			.withMouseWheel()
			.disableContextMenu();
	}

	public void init() {
	}

	public void start() {
	}

	public String getParameter(String name) {
		return stub.getParameter(name);
	}

	@Override
	public TGraphics getGraphics() {
		if (this.graphics == null) {
			this.graphics = new TCanvas2DGraphics(this.context);
		}
		return this.graphics;
	}

	@Override
	public void setSize(int width, int height) {
		super.setSize(width, height);
		if (stub != null) {
			stub.appletResize(width, height);
		}
	}
}
