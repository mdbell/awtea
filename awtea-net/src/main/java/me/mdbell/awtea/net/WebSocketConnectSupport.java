package me.mdbell.awtea.net;

import java.io.IOException;
import java.util.function.Consumer;

import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.function.JSConsumer;
import org.teavm.jso.websocket.WebSocket;

/**
 * JS-side WebSocket connect plumbing for the classlib Socket.
 *
 * Lives OUTSIDE the package-mapped classlib hierarchy on purpose: under the
 * wasm-gc backend, lambdas declared inside {@code mapPackageHierarchy}-renamed
 * classes get CPS-transformed regardless of body and trap in
 * Fiber.isResuming when invoked from the JS event loop (no current fiber).
 * The same lambdas declared in a normal class compile clean. See
 * docs/wasm-port-plan.md in the client repo for the full catalogue of
 * mapped-class miscompiles (@JSBody, @PlatformMarker, lambda CPS-taint).
 *
 * All callbacks here are JS-async-invoked and must stay non-suspending:
 * resolve/reject + plain JSO calls only. No Window.clearTimeout (routed
 * through TeaVM's suspendable EventQueue under wasm-gc) — instead the timeout
 * checks readyState, and a late firing after settle is a no-op.
 */
public final class WebSocketConnectSupport {

    private WebSocketConnectSupport() {
    }

    /**
     * Java-side consumer of stream events. Implementations live in mapped
     * classlib code — that is fine: only @JSFunctor lambda classes declared in
     * mapped classes miscompile, plain Java interfaces/virtual calls do not.
     * Implementations must stay non-suspending (no monitors, no suspendable
     * calls) since they run on the JS event loop with no current fiber.
     */
    public interface StreamSink {

        void data(org.teavm.jso.typedarrays.Int8Array bytes);

        void error();
    }

    /**
     * Attaches the post-connect message/error handlers. Declared here (not in
     * the mapped Socket class) for the same CPS-taint reason as
     * {@link #connect}.
     */
    public static void attachStreamHandlers(WebSocket ws, StreamSink sink) {
        ws.onMessage(evt -> sink.data(new org.teavm.jso.typedarrays.Int8Array(evt.getDataAsArray())));
        ws.onError(e -> {
            sink.error();
            if (ws.getReadyState() <= 1) {
                ws.close();
            }
        });
    }

    /**
     * Returns a promise that settles when the socket opens or errors.
     *
     * @param ws           the (already constructed) WebSocket
     * @param timeoutMs    connect timeout in ms; 0 or negative disables it.
     *                     Without it the promise may never settle — V8 keeps
     *                     a copy of the current script alive per pending
     *                     promise, which leaks significantly with large
     *                     generated output.
     * @param timeoutError pre-constructed exception to reject with on timeout
     *                     (constructed eagerly by the caller in fiber context;
     *                     constructing it inside the callback would need a
     *                     caller-supplied lambda, which would be mapped-class
     *                     code again)
     * @param rejectSink   receives the reject function synchronously so the
     *                     caller can fail a pending connect from close()
     */
    public static JSPromise<WebSocket> connect(WebSocket ws, int timeoutMs, Throwable timeoutError,
            Consumer<JSConsumer<Object>> rejectSink) {
        return new JSPromise<>((resolve, reject) -> {
            rejectSink.accept(reject);
            if (timeoutMs > 0) {
                Window.setTimeout(() -> {
                    if (ws.getReadyState() == 0) { // still CONNECTING
                        reject.accept(timeoutError);
                        ws.close();
                    }
                }, timeoutMs);
            }
            // Not tracked/cleaned up: a settled promise ignores late
            // resolve/reject, and onOpen cannot re-fire. The error handler
            // below stays alive after connect; post-connect errors then hit
            // both it (no-op reject + idempotent close) and the caller's own
            // error handler.
            ws.onOpen(e -> resolve.accept(ws));
            ws.onError(e -> {
                reject.accept(new IOException("WebSocket error during connect"));
                ws.close();
            });
        });
    }
}
