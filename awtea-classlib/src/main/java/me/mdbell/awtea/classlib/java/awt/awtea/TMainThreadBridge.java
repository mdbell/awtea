package me.mdbell.awtea.classlib.java.awt.awtea;

import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.util.JSObjectsExtensions;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.MessageEvent;
import org.teavm.jso.typedarrays.ArrayBuffer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Worker-side postMessage bridge to the main thread.
 *
 * Follows the same ID-based request/response pattern as OPFSWorkerVirtualFileAccessor:
 * each outgoing request gets a unique integer ID, and the persistent onmessage handler
 * resolves the matching JSPromise when the main thread replies.
 *
 * The main thread shim (worker-init.js) handles the other side of each message type.
 */
@ExtensionMethod({JSObjectsExtensions.class})
public final class TMainThreadBridge {

    private static int nextId = 1;
    private static final Map<Integer, Consumer<BridgeResponse>> pending = new HashMap<>();

    // Transferred OffscreenCanvas received during 'init' — held as raw JSObject
    // so it can be passed to WebGL context creation without needing the full
    // HTMLCanvasElement type.
    private static JSObject offscreenCanvas;

    private static int initWidth;
    private static int initHeight;

    // Registered by TEventManager.withWorkerEvents() to receive forwarded DOM events
    // and resize notifications from the main thread.
    private static Consumer<BridgeResponse> eventListener;

    private TMainThreadBridge() {}

    public static void setEventListener(Consumer<BridgeResponse> listener) {
        eventListener = listener;
    }

    /**
     * Called once during TWorkerToolkit construction.
     * Reads the init payload stored in self.pendingInit by worker-entry.js,
     * then wires the persistent onmessage response handler.
     */
    public static void init() {
        InitMessage msg = getPendingInit();
        offscreenCanvas = msg.getOffscreenCanvas();
        initWidth = msg.getWidth();
        initHeight = msg.getHeight();

        setOnMessage(evt -> {
            BridgeResponse resp = (BridgeResponse) evt.getData();
            if (resp.nullish()) return;
            String type = resp.getType();
            // Incoming events/resize notifications are forwarded to whoever registered
            if (("event".equals(type) || "resize".equals(type)) && eventListener != null) {
                eventListener.accept(resp);
                return;
            }
            // All other messages are responses to pending requests
            Consumer<BridgeResponse> resolve = pending.remove(resp.getId());
            if (resolve != null) {
                resolve.accept(resp);
            }
        });
    }

    public static JSObject getOffscreenCanvas() {
        return offscreenCanvas;
    }

    public static int getInitWidth() {
        return initWidth;
    }

    public static int getInitHeight() {
        return initHeight;
    }

    // -------------------------------------------------------------------------
    // Request helpers
    // -------------------------------------------------------------------------

    /** Sends a request and blocks (via JSPromise.await) until the main thread replies. */
    public static BridgeResponse request(String type) {
        BridgeRequest req = JSObjects.create();
        int id = nextId++;
        req.setId(id);
        req.setType(type);
        return post(id, req).await();
    }

    public static BridgeResponse request(String type, RequestWriter writer) {
        BridgeRequest req = JSObjects.create();
        int id = nextId++;
        req.setId(id);
        req.setType(type);
        writer.write(req);
        return post(id, req).await();
    }

    /** Fire-and-forget — no ID, no response awaited. */
    public static void send(String type) {
        BridgeRequest req = JSObjects.create();
        req.setId(0);
        req.setType(type);
        postRaw(req);
    }

    private static JSPromise<BridgeResponse> post(int id, BridgeRequest req) {
        return new JSPromise<>((resolve, reject) -> {
            pending.put(id, resolve::accept);
            postRaw(req);
        });
    }

    // -------------------------------------------------------------------------
    // JSO bindings
    // -------------------------------------------------------------------------

    @JSBody(script = "return self.pendingInit;")
    private static native InitMessage getPendingInit();

    @JSBody(params = "handler", script = "self.onmessage = handler;")
    private static native void setOnMessage(EventListener<MessageEvent> handler);

    @JSBody(params = "msg", script = "self.postMessage(msg);")
    private static native void postRaw(JSObject msg);

    // -------------------------------------------------------------------------
    // Message interfaces
    // -------------------------------------------------------------------------

    public interface RequestWriter {
        void write(BridgeRequest req);
    }

    public interface BridgeRequest extends JSObject {
        @JSProperty("id")   void setId(int id);
        @JSProperty("type") void setType(String type);
        @JSProperty("cursor") void setCursor(String cursor);
        @JSProperty("enable") void setEnable(boolean enable);
        @JSProperty("url")  void setUrl(String url);
    }

    public interface BridgeResponse extends JSObject {
        @JSProperty("id")     int getId();
        @JSProperty("type")   String getType();
        @JSProperty("width")  int getWidth();
        @JSProperty("height") int getHeight();
        @JSProperty("dpr")    double getDpr();
        @JSProperty("data")   ArrayBuffer getData();
        @JSProperty("error")  String getError();
        // incoming event fields (forwarded raw browser event data)
        @JSProperty("eventType") String getEventType();
        @JSProperty("x")          int getX();
        @JSProperty("y")          int getY();
        @JSProperty("button")     int getButton();
        @JSProperty("buttons")    int getButtons();
        @JSProperty("deltaY")     double getDeltaY();
        @JSProperty("deltaMode")  int getDeltaMode();
        @JSProperty("code")       String getCode();
        @JSProperty("key")        String getKey();
        @JSProperty("shiftKey")   boolean isShiftKey();
        @JSProperty("ctrlKey")    boolean isCtrlKey();
        @JSProperty("altKey")     boolean isAltKey();
        @JSProperty("metaKey")    boolean isMetaKey();
    }

    public interface InitMessage extends JSObject {
        @JSProperty("offscreenCanvas") JSObject getOffscreenCanvas();
        @JSProperty("width")  int getWidth();
        @JSProperty("height") int getHeight();
        @JSProperty("dpr")    double getDpr();
    }
}
