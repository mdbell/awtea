package me.mdbell.awtea.classlib.java.applet;

import org.teavm.jso.dom.html.HTMLCanvasElement;

import java.net.URL;

public interface TAppletStub {

    boolean isActive();

    URL getDocumentBase();

    URL getCodeBase();

    String getParameter(String name);

    TAppletContext getAppletContext();

    void appletResize(int width, int height);

    HTMLCanvasElement getCanvas();

}
