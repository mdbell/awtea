package me.mdbell.awtea.classlib.java.applet;

import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.classlib.java.awt.TContainer;
import me.mdbell.awtea.util.JSObjectsExtensions;

import java.net.URL;

/**
 * @see java.applet.Applet
 */
@ExtensionMethod({JSObjectsExtensions.class})
public class TApplet extends TContainer {

	private TAppletStub stub;

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
	}

	public void init() {
	}

	public void start() {
	}

	public String getParameter(String name) {
		return stub.getParameter(name);
	}


	@Override
	public void setSize(int width, int height) {
		super.setSize(width, height);
		if (stub != null) {
			stub.appletResize(width, height);
		}
	}
}
