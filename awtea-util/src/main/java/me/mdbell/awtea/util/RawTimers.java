package me.mdbell.awtea.util;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/**
 * Raw browser timers, bypassing {@code org.teavm.jso.browser.Window}.
 *
 * Under wasm-gc, TeaVM routes the JSO Window timer methods through its own
 * (suspendable) event-queue machinery, so any method that calls
 * Window.setTimeout/clearTimeout gets CPS-transformed — and a JS-invoked
 * functor (timer tick, event handler) that is CPS-transformed traps in
 * Fiber.isResuming, because there is no current fiber on the JS event loop.
 * These raw bindings call the real browser functions, which is plain JS
 * interop and stays non-suspending.
 *
 * Use these for timer chains driven by JS callbacks (keep-alives, drains).
 * The handler still must not reach suspendable code itself.
 */
public final class RawTimers {

    private RawTimers() {
    }

    /**
     * Deliberately NOT {@code org.teavm.jso.browser.TimerHandler}: TeaVM's
     * classlib rewrites TimerHandler functors into its own timer machinery
     * (they show up retyped as NotifyListener in the wasm), which re-taints
     * the callback no matter how it is scheduled. A private functor type gets
     * no special treatment.
     */
    @JSFunctor
    public interface RawTimerHandler extends JSObject {
        void onTimer();
    }

    @JSBody(params = { "handler", "delay" }, script = "return setTimeout(handler, delay);")
    public static native int setTimeout(RawTimerHandler handler, int delay);

    @JSBody(params = { "handler", "delay" }, script = "return setTimeout(handler, delay);")
    public static native int setTimeout(RawTimerHandler handler, double delay);

    @JSBody(params = { "id" }, script = "clearTimeout(id);")
    public static native void clearTimeout(int id);
}
