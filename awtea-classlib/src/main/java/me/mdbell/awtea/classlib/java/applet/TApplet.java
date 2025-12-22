package me.mdbell.awtea.classlib.java.applet;

import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.classlib.java.awt.*;
import me.mdbell.awtea.classlib.java.awt.event.TPaintEvent;
import me.mdbell.awtea.util.JSObjectsExtensions;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLCanvasElement;

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

	private static final Logger log = LoggerFactory.getLogger(TApplet.class);

	private THeavyCanvas heavyCanvas;
	private TAppletStub stub;

	public TApplet() {
		super();
	}

	@Override
	protected void dispatchPaintEvent(TPaintEvent event) {
		if(heavyCanvas != null) {
			log.trace("TApplet dispatchPaintEvent via heavy canvas");
			TGraphics g = heavyCanvas.getGraphics();
			try {
				update(g);
			}finally {
				g.dispose();
			}
		}else {
			super.dispatchPaintEvent(event);
		}
	}

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
		this.heavyCanvas = createHeavyCanvas();
		if(heavyCanvas != null){
			super.setSize(heavyCanvas.getWidth(), heavyCanvas.getHeight());
			if(stub != null){
				stub.appletResize(heavyCanvas.getWidth(), heavyCanvas.getHeight());
			}
		}
	}

	private THeavyCanvas createHeavyCanvas(){
		String canvasId = System.getProperty("me.mdbell.awtea.classlib.java.awt.Applet.canvasId");
		try {
			if(canvasId == null){
				return null;
			}
			log.info("Creating heavyweight canvas with ID: {}", canvasId);
			HTMLCanvasElement canvasElement = (HTMLCanvasElement) Window.current().getDocument().getElementById(canvasId);
			if(canvasElement == null){
				log.error("Could not find canvas element with ID: {}", canvasId);
				return null;
			}
			return new THeavyCanvas(canvasElement, this).configureStandardEvents();
		}finally {
			// no matter what we unset the canvasId property to avoid affecting nested applets
			System.clearProperty("me.mdbell.awtea.classlib.java.awt.Applet.canvasId");
		}
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
		if (heavyCanvas != null) {
			return heavyCanvas.getGraphics();
		}
		return super.getGraphics();
	}

	@Override
	public void setSize(int width, int height) {
		if(heavyCanvas != null){
			heavyCanvas.resize(width, height);
		}
		super.setSize(width, height);
		if (stub != null) {
			stub.appletResize(width, height);
		}
	}
}
