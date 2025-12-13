package me.mdbell.awtea.ui;

import me.mdbell.awtea.impl.TeaAppletStub;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLElement;

import java.applet.Applet;
import java.util.Properties;

public class AppletWindow extends FloatingFrame {

	private Applet applet;
	private TeaAppletStub stub;
	private final HTMLCanvasElement canvasElement;

	public AppletWindow(Applet applet, String titleText, Properties props) {
		super("applet-" + applet.hashCode(), titleText, applet.getWidth(), applet.getHeight(), 0);

		setScrollable(false);
		setResizeable(false);
		setSize(0, 0); // auto-size to content
		canvasElement = createElement("canvas");

		canvasElement.setAttribute("tabindex", "0"); // make canvas focusable
		canvasElement.getStyle().setProperty("outline", "none"); // remove focus outline

//		applet.setStub(stub = new TeaAppletStub(canvasElement, props) {
//			@Override
//			public void appletResize(int width, int height) {
//				canvasElement.setWidth(width);
//				canvasElement.setHeight(height);
//			}
//		});
	}

	@Override
	protected String computeSignature() {
		return ""; // No dynamic content, static signature
	}

	@Override
	protected HTMLElement buildBodyContent() {
		return canvasElement;
	}
}
