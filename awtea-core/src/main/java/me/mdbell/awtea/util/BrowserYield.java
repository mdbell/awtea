package me.mdbell.awtea.util;

import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

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
    @Async
    public static native void yieldNow();

    private static void yieldNow(AsyncCallback<Void> callback) {
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
