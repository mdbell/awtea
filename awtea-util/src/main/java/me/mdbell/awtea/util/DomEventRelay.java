package me.mdbell.awtea.util;

import org.teavm.jso.JSBody;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * wasm-gc input path: captures DOM events with a pure-JS listener into a
 * per-element JS array, polled by a Java green thread (fiber context) that
 * runs the real conversion/dispatch logic.
 *
 * Exists because DOM listener lambdas declared in the package-mapped
 * TEventManager get CPS-tainted under wasm-gc and trap in Fiber.isResuming on
 * the first input event. preventDefault decisions that must be synchronous
 * (context menu, keypress, Tab focus-trap) are made in the JS listener; the
 * Java side sees the event afterwards.
 */
public final class DomEventRelay {

    private DomEventRelay() {
    }

    @JSBody(params = { "el", "type" }, script = ""
            + "if (!el.__awteaQ) { el.__awteaQ = []; }"
            + "var q = el.__awteaQ;"
            + "el.addEventListener(type, function(e) {"
            + "  if (type === 'contextmenu' || type === 'keypress') { e.preventDefault(); }"
            + "  if (type === 'keydown' && e.key === 'Tab') { e.preventDefault(); }"
            + "  if (type !== 'contextmenu') { q.push(e); }"
            + "  if (q.length > 4096) { q.splice(0, 2048); }"
            + "}, { passive: false });")
    public static native void capture(HTMLElement el, String type);

    @JSBody(params = { "el" }, script = "var q = el.__awteaQ; return (q && q.length > 0) ? q.shift() : null;")
    public static native Event poll(HTMLElement el);
}
