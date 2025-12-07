package me.mdbell.awtea.classlib.java.applet;

import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.impl.Debug;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import me.mdbell.awtea.classlib.java.awt.TCanvas2DGraphics;
import me.mdbell.awtea.classlib.java.awt.TContainer;
import me.mdbell.awtea.classlib.java.awt.TCanvasGraphics;
import me.mdbell.awtea.classlib.java.awt.TGraphics;
import me.mdbell.awtea.classlib.java.awt.event.TFocusEvent;
import me.mdbell.awtea.classlib.java.awt.event.TKeyEvent;
import me.mdbell.awtea.classlib.java.awt.event.TMouseEvent;
import me.mdbell.awtea.classlib.java.awt.event.TMouseWheelEvent;
import me.mdbell.awtea.input.MouseEventType;
import me.mdbell.awtea.util.JSObjectsExtensions;
import org.teavm.jso.json.JSON;

import java.net.URL;

@ExtensionMethod({JSObjectsExtensions.class})
public class TApplet extends TContainer {

    private TAppletStub stub;
    private TCanvasGraphics graphics;
    private CanvasRenderingContext2D context;

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
        this.context = stub.getCanvas().getContext2d(false);
        this.graphics = null;
        registerEventHandlers();
    }

    private void registerEventHandlers() {
        HTMLCanvasElement canvasElement = stub.getCanvas();

		canvasElement.focus();

        // stops the right click menu from opening on the canvas
        canvasElement.onEvent("contextmenu", Event::preventDefault);

        // Mouse events
        for (MouseEventType event : MouseEventType.values()) {
            canvasElement.addEventListener(event.getType(), e -> {
                MouseEvent evt = (MouseEvent) e;
                dispatchEvent(TMouseEvent.adapt(this, canvasElement, evt, event.getType()));
            });
        }

        // Mouse wheel events
        canvasElement.addEventListener("wheel", e -> {
            dispatchEvent(TMouseWheelEvent.adapt(this, canvasElement, (org.teavm.jso.dom.events.WheelEvent) e, "wheel"));
        });

        // Keyboard events
        for (TKeyEvent.KeyEvent event : TKeyEvent.KeyEvent.values()) {
            canvasElement.addEventListener(event.getType(), e -> {
                dispatchEvent(TKeyEvent.adapt(this, (KeyboardEvent) e));
            });
        }

        // Focus events
        canvasElement.addEventListener("focus", e -> {
            dispatchEvent(new TFocusEvent(this, TFocusEvent.FOCUS_GAINED));
        });
        canvasElement.addEventListener("blur", e -> {
            dispatchEvent(new TFocusEvent(this, TFocusEvent.FOCUS_LOST));
        });
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
        if (this.graphics == null) {
            this.graphics = new TCanvas2DGraphics(this.context);
        }
        return this.graphics;
    }

    @Override
    public void setSize(int width, int height) {
		super.setSize(width, height);
		if(stub != null) {
			stub.appletResize(width, height);
		}
    }
}
