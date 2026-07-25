package me.mdbell.awtea.util;

import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Window;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLImageElement;

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
    /**
     * Decodes an image URL into a canvas 2D context. Declared here (unmapped)
     * because the wasm-gc backend CPS-taints JS-callback lambdas declared in
     * package-mapped classlib classes — the old @Async onLoad handler in the
     * Toolkit trapped in Fiber.isResuming when the image finished decoding,
     * permanently suspending the loading thread. The object URL is revoked on
     * success.
     */
    public static JSPromise<CanvasRenderingContext2D> loadImage(String url) {
        return new JSPromise<>((resolve, reject) -> {
            HTMLImageElement img = (HTMLImageElement) Window.current().getDocument().createElement("img");
            HTMLCanvasElement canvasElement = (HTMLCanvasElement) Window.current().getDocument().createElement("canvas");
            img.onLoad(evt -> {
                canvasElement.setWidth(img.getWidth());
                canvasElement.setHeight(img.getHeight());
                CanvasRenderingContext2D context = JSObjectsExtensions.getContext2d(canvasElement, true);
                context.drawImage(img, 0, 0);
                JSObjectsExtensions.revokeObjectUrl(url);
                resolve.accept(context);
            });
            img.onEvent("error", evt -> reject.accept(new java.io.IOException("Unable to read image from URL")));
            img.setSrc(url);
        });
    }

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
