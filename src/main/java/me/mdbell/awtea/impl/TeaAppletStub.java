package me.mdbell.awtea.impl;

import lombok.SneakyThrows;
import me.mdbell.awtea.util.URLSearchParams;
import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSString;

import java.applet.AppletContext;
import java.applet.AppletStub;
import java.net.URL;
import java.util.Properties;

public class TeaAppletStub implements AppletStub {

	private final Properties props;


	public TeaAppletStub(Properties props) {
		this.props = props;
	}

	@Override
	public void appletResize(int width, int height) {
		// no-op - resizing should be handled by the container
	}

	@Override
	public AppletContext getAppletContext() {
		return null;
	}

	@SneakyThrows
	@Override
	public URL getCodeBase() {
		JSString jsUrl = getImportMeta("url");
		String url = jsUrl.stringValue();
		// the url contains the js filename, so we need to remove it
		url = url.substring(0, url.lastIndexOf('/') + 1);
		return new URL(url);
	}

	@SneakyThrows
	@Override
	public URL getDocumentBase() {
		return new URL(Window.current().getLocation().getFullURL());
	}

	@JSBody(params = {"key"}, script = "return import.meta[key];")
	private static native <T> T getImportMeta(String key);

	@Override
	public String getParameter(String name) {

		// tiered params, first check query params, then canvas data attributes, then the default properties

		// get query params
		URLSearchParams searchParams = new URLSearchParams(Window.current().getLocation().getSearch());

		if (searchParams.has(name)) {
			return searchParams.get(name);
		}

//        String attr = "data-" + name.toLowerCase();
//        if (canvas.hasAttribute(attr)) {
//            return canvas.getAttribute(attr);
//        }

		return props.getProperty(name);
	}

	@Override
	public boolean isActive() {
		return true;
	}
}
