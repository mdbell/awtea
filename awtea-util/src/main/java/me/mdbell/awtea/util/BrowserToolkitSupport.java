package me.mdbell.awtea.util;

import org.teavm.jso.JSBody;
import org.teavm.jso.core.JSPromise;

/**
 * Browser-side helpers backing {@code java.awt.Toolkit} services (beep,
 * rendering sync).
 *
 * These live here rather than in the classlib Toolkit itself because the
 * wasm-gc backend does not process {@code @JSBody} methods declared inside
 * package-mapped classlib classes (they reach codegen as bare natives and
 * fail with "not annotated with @Import"). Classes outside the
 * {@code me.mdbell.awtea.classlib} hierarchy are unaffected.
 */
public final class BrowserToolkitSupport {

    private BrowserToolkitSupport() {
    }

    /**
     * Plays a system beep sound using the Web Audio API.
     * Creates a short 440Hz tone (standard beep frequency).
     */
    @JSBody(script =
        "try {" +
        "  var ctx = new (window.AudioContext || window.webkitAudioContext)();" +
        "  var osc = ctx.createOscillator();" +
        "  var gain = ctx.createGain();" +
        "  osc.connect(gain);" +
        "  gain.connect(ctx.destination);" +
        "  osc.frequency.value = 440;" + // Standard A4 note
        "  gain.gain.value = 0.3;" +      // 30% volume to avoid being too loud
        "  osc.start(ctx.currentTime);" +
        "  osc.stop(ctx.currentTime + 0.1);" + // 100ms beep
        "} catch(e) {" +
        "  console.warn('Unable to play beep sound:', e);" +
        "}"
    )
    public static native void playBeepSound();

    /**
     * Synchronizes rendering by waiting for the next animation frame.
     * This ensures all pending DOM and canvas operations are flushed.
     * Returns a promise that resolves when the next animation frame is processed.
     *
     * @return a promise that resolves after the next animation frame
     */
    @JSBody(script =
        "return new Promise(function(resolve) {" +
        "  if (typeof requestAnimationFrame !== 'undefined') {" +
        "    requestAnimationFrame(function() { resolve(null); });" +
        "  } else {" +
        "    resolve(null);" +
        "  }" +
        "});"
    )
    public static native JSPromise<Void> syncRendering();
}
