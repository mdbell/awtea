package me.mdbell.awtea.impl;

import lombok.Getter;
import lombok.SneakyThrows;
import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSString;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import me.mdbell.awtea.util.URLSearchParams;

import java.applet.AppletContext;
import java.applet.AppletStub;
import java.net.URL;
import java.util.Properties;

public class TeaAppletStub implements AppletStub {

    @Getter
    private final HTMLCanvasElement canvas;
    private final Properties props;

    public TeaAppletStub(String canvasId, Properties props) {
        this((HTMLCanvasElement) Window.current().getDocument().getElementById(canvasId), props);
    }

    public TeaAppletStub(HTMLCanvasElement canvas, Properties props) {
        this.canvas = canvas;
        this.props = props;
    }

    @Override
    public void appletResize(int width, int height) {
        if (canvas.getWidth() != width || canvas.getHeight() != height) {
            canvas.setWidth(width);
            canvas.setHeight(height);
        }
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

        String attr = "data-" + name.toLowerCase();
        if (canvas.hasAttribute(attr)) {
            return canvas.getAttribute(attr);
        }

        return props.getProperty(name);
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
