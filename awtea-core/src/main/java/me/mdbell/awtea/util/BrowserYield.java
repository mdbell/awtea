package me.mdbell.awtea.util;

import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSPromise;

/**
 * Sub-millisecond main-thread yield. TeaVM lowers {@code Thread.sleep} to
 * {@code setTimeout}, and browsers clamp nested timeouts to ~4ms — a fixed
 * per-frame tax on the shell loop, which must suspend once per frame to keep
 * input, networking and compositing alive (~4ms of every frame spent idle,
 * capping even a free renderer at ~250 FPS).
 *
 * <p>A MessageChannel message is a macrotask with no such clamp: the browser
 * still interleaves queued input tasks and compositor work between messages,
 * but control returns in ~0.05-0.2ms.</p>
 */
public final class BrowserYield {

    private BrowserYield() {
    }

    @JSFunctor
    public interface YieldHandler extends JSObject {
        void run();
    }

    /** Suspends the calling green thread until the next macrotask turn. */
    public static void yieldNow() {
        if (PlatformSupport.isWebAssemblyGC()) {
            // wasm-gc: await a promise that resolves on a MessageChannel
            // macrotask. The resolve happens entirely in JS (port.onmessage),
            // so there is no AsyncCallback.complete / fiber-resume functor in
            // the raw JS event path — that hand-rolled @Async form trapped in
            // Fiber.isResuming (no current fiber on the JS event loop). Promise
            // await() DOES bridge correctly on wasm-gc (same path as the
            // WebSocket connect and image loader), and MessageChannel is an
            // unclamped macrotask. This replaced Thread.sleep(0), which TeaVM
            // lowers to setTimeout — browser-clamped to ~1-4ms, leaving the
            // render loop idle ~40% of the time (measured).
            macrotaskPromise().await();
            return;
        }
        yieldViaMessageChannel();
    }

    /**
     * A promise that resolves on the next MessageChannel macrotask. Pure JS:
     * the resolver is stored and invoked from port.onmessage, giving the
     * browser a compositing/input turn between messages without the
     * nested-setTimeout clamp.
     */
    @JSBody(script =
            "var w = window;" +
            "if (!w.__srYield) {" +
            "  w.__srYield = { queue: [], channel: new MessageChannel() };" +
            "  w.__srYield.channel.port1.onmessage = function() {" +
            "    var q = w.__srYield.queue;" +
            "    w.__srYield.queue = [];" +
            "    for (var i = 0; i < q.length; i++) q[i]();" +
            "  };" +
            "}" +
            "return new Promise(function(resolve) {" +
            "  w.__srYield.queue.push(resolve);" +
            "  w.__srYield.channel.port2.postMessage(0);" +
            "});")
    private static native JSPromise<Void> macrotaskPromise();

    @Async
    private static native void yieldViaMessageChannel();

    private static void yieldViaMessageChannel(AsyncCallback<Void> callback) {
        post(() -> callback.complete(null));
    }

    @JSBody(params = {"cb"}, script =
            "var w = window;" +
            "if (!w.__srYield) {" +
            "  w.__srYield = { queue: [], channel: new MessageChannel() };" +
            "  w.__srYield.channel.port1.onmessage = function() {" +
            "    var q = w.__srYield.queue;" +
            "    w.__srYield.queue = [];" +
            "    for (var i = 0; i < q.length; i++) q[i]();" +
            "  };" +
            "}" +
            "w.__srYield.queue.push(cb);" +
            "w.__srYield.channel.port2.postMessage(0);")
    private static native void post(YieldHandler cb);
}
