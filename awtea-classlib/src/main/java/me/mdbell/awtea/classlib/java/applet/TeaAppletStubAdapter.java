package me.mdbell.awtea.classlib.java.applet;

import me.mdbell.awtea.impl.TeaAppletStub;
import org.teavm.jso.dom.html.HTMLCanvasElement;

import java.net.URL;

/**
 * Adapter that wraps TeaAppletStub to implement the TAppletStub interface.
 * This avoids a circular dependency between awtea-core and awtea-classlib.
 */
class TeaAppletStubAdapter implements TAppletStub {
    
    private final TeaAppletStub delegate;
    
    public TeaAppletStubAdapter(TeaAppletStub delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public boolean isActive() {
        return delegate.isActive();
    }
    
    @Override
    public URL getDocumentBase() {
        return delegate.getDocumentBase();
    }
    
    @Override
    public URL getCodeBase() {
        return delegate.getCodeBase();
    }
    
    @Override
    public String getParameter(String name) {
        return delegate.getParameter(name);
    }
    
    @Override
    public TAppletContext getAppletContext() {
        return null; // Not implemented yet
    }
    
    @Override
    public void appletResize(int width, int height) {
        delegate.appletResize(width, height);
    }
    
    @Override
    public HTMLCanvasElement getCanvas() {
        return delegate.getCanvas();
    }
}
