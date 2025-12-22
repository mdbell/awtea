package me.mdbell.awtea.util;

import java.applet.AppletContext;
import java.applet.AppletStub;
import java.net.URL;

public class StubAppletStub implements AppletStub {

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public URL getDocumentBase() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDocumentBase'");
    }

    @Override
    public URL getCodeBase() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCodeBase'");
    }

    @Override
    public String getParameter(String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getParameter'");
    }

    @Override
    public AppletContext getAppletContext() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAppletContext'");
    }

    @Override
    public void appletResize(int width, int height) {
        // no-op
    }

}
