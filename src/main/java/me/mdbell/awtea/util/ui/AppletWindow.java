package me.mdbell.awtea.util.ui;

import me.mdbell.awtea.impl.TeaAppletStub;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLElement;

import java.applet.Applet;
import java.applet.AppletStub;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

public class AppletWindow extends FloatingWindow {

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

		applet.setStub(stub = new TeaAppletStub(canvasElement, props){
			@Override
			public URL getCodeBase() {
				String url = getParameter("codebase");
				if (url == null) {
					url = Window.current().getLocation().getFullURL();
				}
				try {
					return new URL(url);
				} catch (MalformedURLException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public void appletResize(int width, int height) {
				canvasElement.setWidth(width);
				canvasElement.setHeight(height);
			}
		});
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
