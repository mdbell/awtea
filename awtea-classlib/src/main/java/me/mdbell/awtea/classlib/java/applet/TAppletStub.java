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
    
    /**
     * Gets the peer associated with this stub, if any.
     * This allows the stub to provide a heavyweight peer to the applet.
     * Returns Object to avoid circular dependencies - will be cast to TAppletPeer in TApplet.
     * 
     * @return The peer as an Object, or null if this is a lightweight applet
     */
    default Object getPeer() {
        return null;
    }

}
