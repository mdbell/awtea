package me.mdbell.awtea.instrument;

/**
 * Runtime side of {@link TraceHacks}: a fixed-size ring buffer of
 * "thread method" entries, drained on demand (e.g. via a CDP-evaluated
 * export).
 *
 * Everything here MUST stay non-suspending (plain field/array ops only): the
 * enter() call is injected into arbitrary methods, including ones invoked
 * from JS callbacks with no current fiber — any suspendable call here would
 * CPS-taint every traced method and trap in Fiber.isResuming. Keep the
 * static initializer trivial for the same reason (suspendable clinits taint
 * every caller).
 */
public final class TraceLog {

    private static final int SIZE = 8192;
    private static final int MASK = SIZE - 1;

    private static final String[] ring = new String[SIZE];
    private static int idx = 0;
    private static long total = 0;

    private TraceLog() {
    }

    /** Injected at method entry by TraceHacks. */
    public static void enter(String sig) {
        Thread t = Thread.currentThread();
        String entry = (t != null ? t.getName() : "?") + " " + sig;
        ring[idx] = entry;
        idx = (idx + 1) & MASK;
        total++;
        // Mirror into a JS-side array (window.__trace) so the buffer is
        // readable from DevTools even when every Java export is
        // CPS-tainted. Plain JS interop — non-suspending.
        jsPush(entry);
    }

    @org.teavm.jso.JSBody(params = { "s" }, script = ""
            + "var w = (typeof window !== 'undefined') ? window : globalThis;"
            + "if (!w.__trace) { w.__trace = []; }"
            + "w.__trace.push(s);"
            + "if (w.__trace.length > 16384) { w.__trace.splice(0, 8192); }")
    private static native void jsPush(String s);

    /** Snapshot of the newest entries (oldest first), plus the total count. */
    public static String drain(int max) {
        StringBuilder sb = new StringBuilder();
        sb.append("total=").append(total).append('\n');
        int n = (int) Math.min(Math.min(max, SIZE), total);
        int start = (idx - n) & MASK;
        for (int i = 0; i < n; i++) {
            String s = ring[(start + i) & MASK];
            if (s != null) {
                sb.append(s).append('\n');
            }
        }
        return sb.toString();
    }
}
