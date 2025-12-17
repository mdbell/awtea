package me.mdbell.awtea.classlib.java.applet;

import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.classlib.java.awt.TPanel;
import me.mdbell.awtea.util.JSObjectsExtensions;

import java.net.URL;

/**
 * An applet is a special component that can be embedded in a web page.
 * This class extends TPanel to provide proper layout management and
 * compositional usage in the component hierarchy.
 * 
 * <p>Like in standard AWT, TApplet extends Panel rather than Container directly,
 * which provides a default FlowLayout and makes it more convenient for composition.
 * 
 * @see java.applet.Applet
 * @see TPanel
 */
@ExtensionMethod({JSObjectsExtensions.class})
public class TApplet extends TPanel {

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
