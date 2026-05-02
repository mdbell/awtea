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

    // Registered by TWorkerAudioSourceDataLine to receive audio.consumed drain events.
    private static Consumer<BridgeResponse> audioListener;

    // Guards against double-init and against being called before worker-entry.js
    // sets self.pendingInit (TToolkit's static field initializer runs at module
    // load time, before the 'init' message fires).
    private static boolean initialized = false;

    private TMainThreadBridge() {}

    public static void setEventListener(Consumer<BridgeResponse> listener) {
        eventListener = listener;
    }

    public static void setAudioListener(Consumer<BridgeResponse> listener) {
        audioListener = listener;
    }

    /**
     * Initialises the bridge from the payload stored in self.pendingInit by
     * worker-entry.js. Safe to call before pendingInit is set (returns without
     * doing anything so that TWorkerToolkit construction at module-load time
     * doesn't crash); TApplet re-calls it inside createHeavyCanvas() once
     * main() is running and pendingInit is guaranteed to be present.
     */
    public static void init() {
        if (initialized) return;
        InitMessage msg = getPendingInit();
        if (msg == null) return;   // pendingInit not yet set; TApplet will retry
        initialized = true;
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
            // Audio drain events forwarded to the audio line
            if ("audio.consumed".equals(type) && audioListener != null) {
                audioListener.accept(resp);
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

    /** Posts msg and transfers the given ArrayBuffer to avoid a copy. */
    @JSBody(params = {"msg", "data"}, script = "self.postMessage(msg, [data]);")
    private static native void postRawTransferring(JSObject msg, ArrayBuffer data);

    // -------------------------------------------------------------------------
    // Audio helpers (fire-and-forget, called from TWorkerAudioSourceDataLine)
    // -------------------------------------------------------------------------

    public static void sendPcm(int audioId, ArrayBuffer data, int frames) {
        BridgeRequest req = JSObjects.create();
        req.setId(0);
        req.setType("audio.pcm");
        req.setAudioId(audioId);
        req.setAudioFrames(frames);
        req.setBytes(data);
        postRawTransferring(req, data);
    }

    public static void sendAudioControl(String type, int audioId) {
        BridgeRequest req = JSObjects.create();
        req.setId(0);
        req.setType(type);
        req.setAudioId(audioId);
        postRaw(req);
    }

    // -------------------------------------------------------------------------
    // Message interfaces
    // -------------------------------------------------------------------------

    public interface RequestWriter {
        void write(BridgeRequest req);
    }

    public interface BridgeRequest extends JSObject {
        @JSProperty("id")     void setId(int id);
        @JSProperty("type")   void setType(String type);
        @JSProperty("cursor") void setCursor(String cursor);
        @JSProperty("enable") void setEnable(boolean enable);
        @JSProperty("url")    void setUrl(String url);
        @JSProperty("bytes")  void setBytes(ArrayBuffer bytes);
        // audio fields
        @JSProperty("sampleRate")    void setAudioSampleRate(int sampleRate);
        @JSProperty("channels")      void setAudioChannels(int channels);
        @JSProperty("sampleSizeBits") void setAudioSampleSizeBits(int bits);
        @JSProperty("bigEndian")     void setAudioBigEndian(boolean bigEndian);
        @JSProperty("maxFrames")     void setAudioMaxFrames(int frames);
        @JSProperty("script")        void setAudioScript(String script);
        @JSProperty("audioId")       void setAudioId(int id);
        @JSProperty("frames")        void setAudioFrames(int frames);
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
        // audio fields
        @JSProperty("audioId")   int getAudioId();
        @JSProperty("consumed")  int getConsumedBytes();
    }

    public interface InitMessage extends JSObject {
        @JSProperty("offscreenCanvas") JSObject getOffscreenCanvas();
        @JSProperty("width")  int getWidth();
        @JSProperty("height") int getHeight();
        @JSProperty("dpr")    double getDpr();
    }
}
